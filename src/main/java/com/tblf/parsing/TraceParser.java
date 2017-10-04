package com.tblf.parsing;

import com.tblf.Model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.CompilationUnit;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.modisco.java.composition.javaapplication.Java2Directory;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.ocl.options.ParsingOptions;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Thibault on 26/09/2017.
 */
public class TraceParser implements Runnable {
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private File file;
    private com.tblf.Model.Model analysisModel;
    private static final ModelFactory MODEL_FACTORY = ModelFactory.eINSTANCE;
    private ResourceSet resourceSet;
    private OCLHelper OCL_HELPER;
    private OCL ocl;

    private String currentTestPackageQN;
    private Java2Directory currentTestPackage;

    private String currentTargetPackageQN;
    private Java2Directory currentTargetPackage;

    private String currentTestQN;
    private Java2File currentTest;

    private String currentTargetQN;
    private Java2File currentTarget;

    private String currentMethodQN;
    private MethodDeclaration currentTestMethod;


    /**
     *
     * @param file the file containing the execution trace
     * @param resourceSet a resource set containing the fragments
     */
    public TraceParser(File file, ResourceSet resourceSet) {
        this.file = file;
        this.resourceSet = resourceSet;
        this.analysisModel = MODEL_FACTORY.createModel();

        ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        OCL_HELPER = ocl.createOCLHelper();

        ParsingOptions.setOption(ocl.getEnvironment(),
                ParsingOptions.implicitRootClass(ocl.getEnvironment()),
                EcorePackage.Literals.EOBJECT);
    }

    /**
     * With the resourceSet:
     * Find the correct package model using the qualifiedName of the class
     * Find the right class, then the right method, and finally the exact statement using position
     * @param qualifiedClass
     * @param method
     * @param startCol
     * @param endCol
     * @return The statement corresponding to the parameters entered
     */
    public Statement findStatementUsingPosition(String qualifiedClass, String method, int startCol, int endCol) {

        throw new RuntimeException("not implemented");
    }

    /**
     * Parse the trace file line by line.
     * Depending of the trace type, will either find the test being executed, or the SUT being executed, or the statement being executed
     * @return a impact analysis model
     */
    public com.tblf.Model.Model parse() {
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
                        LOGGER.fine("Getting the target class: "+split[1]);

                        LOGGER.fine("Getting the target method: "+split[2]);
                        break;
                    case "?": //get the statement using its line

                        break;
                    case "!": //get the statement using its position
                        throw new RuntimeException("not implemented");
                }
            }

            LineIterator.closeQuietly(lineIterator);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.analysisModel;
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
            Resource resource = resourceSet
                    .getResources()
                    .stream()
                    .filter(r -> r.getURI().toString().contains(packageName))
                    .findFirst().get(); //Assuming the resource always exists. Will throw NPE otherwise
            currentTestPackage = (Java2Directory) resource.getContents().get(0);
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
            Resource resource = resourceSet
                    .getResources()
                    .stream()
                    .filter(r -> r.getURI().toString().contains(packageName))
                    .findFirst().get(); //Assuming the resource always exists. Will throw NPE otherwise
            currentTargetPackage = (Java2Directory) resource.getContents().get(0);
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
     * @param java2Directory A {@link Java2Directory}
     * @param name the name of the {@link org.eclipse.gmt.modisco.java.ClassDeclaration}
     * @return the {@link Java2File}
     */
    private Java2File getJava2FileFromJava2Directory(Java2Directory java2Directory, String name) {
        String finalName  = name.substring(name.lastIndexOf(".")+1);
        return java2Directory.getJava2FileChildren() // get all the Java2File
                .stream() // as a stream
                .filter(java2File -> ((CompilationUnit) java2File.getUnit()) // check that the compilation unit is the file corresponding to the
                        .getName().endsWith(finalName.concat(".java"))) // defined class file
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

    @Override
    public void run() {

    }
}
