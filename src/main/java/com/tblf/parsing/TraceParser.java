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
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.modisco.java.composition.javaapplication.Java2Directory;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationPackage;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.eclipse.modisco.kdm.source.extension.ExtensionPackage;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
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
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private File file;
    private Resource outputModelResource;

    private ResourceSet resourceSet;
    private OCLHelper OCL_HELPER;
    private OCL ocl;

    private Map<String, Resource> packages;

    private String currentTestPackageQN;
    private Resource currentTestPackage;

    private String currentTargetPackageQN;
    private Resource currentTargetPackage;

    private String currentTestQN;
    private Java2File currentTest;

    private String currentTargetQN;
    private Java2File currentTarget;

    private String currentMethodQN;
    private MethodDeclaration currentTestMethod;


    /**
     *
     * @param traceFile the file containing the execution trace
     * @param resourceSet a resource set containing the fragments
     */
    public TraceParser(File traceFile, File outputModel, ResourceSet resourceSet) {
        this.file = traceFile;
        this.resourceSet = resourceSet;

        try {
            if (! outputModel.exists()) {
                outputModel.createNewFile();
            }
            outputModelResource = resourceSet.createResource(URI.createURI(outputModel.toURI().toURL().toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JavaPackage.eINSTANCE.eClass();
        JavaapplicationPackage.eINSTANCE.eClass();
        ExtensionPackage.eINSTANCE.eClass();
        KdmPackage.eINSTANCE.eClass();
        ModelPackage.eINSTANCE.eClass();

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

        EcoreEnvironmentFactory ecoreEnvironmentFactory = new EcoreEnvironmentFactory(EPackage.Registry.INSTANCE);

        ocl = OCL.newInstance(ecoreEnvironmentFactory);
        OCL_HELPER = ocl.createOCLHelper();

        ParsingOptions.setOption(ocl.getEnvironment(),
                ParsingOptions.implicitRootClass(ocl.getEnvironment()),
                EcorePackage.Literals.EOBJECT);

        packages = new HashMap<>();
        resourceSet.getResources().stream()
                .filter(resource -> resource.getURI().segment(resource.getURI().segmentCount() - 2).equals(Configuration.getProperty("fragmentFolder")))
                .forEach(resource -> packages.put(resource.getURI().lastSegment().replace("_java2kdm.xmi", ""), resource));
    }

    @Override
    public void run() {
        parse();
    }

    /**
     * Parse the trace file line by line.
     * Depending of the trace type, will either find the test being executed, or the SUT being executed, or the statement being executed
     * @return a impact analysis model
     */
    public Resource parse() {
        try {
            LineIterator lineIterator = FileUtils.lineIterator(file);
            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                String[] split = line.split(":");

                switch (split[0]){
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

            LineIterator.closeQuietly(lineIterator);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputModelResource.save(Collections.EMPTY_MAP);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save the analysis model", e);
        }

        LOGGER.info("Model available at URI: "+outputModelResource.getURI());
        return outputModelResource;
    }

    /**
     * Update the current tests being executed.
     * @param testQN the test qualified name: pkg1.pk2.pkg3.MyClass
     * @param method the method name: MyMethod
     */
    private void updateTest(String testQN, String method) {
        String packageName = ParserUtils.getPackageQNFromClassQN(testQN);

        // Refreshing the current package containing the test, to lighten the number of queries to the model
        if (currentTestPackage == null || ! currentTestPackageQN.equals(packageName)) {
            LOGGER.fine("Updating the current test package: "+packageName);

            currentTestPackage = packages.get(packageName);

            /*currentTestPackage = resourceSet
                    .getResources()
                    .stream()
                    .filter(r -> r.getURI().toString().contains(packageName))
                    .findFirst().get(); //Assuming the resource always exists. Will throw NPE otherwise*/

            LOGGER.fine("Found the resource with URI: "+currentTestPackage.getURI());
            currentTestPackageQN = packageName;
        }

        if (currentTest == null || ! currentTestQN.equals(testQN)) {
            LOGGER.fine("Updating the current test class: "+testQN);
            currentTest = getJava2FileFromJava2Directory(currentTestPackage, testQN);
            currentTestQN = testQN;
        }

        String fullMethodQN = testQN.concat(":").concat(method);
        if (currentTestMethod == null || !fullMethodQN.equals(currentMethodQN)) {
            LOGGER.fine("Updating the current test method: "+ fullMethodQN);
            currentTestMethod = getMethodFromJava2File(currentTest, method);
            currentMethodQN = fullMethodQN;
        }
    }

    /**
     * Update the target being executed
     * @param targetQn the target's qualified name: pkg1.pk2.pkg3.MyClass
     * @param method the method's name: MyMethod()
     */
    private void updateTarget(String targetQn, String method) {
        String packageName = ParserUtils.getPackageQNFromClassQN(targetQn);


        // Refreshing the current package containing the test, to lighten the number of queries to the model
        if (currentTargetPackage == null || ! currentTargetPackageQN.equals(packageName)) {
            LOGGER.fine("Updating the current target package: "+packageName);

            currentTargetPackage = packages.get(packageName);

/*            currentTargetPackage = resourceSet
                    .getResources()
                    .stream()
                    .filter(r -> r.getURI().toString().contains(packageName))
                    .findFirst().get(); //Assuming the resource always exists. Will throw NPE otherwise*/

            currentTargetPackageQN = packageName;
        }

        if (currentTarget == null || ! currentTargetQN.equals(targetQn)) {
            LOGGER.fine("Updating the current target class: "+targetQn);
            currentTarget = getJava2FileFromJava2Directory(currentTargetPackage, targetQn);
            currentTargetQN = targetQn;
        }
    }

    /**
     * Parse the {@link Java2Directory} model in order to find a class using its qualified name
     * @param resource A {@link Resource} containing {@link Java2File}s
     * @param name the name of the {@link org.eclipse.gmt.modisco.java.ClassDeclaration}
     * @return the {@link Java2File}
     */
    private Java2File getJava2FileFromJava2Directory(Resource resource, String name) {
        String finalName  = name.substring(name.lastIndexOf(".")+1);
        return (Java2File) resource.getContents() // get all the Java2File
                .stream() // as a stream
                .filter(eObject -> (((Java2File)eObject).getJavaUnit().getName() // check that the compilation unit is the file corresponding to the
                        .endsWith(finalName.concat(".java")))) // defined class file
                .findFirst().orElse(null);
    }

    /**
     * Parse the {@link org.eclipse.gmt.modisco.java.ASTNode} of a {@link Java2File} to find a specific {@link MethodDeclaration}
     * @param java2File a {@link Java2File}
     * @param methodName the name of the {@link MethodDeclaration}
     * @return the {@link MethodDeclaration}
     */
    private MethodDeclaration getMethodFromJava2File(Java2File java2File, String methodName) {
        MethodDeclaration methodDeclaration = null;

        Optional<ASTNodeSourceRegion> method = java2File
                .getChildren() //Get all the node source region
                .stream() // as a stream
                .filter(astNodeSourceRegion -> (astNodeSourceRegion.getNode() instanceof MethodDeclaration) // get the method declaration nodes
                    && (methodName.equals(((MethodDeclaration) astNodeSourceRegion.getNode()).getName()))) // with the name 'methodName'
                .findFirst(); // and return the first one found

        if (method.isPresent()) {
            methodDeclaration = (MethodDeclaration) method.get().getNode();
        }

        return methodDeclaration;
    }

    /**
     * Create an impact relation between a statement and a test method, using the statement line number after finding the statement in the model
     * @param lineNumber the line number
     */
    private void updateStatementUsingLine(int lineNumber) {
        OCL_HELPER.setContext(JavaapplicationPackage.eINSTANCE.getEClassifier("Java2File"));

        try {
            String queryAsString = "JavaNodeSourceRegion.allInstances() -> select (startLine = " +
                    lineNumber +
                    " and endLine = " +
                    lineNumber +
                    " and node.oclIsKindOf(java::Statement))";

            LOGGER.fine("Executing the following query: "+queryAsString);

            OCLExpression query = OCL_HELPER.createQuery(queryAsString);
            Set<ASTNodeSourceRegion> nodes = (Set<ASTNodeSourceRegion>) ocl.createQuery(query).evaluate(currentTarget);
            nodes.parallelStream().forEach(astNodeSourceRegion -> {
                LOGGER.fine("Found a node. Creating an object in the output model");

                //Is there already an analysis Object in the node pointing from the Statement to the current test ?
                if (astNodeSourceRegion.getAnalysis()
                        .stream()
                        .noneMatch(eObject -> (eObject instanceof Analysis) &&
                                ((Analysis) eObject).getTarget().equals(currentTestMethod))) {
                    Analysis analysis = ModelFactory.eINSTANCE.createAnalysis();
                    analysis.setName("runby");
                    analysis.setSource(astNodeSourceRegion.getNode());
                    analysis.setTarget(currentTestMethod);
                    astNodeSourceRegion.getAnalysis().add(analysis);
                    outputModelResource.getContents().add(analysis);
                    LOGGER.fine("Added the link.");
                } else {
                    LOGGER.fine("Link already existing. Not adding.");
                }
            });

        } catch (ParserException e) {
            LOGGER.warning("Couldn't create the OCL request to find the statement in the model " + Arrays.toString(e.getStackTrace()));
        }

    }


    /**
     * Create an impact relation between a statement and a test method using the statement position, after finding the statement in the model
     * This approach is way more accurate than the line one, since we can only have 1 statement with the specified position, when we could find multiple
     * statements on the same line
     * @param startPos the start position inside the file, of the statement looked for
     * @param endPos the end position inside the class file of the statement looked for*
     */
    private void updateStatementUsingPosition(int startPos, int endPos) {
        OCL_HELPER.setContext(JavaapplicationPackage.eINSTANCE.getEClassifier("Java2File"));

        try {
            String queryAsString = "JavaNodeSourceRegion.allInstances() -> select (startPosition = " +
                    startPos +
                    " and endPosition = " +
                    endPos +
                    " and node.oclIsKindOf(java::Statement))";
            System.out.println(queryAsString);
            OCLExpression query = OCL_HELPER.createQuery(queryAsString);
            Set<ASTNodeSourceRegion> nodes = (Set<ASTNodeSourceRegion>) ocl.createQuery(query).evaluate(currentTarget);
            nodes.parallelStream().forEach(astNodeSourceRegion -> {
                LOGGER.fine("Found a node. Creating an object in the output model");

                //Is there already an analysis Object in the node pointing from the Statement to the current test ?
                if (astNodeSourceRegion.getAnalysis()
                        .stream()
                        .noneMatch(eObject -> (eObject instanceof Analysis) &&
                                ((Analysis) eObject).getTarget().equals(currentTestMethod))) {
                    Analysis analysis = ModelFactory.eINSTANCE.createAnalysis();
                    analysis.setName("runby");
                    analysis.setSource(astNodeSourceRegion.getNode());
                    analysis.setTarget(currentTestMethod);
                    astNodeSourceRegion.getAnalysis().add(analysis);
                    outputModelResource.getContents().add(analysis);
                    LOGGER.fine("Added the link.");
                } else {
                    LOGGER.fine("Link already existing. Not adding.");
                }

            });

        } catch (ParserException e) {
            LOGGER.warning("Couldn't create the OCL request to find the statement in the model " + Arrays.toString(e.getStackTrace()));
        }
    }

    public Resource getOutputModelResource() {
        return outputModelResource;
    }

    public void setOutputModelResource(Resource outputModelResource) {
        this.outputModelResource = outputModelResource;
    }
}
