package com.tblf.parsing.parsers;

import com.tblf.model.Analysis;
import com.tblf.model.ModelFactory;
import com.tblf.parsing.queries.Query;
import com.tblf.parsing.queries.StreamQuery;
import com.tblf.utils.Configuration;
import com.tblf.utils.ParserUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.*;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by Thibault on 26/09/2017.
 */
public class TraceParser extends Parser {
    private static final String ANALYSIS_NAME = Configuration.getProperty("analysisName");

    private Map<String, Java2File> classQNJava2File;

    private String currentTestQN;
    private Java2File currentTest;

    private String currentTargetQN;
    private Java2File currentTarget;

    private String currentTestMethodQN;
    private ASTNodeSourceRegion currentTestMethod;

    private String currentTargetMethodQN;
    private ASTNodeSourceRegion currentTargetMethod;

    private Query query;
    private BufferedReader reader;

    /**
     * @param traceFile   the file containing the execution trace
     * @param resourceSet a resource set containing the fragments
     */
    public TraceParser(File traceFile, File outputModel, ResourceSet resourceSet) {
        super(traceFile, outputModel, resourceSet);

        query = new StreamQuery();

        classQNJava2File = new HashMap<>();
        resourceSet.getAllContents().forEachRemaining(notifier -> {
            if (notifier instanceof Java2File) {
                Java2File java2File = (Java2File) notifier;
                try {
                    classQNJava2File.put(ParserUtils.getClassQNFromFile(new File(java2File.getJavaUnit().getOriginalFilePath())), java2File);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Couldn't add the java2file "+java2File.getFile().toString()+" in the map");
                }
            }
        });

    }

