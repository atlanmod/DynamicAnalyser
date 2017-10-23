package com.tblf.parsing;

import com.tblf.Model.Analysis;
import com.tblf.Model.ModelFactory;
import com.tblf.Model.ModelPackage;
import com.tblf.util.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.CompilationUnit;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.modisco.java.composition.javaapplication.Java2Directory;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationPackage;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.eclipse.modisco.kdm.source.extension.ExtensionPackage;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.ocl.options.ParsingOptions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Thibault on 26/09/2017.
 */
public class TraceParser implements Runnable {
    private static final Logger LOGGER = Logger.getLogger("TraceParser");
    private File file;
    private Resource outputModelResource;

    private static final String ANALYSIS_NAME = Configuration.getProperty("analysisName");

    private Map<String, Resource> packages;

    private String currentTestPackageQN;
    private Resource currentTestPackage;

    private String currentTargetPackageQN;
    private Resource currentTargetPackage;

    private String currentTestQN;
    private Java2File currentTest;

    private String currentTargetQN;
    private Java2File currentTarget;

    private String currentTestMethodQN;
    private ASTNodeSourceRegion currentTestMethod;

    private String currentTargetMethodQN;
    private ASTNodeSourceRegion currentTargetMethod;

    private OCLStatementQuery oclStatementQuery;

