package com.tblf.parsing;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.*;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaFactory;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.modisco.java.composition.javaapplication.*;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.eclipse.modisco.kdm.source.extension.CodeUnit2File;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by Thibault on 02/10/2017.
 */
public class TraceParserTest {
    private File trace;
    private ResourceSet resourceSet;

    @Before
    public void setUp() throws IOException {

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        JavaapplicationPackage.eINSTANCE.eClass();
        JavaPackage.eINSTANCE.eClass();
        KdmPackage.eINSTANCE.eClass();

        StringBuilder sb = new StringBuilder();
        sb.append("&:com.tblf.pk1.Test:<init>\n");
        sb.append("&:com.tblf.pk1.Test:myTest\n");
        sb.append("%:com.tblf.pk1.Target:myMethod\n");
        sb.append("?:5\n");
        sb.append("?:6");

        trace = File.createTempFile("tmpTrace", ".extr");

        Files.write(trace.toPath(), sb.toString().getBytes());

        Package aPackage = JavaFactory.eINSTANCE.createPackage();
        aPackage.setName("com.tblf.pk1");

        ClassDeclaration test = JavaFactory.eINSTANCE.createClassDeclaration();
        test.setName("Test");
        ClassDeclaration target = JavaFactory.eINSTANCE.createClassDeclaration();
        target.setName("Target");

        MethodDeclaration init = JavaFactory.eINSTANCE.createMethodDeclaration();
        init.setName("<init>");
        MethodDeclaration myTest = JavaFactory.eINSTANCE.createMethodDeclaration();
        myTest.setName("myTest");
        MethodDeclaration myMethod = JavaFactory.eINSTANCE.createMethodDeclaration();
        myMethod.setName("myMethod");

        Statement statement1 = JavaFactory.eINSTANCE.createExpressionStatement();
        Statement statement2 = JavaFactory.eINSTANCE.createExpressionStatement();

        test.getBodyDeclarations().add(init);
        test.getBodyDeclarations().add(myTest);
        target.getBodyDeclarations().add(myMethod);

        ASTNodeSourceRegion testMethodNode = JavaapplicationFactory.eINSTANCE.createJavaNodeSourceRegion();
        testMethodNode.setStartLine(4);
        testMethodNode.setEndLine(6);
        testMethodNode.setNode(myTest);

        ASTNodeSourceRegion targetMethodNode = JavaapplicationFactory.eINSTANCE.createJavaNodeSourceRegion();
        targetMethodNode.setStartLine(4);
        targetMethodNode.setEndLine(7);
        targetMethodNode.setNode(myMethod);

        ASTNodeSourceRegion statementNode1 = JavaapplicationFactory.eINSTANCE.createJavaNodeSourceRegion();
        statementNode1.setStartLine(5);
        statementNode1.setEndLine(5);
        statementNode1.setStartPosition(12);
        statementNode1.setEndPosition(65);
        statementNode1.setNode(statement1);

        ASTNodeSourceRegion statementNode2 = JavaapplicationFactory.eINSTANCE.createJavaNodeSourceRegion();
        statementNode2.setStartLine(5);
        statementNode2.setEndLine(5);
        statementNode2.setStartPosition(12);
        statementNode2.setEndPosition(65);
        statementNode2.setNode(statement2);

        Java2Directory java2Directory = JavaapplicationFactory.eINSTANCE.createJava2Directory();
        java2Directory.setJavaPackage(aPackage);

        Java2File testFile = JavaapplicationFactory.eINSTANCE.createJava2File();
        CompilationUnit testUnit = JavaFactory.eINSTANCE.createCompilationUnit();
        testUnit.setName("Test.java");
        testFile.setUnit(testUnit);
        testFile.getChildren().addAll(List.of(testMethodNode));

        Java2File targetFile = JavaapplicationFactory.eINSTANCE.createJava2File();
        CompilationUnit targetUnit = JavaFactory.eINSTANCE.createCompilationUnit();
        targetUnit.setName("Target.java");
        targetFile.setUnit(targetUnit);
        targetFile.getChildren().addAll(List.of(targetMethodNode, statementNode1, statementNode2));

        java2Directory.getJava2FileChildren().add(testFile);
        java2Directory.getJava2FileChildren().add(targetFile);

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

        resourceSet = new ResourceSetImpl();
        resourceSet.setResourceFactoryRegistry(reg);

        Resource resource = resourceSet.createResource(URI.createURI("com.tblf.pk1_java2kdm.xmi"));
        resource.getContents().add(java2Directory);
    }

    @Test
    public void checkParse() throws IOException {
        TraceParser traceParser = new TraceParser(trace, resourceSet);
        traceParser.parse();
    }
}
