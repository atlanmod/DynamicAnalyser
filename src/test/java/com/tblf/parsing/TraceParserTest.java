package com.tblf.parsing;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.CompilationUnit;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaFactory;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.modisco.java.composition.javaapplication.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Created by Thibault on 02/10/2017.
 */
public class TraceParserTest {
    private File trace;
    private ResourceSet resourceSet;

    @Before
    public void setUp() throws IOException {
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

        test.getBodyDeclarations().add(init);
        test.getBodyDeclarations().add(myTest);
        target.getBodyDeclarations().add(myMethod);

        Java2Directory java2Directory = JavaapplicationFactory.eINSTANCE.createJava2Directory();
        java2Directory.setJavaPackage(aPackage);

        Java2File testFile = JavaapplicationFactory.eINSTANCE.createJava2File();
        CompilationUnit testUnit = JavaFactory.eINSTANCE.createCompilationUnit();
        testUnit.setName("Test.java");
        testFile.setUnit(testUnit);

        Java2File targetFile = JavaapplicationFactory.eINSTANCE.createJava2File();
        CompilationUnit targetUnit = JavaFactory.eINSTANCE.createCompilationUnit();
        targetUnit.setName("Target.java");
        targetFile.setUnit(targetUnit);

        java2Directory.getJava2FileChildren().add(testFile);
        java2Directory.getJava2FileChildren().add(targetFile);

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

        resourceSet = new ResourceSetImpl();
        resourceSet.setResourceFactoryRegistry(reg);

        Resource resource = resourceSet.createResource(URI.createURI("MyModel.xmi"));
        resource.getContents().add(java2Directory);
    }

    @Test
    public void checkParse() throws IOException {
        TraceParser traceParser = new TraceParser(trace, resourceSet);
        traceParser.parse();
    }
}