    /**
     * @param traceFile   the file containing the execution trace
     * @param resourceSet a resource set containing the fragments
     */
    public TraceParser(File traceFile, File outputModel, ResourceSet resourceSet) {
        this.file = traceFile;

        try {
            if (!outputModel.exists()) {
                outputModel.createNewFile();
            }
            outputModelResource = resourceSet.createResource(URI.createURI(outputModel.toURI().toURL().toString()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot load the traces", e);
        }

        JavaPackage.eINSTANCE.eClass();
        JavaapplicationPackage.eINSTANCE.eClass();
        ExtensionPackage.eINSTANCE.eClass();
        KdmPackage.eINSTANCE.eClass();
        ModelPackage.eINSTANCE.eClass();

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

        packages = new HashMap<>();
        resourceSet.getResources().stream()
                .filter(resource -> resource.getURI().segment(resource.getURI().segmentCount() - 2).equals(Configuration.getProperty("fragmentFolder")))
                .forEach(resource -> packages.put(resource.getURI().lastSegment().replace("_java2kdm.xmi", ""), resource));

        oclStatementQuery = new OCLStatementQuery();
    }

    @Override
    public void run() {
        parse();
    }

    /**
     * Parse the trace file line by line.
     * Depending of the trace type, will either find the test being executed, or the SUT being executed, or the statement being executed
     *
     * @return a impact analysis model
     */
    public Resource parse() {
        long startTime = System.currentTimeMillis();
        long currLine = 0;
        long maxLine = ParserUtils.getLineNumber(file); //We iterate starting from 0

        try {
            LineIterator lineIterator = FileUtils.lineIterator(file);
            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                ParserUtils.printProgress(startTime, maxLine, currLine);
                currLine += 1;
                String[] split = line.split(":");
                switch (split[0]) {
                    case "&": //set the test
                        updateTest(split[1], split[2]); // {qualifiedName; methodName}
                        break;

                    case "%": //set the SUT
                        updateTarget(split[1], split[2]); // {qualifiedName; methodName}
                        break;

                    case "?": //get the statement using its line
                        int lineNumber = Integer.parseInt(split[1]);
                        updateStatementUsingLine(lineNumber);
                        break;
                    case "!": //get the statement using its position
                        int startPos = Integer.parseInt(split[1]);
                        int endPos = Integer.parseInt(split[2]);

                        updateStatementUsingPosition(startPos, endPos);
                }
            }

            ParserUtils.endProgress(maxLine);
            LineIterator.closeQuietly(lineIterator);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't parse the traces", e);
        }

        try {
            outputModelResource.save(Collections.EMPTY_MAP);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save the analysis model", e);
        }

        LOGGER.info("Model available at URI: " + outputModelResource.getURI());
        return outputModelResource;
    }

    /**
     * Update the current tests being executed.
     *
     * @param testQN the test qualified name: pkg1.pk2.pkg3.MyClass
     * @param method the method name: MyMethod
     */
    private void updateTest(String testQN, String method) {
        String packageName = ParserUtils.getPackageQNFromClassQN(testQN);

        // Refreshing the current package containing the test, to lighten the number of queries to the model
        if (currentTestPackage == null || !currentTestPackageQN.equals(packageName)) {
            LOGGER.fine("Updating the current test package: " + packageName);

            currentTestPackage = packages.get(packageName);

            LOGGER.fine("Found the resource with URI: " + currentTestPackage.getURI());
            currentTestPackageQN = packageName;
        }

        if (currentTest == null || !currentTestQN.equals(testQN)) {
            LOGGER.fine("Updating the current test class: " + testQN);
            currentTest = getJava2FileFromJava2Directory(currentTestPackage, testQN);
            currentTestQN = testQN;
        }

        String fullMethodQN = testQN.concat(":").concat(method);
        if (currentTestMethod == null || !fullMethodQN.equals(currentTestMethodQN)) {
            LOGGER.fine("Updating the current test method: " + fullMethodQN);
            currentTestMethod = getMethodFromJava2File(currentTest, method);
            currentTestMethodQN = fullMethodQN;

            currentTargetMethod = null; //We reset the current SUT

            if (currentTestMethod == null) {
                currentTestMethod = getClassFromJava2File(currentTest, ((CompilationUnit) currentTest.getUnit()).getName().replace(".java", ""));
            }
        }
    }

    /**
     * Update the target being executed
     *
     * @param targetQn the target's qualified name: pkg1.pk2.pkg3.MyClass
     * @param method   the method's name: MyMethod()
     */
    private void updateTarget(String targetQn, String method) {
        String packageName = ParserUtils.getPackageQNFromClassQN(targetQn);

        // Refreshing the current package containing the test, to lighten the number of queries to the model
        if (currentTargetPackage == null || !currentTargetPackageQN.equals(packageName)) {
            LOGGER.fine("Updating the current target package: " + packageName);

            currentTargetPackage = packages.get(packageName);
            currentTargetPackageQN = packageName;
        }

        if (currentTarget == null || !currentTargetQN.equals(targetQn)) {
            LOGGER.fine("Updating the current target class: " + targetQn);
            currentTarget = getJava2FileFromJava2Directory(currentTargetPackage, targetQn);
            currentTargetQN = targetQn;
        }

        if (currentTargetMethod == null || !currentTargetMethodQN.equals(method)) {
            LOGGER.fine("Updating the current target method: " + method);
            currentTargetMethod = getMethodASTNodeFromJava2File(currentTarget, method);
            currentTargetMethodQN = method;
            createRunByAnalysis(currentTargetMethod, currentTestMethod);
        }

    }

    /**
     * Parse the {@link Java2File} children to file the node corresponding to the {@link MethodDeclaration} with the name given as a parameter
     *
     * @param java2File  a {@link Java2File}
     * @param methodName a {@link String}
     * @return the {@link org.eclipse.gmt.modisco.java.ASTNode} with the given {@link MethodDeclaration} name
     */
    private ASTNodeSourceRegion getMethodASTNodeFromJava2File(Java2File java2File, String methodName) {
        return java2File.getChildren().stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof MethodDeclaration
                        && ((MethodDeclaration) astNodeSourceRegion.getNode()).getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Parse the {@link Java2Directory} model in order to find a class using its qualified name
     *
     * @param resource A {@link Resource} containing {@link Java2File}s
     * @param name     the name of the {@link org.eclipse.gmt.modisco.java.ClassDeclaration}
     * @return the {@link Java2File}
     */
    private Java2File getJava2FileFromJava2Directory(Resource resource, String name) {
        name = name.substring(name.lastIndexOf(".") + 1); //We only keep the last part of the qualified name of the class

        if (name.contains("$")) { //This is an internal class
            name = name.substring(0, name.lastIndexOf("$")); //We remove the sub class part to find the right compilation unit
        }

        String finalName = name;
        return (Java2File) resource.getContents() // get all the Java2File
                .stream() // as a stream
                .filter(eObject -> (((Java2File) eObject).getJavaUnit().getName() // check that the compilation unit is the file corresponding to the
                        .equals(finalName.concat(".java")))) //Might have some issues with internal classes
                .findFirst().orElse(null);
    }

    /**
     * Parse the {@link org.eclipse.gmt.modisco.java.ASTNode} of a {@link Java2File} to find a specific {@link MethodDeclaration}
     *
     * @param java2File  a {@link Java2File}
     * @param methodName the name of the {@link MethodDeclaration}
     * @return the {@link MethodDeclaration}
     */
    private ASTNodeSourceRegion getMethodFromJava2File(Java2File java2File, String methodName) {
        return java2File
                .getChildren() //Get all the node source region
                .stream() // as a stream
                .filter(astNodeSourceRegion -> (astNodeSourceRegion.getNode() instanceof MethodDeclaration) // get the method declaration nodes
                        && (methodName.equals(((MethodDeclaration) astNodeSourceRegion.getNode()).getName()))) // with the name 'methodName'
                .findFirst() // and return the first one found
                .orElse(null);
    }

    /**
     * Parse the {@link org.eclipse.gmt.modisco.java.ASTNode} of a {@link Java2File} to find a specific {@link org.eclipse.gmt.modisco.java.ClassDeclaration}
     *
     * @param java2File a {@link Java2File}
     * @param className the name of the {@link org.eclipse.gmt.modisco.java.ClassDeclaration}
     * @return the {@link org.eclipse.gmt.modisco.java.ClassDeclaration}
     */
    private ASTNodeSourceRegion getClassFromJava2File(Java2File java2File, String className) {
        return java2File
                .getChildren() //Get all the node source region
                .stream() // as a stream
                .filter(astNodeSourceRegion -> (astNodeSourceRegion.getNode() instanceof ClassDeclaration) // get the method declaration nodes
                        && (className.equals(((ClassDeclaration) astNodeSourceRegion.getNode()).getName()))) // with the name 'methodName'
                .findFirst() // and return the first one found
                .orElse(null);
    }

    /**
     * Create an impact relation between a statement and a test method, using the statement line number after finding the statement in the model
     *
     * @param lineNumber the line number
     */
    private void updateStatementUsingLine(int lineNumber) {
        Collection<ASTNodeSourceRegion> astNodeSourceRegions = oclStatementQuery.queryLine(lineNumber, lineNumber, currentTarget);
        astNodeSourceRegions.forEach(astNodeSourceRegion -> createRunByAnalysis(astNodeSourceRegion, currentTestMethod));
    }

    /**
     * Create an impact relation between a statement and a test method using the statement position, after finding the statement in the model
     * This approach is way more accurate than the line one, since we can only have 1 statement with the specified position, when we could find multiple
     * statements on the same line
     *
     * @param startPos the start position inside the file, of the statement looked for
     * @param endPos   the end position inside the class file of the statement looked for*
     */
    private void updateStatementUsingPosition(int startPos, int endPos) {
        Collection<ASTNodeSourceRegion> astNodeSourceRegions = oclStatementQuery.queryPosition(startPos, endPos, currentTarget);
        astNodeSourceRegions.forEach(astNodeSourceRegion -> createRunByAnalysis(astNodeSourceRegion, currentTestMethod));
    }

    /**
     * Create an {@link Analysis} object pointing from a Source {@link ASTNodeSourceRegion} to a Target {@link ASTNodeSourceRegion}
     * Add this analysis to the Source {@link ASTNodeSourceRegion} and to the Target {@link ASTNodeSourceRegion}
     * Is usually a Statement pointing to a test {@link MethodDeclaration} node, or a SUT {@link MethodDeclaration} pointing to a test
     * {@link MethodDeclaration}.
     *
     * @param source an {@link org.eclipse.gmt.modisco.java.ASTNode}
     * @param target an {@link ASTNodeSourceRegion}
     */
    private void createRunByAnalysis(ASTNodeSourceRegion source, ASTNodeSourceRegion target) {
        //We create an analysis if it does not already exist.
        if (source != null && target != null && source.getAnalysis().stream().noneMatch(eObject -> ((Analysis) eObject).getTarget().equals(target.getNode()))) {
            Analysis analysis = ModelFactory.eINSTANCE.createAnalysis();
            analysis.setName(ANALYSIS_NAME);
            analysis.setSource(source.getNode());
            analysis.setTarget(target.getNode());
            source.getAnalysis().add(analysis);
            target.getAnalysis().add(analysis);
            outputModelResource.getContents().add(analysis);
        } else {
            LOGGER.fine("Link already existing. Not adding.");
        }
    }

    public Resource getOutputModelResource() {
        return outputModelResource;
    }

    public void setOutputModelResource(Resource outputModelResource) {
        this.outputModelResource = outputModelResource;
    }
}
