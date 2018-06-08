package com.tblf.gitdiff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class GitDiffManagerTest {

    @Test
    public void checkGetMethodUsingSignature() throws IOException {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }
        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/fullprojects/SimpleProject3.zip"));

        File file = new File("src/test/resources/fullprojects/SimpleProject");
        assert file.exists();

        ResourceSet resourceSet = ModelUtils.buildResourceSet(file);

        Java2File j2File = resourceSet.getResources().stream()
                .map(resource -> resource.getContents())
                .flatMap(Collection::stream)
                .filter(eObject -> eObject instanceof Java2File)
                .map(eObject -> ((Java2File) eObject))
                .filter(java2File -> java2File.getJavaUnit().getOriginalFilePath().endsWith("/App.java"))
                .findAny().get();

        CompilationUnit compilationUnit = JavaParser.parse(new File(file, "src/main/java/com/tblf/SimpleProject/App.java"));
        compilationUnit.getChildNodesByType(MethodDeclaration.class).forEach(methodDeclaration -> System.out.println(methodDeclaration.getSignature()));

        j2File.getChildren().stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof org.eclipse.gmt.modisco.java.MethodDeclaration)
                .map(astNodeSourceRegion -> ((org.eclipse.gmt.modisco.java.MethodDeclaration) astNodeSourceRegion.getNode()))
                .forEach(methodDeclaration -> {
                    System.out.println(ModelUtils.getMethodSignature(methodDeclaration));
                    methodDeclaration.getParameters().forEach(singleVariableDeclaration -> System.out.println(singleVariableDeclaration.getType().getType().getName()));
                });

        JavaParser.parse(new File("/home/thibault/Documents/git/Dynamic-Analyser/diff/src/test/resources/fullprojects/SimpleProject/src/main/java/com/tblf/SimpleProject/App.java")).getChildNodesByType(MethodDeclaration.class).forEach(methodDeclaration -> System.out.println("THE METHOD "+methodDeclaration.getSignature().asString()));
        FileUtils.deleteDirectory(file);
    }
}