    /**
     * Parse the trace file line by line.
     * Depending of the trace type, will either find the test being executed, or the SUT being executed, or the statement being executed
     *
     * @return a impact analysis model
     */
    public Resource parse() {

        //Generate the right trace reader
        try {
            switch (Configuration.getProperty("trace")) {
                case "file":
                    reader = new BufferedReader(new FileReader(trace));
                    break;
                case "queue":
                    reader = new QueueReader(trace);
                    break;
                default:
                    reader = (trace.isFile()) ? new BufferedReader(new FileReader(trace)) : new QueueReader(trace);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read the traces", e);
        }

        //Parsing the trace
        long currLine = 0;
        String line ;
        try {
            while ((line = reader.readLine()) != null) {

                //ParserUtils.printProgress(startTime, maxLine, currLine);
                currLine += 1;
                String[] split = line.split(":");

                try {
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

                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Couldn't parse the traces at line: " + currLine, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not parse trace file", e);
        }

        try {
            outputModel.save(Collections.EMPTY_MAP);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save the analysis model", e);
        }

        LOGGER.info("Model available at URI: " + outputModel.getURI());
        return outputModel;
    }

    /**
     * Update the current tests being executed.
     *
     * @param testQN the test qualified name: pkg1.pk2.pkg3.MyClass
     * @param method the method name: MyMethod
     */
    private void updateTest(String testQN, String method) {

        if (currentTest == null || !currentTestQN.equals(testQN)) {
            LOGGER.fine("Updating the current test class: " + testQN);
            //currentTest = getJava2FileFromJava2Directory(currentTestPackage, testQN);
            currentTest = getJava2File(testQN);
            currentTestQN = testQN;
        }

        String fullMethodQN = testQN.concat(":").concat(method);
        if ((currentTestMethod == null || !fullMethodQN.equals(currentTestMethodQN)) && currentTest != null) {
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

        if (currentTarget == null || !currentTargetQN.equals(targetQn)) {
            LOGGER.fine("Updating the current target class: " + targetQn);
            //currentTarget = getJava2FileFromJava2Directory(currentTargetPackage, targetQn);
            currentTarget = getJava2File(targetQn);
            currentTargetQN = targetQn;
        }

        if (!method.equals(currentTargetMethodQN)) {
            LOGGER.fine("Updating the current target method: " + method);
            currentTargetMethodQN = method;
            currentTargetMethod = null;
        }

    }

    /**
     * Get the Java2File using the QN of the class. If it's an internal class, we just use the container class name instead
     * @param name
     * @return
     */
    private Java2File getJava2File(String name) {
        while (name.contains("$")) {
            name = name.substring(0, name.lastIndexOf("$"));
        }

        return classQNJava2File.get(name);
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
     * Parse the {@link Java2File} children to file the node corresponding to the {@link MethodDeclaration} with the name given as a parameter
     *
     * @param java2File  a {@link Java2File}
     * @param lineNumber a {@link Integer} contained inside the method block
     * @return the {@link org.eclipse.gmt.modisco.java.ASTNode} with the {@link MethodDeclaration} as a node
     */
    private ASTNodeSourceRegion getMethodASTNodeFromJava2File(Java2File java2File, int lineNumber) {
        return java2File.getChildren().stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof AbstractMethodDeclaration &&
                        astNodeSourceRegion.getStartLine() <= lineNumber &&
                        astNodeSourceRegion.getEndLine() >= lineNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * Parse the {@link Java2File} children to file the node corresponding to the {@link MethodDeclaration} with the name given as a parameter
     *
     * @param java2File a {@link Java2File}
     * @param startPos  the start position of an element inside the method
     * @param endPos    the end position of an element inside the method
     * @return the {@link ASTNodeSourceRegion} with the {@link MethodDeclaration} as a node
     */
    private ASTNodeSourceRegion getMethodASTNodeFromJava2File(Java2File java2File, int startPos, int endPos) {
        return java2File.getChildren().stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof AbstractMethodDeclaration &&
                        astNodeSourceRegion.getStartPosition() <= startPos &&
                        astNodeSourceRegion.getEndPosition() >= endPos)
                .findFirst()
                .orElse(null);
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
     * Create an impact relation between a statement and a test method, using the statement line number after finding the statement in the model
     *
     * @param lineNumber the line number
     */
    private void updateStatementUsingLine(int lineNumber) {
        Collection<ASTNodeSourceRegion> astNodeSourceRegions = query.queryLine(lineNumber, lineNumber, currentTarget);
        astNodeSourceRegions.forEach(astNodeSourceRegion -> {
            if (currentTargetMethod == null || !(currentTargetMethod.getStartLine() <= lineNumber && currentTargetMethod.getEndLine() >= lineNumber)) {
                currentTargetMethod = getMethodASTNodeFromJava2File(currentTarget, lineNumber);
                createRunByAnalysis(currentTargetMethod, currentTestMethod);
            }

            createRunByAnalysis(astNodeSourceRegion, currentTestMethod);
        });
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
        Collection<ASTNodeSourceRegion> astNodeSourceRegions = query.queryPosition(startPos, endPos, currentTarget);
        astNodeSourceRegions.forEach(astNodeSourceRegion -> {
            if (currentTargetMethod == null || !(currentTargetMethod.getStartPosition() <= startPos && currentTargetMethod.getEndPosition() >= endPos)) {
                currentTargetMethod = getMethodASTNodeFromJava2File(currentTarget, startPos, endPos);
                createRunByAnalysis(currentTargetMethod, currentTestMethod);
            }
            createRunByAnalysis(astNodeSourceRegion, currentTestMethod);
        });
    }

    /**
     * Create an {@link Analysis} object pointing from a Source {@link ASTNodeSourceRegion} to a Target {@link ASTNodeSourceRegion}
     * Add this analysis to the Source {@link ASTNodeSourceRegion} and to the Target {@link ASTNodeSourceRegion}
     * Is usually a Statement pointing to a test {@link MethodDeclaration} node, or a SUT {@link MethodDeclaration} pointing to a test
     * {@link MethodDeclaration}.
     *
     * @param source an {@link ASTNode}
     * @param target an {@link ASTNodeSourceRegion}
     */
    private void createRunByAnalysis(ASTNodeSourceRegion source, ASTNodeSourceRegion target) {
        //We create an analysis if it does not already exist.
        if (source != null
                && target != null
                && target.getNode() instanceof MethodDeclaration
                && source.getAnalysis().stream()
                .map(eObject -> (Analysis) eObject)
                .noneMatch(analysis -> analysis.getTarget().contains(target.getNode()))) {

            Analysis analysis = ModelFactory.eINSTANCE.createAnalysis();
            analysis.setName(ANALYSIS_NAME);
            analysis.setSource(source.getNode());
            analysis.getTarget().add(target.getNode());
            source.getAnalysis().add(analysis);
            //target.getAnalysis().add(analysis);
            outputModel.getContents().add(analysis);
        } else {
            LOGGER.fine("Link already existing. Not adding.");
        }
    }
}
