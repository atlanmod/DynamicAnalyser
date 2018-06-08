package com.tblf.utils;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.*;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaFactory;
import org.eclipse.modisco.java.composition.javaapplication.Java2Directory;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Collection;

/**
 * Unit test for simple App.
 */
public class ModelUtilsTest {

    @Test
    public void checkCreateModel() {
        File f = new File("src/test/resources/models/junit_java.zip");
        try {
            Resource resource = ModelUtils.loadModelFromZip(f);
            assert resource != null;
            Model model = (Model) resource.getContents().get(0);
            Assert.assertEquals(model.getName(), "junit");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkGetTests() throws IOException {
        File f = new File("src/test/resources/models/junit_java.zip");
        Resource resource = ModelUtils.loadModelFromZip(f);
        assert resource != null;
        Model model = (Model) resource.getContents().get(0);
        Assert.assertEquals(model.getName(), "junit");

        Collection collection = ModelUtils.queryForTestClasses(resource);

        Assert.assertTrue(collection.size() > 400);
    }

    @Test
    public void checkGetTestMethods() throws IOException {
        File f = new File("src/test/resources/models/junit_java.zip");
        Resource resource = ModelUtils.loadModelFromZip(f);

        assert resource != null;
        Model model = (Model) resource.getContents().get(0);

        Assert.assertEquals(model.getName(), "junit");

        Collection collection = ModelUtils.getAllTestMethods(model);

        Assert.assertTrue(collection.size() > 1200);
    }

    @Test
    public void checkGetAllClasses() throws IOException {
        File f = new File("src/test/resources/models/junit_java.zip");
        Resource resource = ModelUtils.loadModelFromZip(f);
        assert resource != null;
        Model model = (Model) resource.getContents().get(0);
        Assert.assertEquals(model.getName(), "junit");

        Collection collection = ModelUtils.queryForAllClasses(resource);

        Assert.assertTrue(collection.size() > 1000);
    }

    @Test(expected = NoSuchFileException.class)
    public void checkCreateModelWithoutFile() throws Exception {
        File f = new File("");
        ModelUtils.loadModel(f);
    }

    @Test
    public void checkGetQualifiedName() throws Exception {

        Model model = JavaFactory.eINSTANCE.createModel();
        model.setName("model");
        Package pkg1 = JavaFactory.eINSTANCE.createPackage();
        pkg1.setName("pkg1");
        Package pkg2 = JavaFactory.eINSTANCE.createPackage();
        pkg2.setName("pkg2");
        Package pkg3 = JavaFactory.eINSTANCE.createPackage();
        pkg3.setName("pkg3");

        ClassDeclaration clazz = JavaFactory.eINSTANCE.createClassDeclaration();
        clazz.setName("clazz");

        ClassDeclaration interClazz = JavaFactory.eINSTANCE.createClassDeclaration();
        interClazz.setName("internClazz");

        clazz.getBodyDeclarations().add(interClazz);
        pkg3.getOwnedElements().add(clazz);
        pkg2.getOwnedPackages().add(pkg3);
        pkg1.getOwnedPackages().add(pkg2);
        model.getOwnedElements().add(pkg1);

        Assert.assertEquals("pkg1.pkg2.pkg3.clazz$internClazz", ModelUtils.getQualifiedName(interClazz));
        Assert.assertEquals("pkg1.pkg2.pkg3.clazz", ModelUtils.getQualifiedName(clazz));
    }

    @Test
    public void checkZipModel() {
        File modelAsZip = new File("src/test/resources/models/junit_java.zip");

        try {
            Resource resource = ModelUtils.loadModelFromZip(modelAsZip);

            assert resource != null;
            Model model = (Model) resource.getContents().get(0);

            Assert.assertEquals("junit", model.getName());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkGetZippedJavaApplicationModel() throws IOException {
        File rootModelFile = new File("src/test/resources/models/java2kdmFragments.zip");
        Assert.assertTrue(rootModelFile.exists());

        Resource resource = ModelUtils.loadJavaApplicationModelFromZip(rootModelFile);

        Assert.assertNotNull(resource.getContents().get(0));

        Java2Directory java2Directory = (Java2Directory) resource.getContents().get(0);

        Assert.assertNotNull(java2Directory);
    }

    @Test
    public void checkGetMethodSignature() throws IOException {
        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/projects/SimpleProject3.zip"));

        File file = new File("src/test/resources/projects/SimpleProject");
        assert file.exists();

        ResourceSet resourceSet = ModelUtils.buildResourceSet(file);

        Java2File j2File = resourceSet.getResources().stream()
                .map(Resource::getContents)
                .flatMap(Collection::stream)
                .filter(eObject -> eObject instanceof Java2File)
                .map(eObject -> ((Java2File) eObject))
                .filter(java2File -> java2File.getJavaUnit().getOriginalFilePath().endsWith("/App.java"))
                .findAny().get();

        MethodDeclaration method =
                j2File.getChildren().stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof org.eclipse.gmt.modisco.java.MethodDeclaration)
                .map(astNodeSourceRegion -> ((org.eclipse.gmt.modisco.java.MethodDeclaration) astNodeSourceRegion.getNode()))
                .filter(methodDeclaration -> methodDeclaration.getName().equals("method") && methodDeclaration.getParameters().size() > 0 )
                .findFirst().get();

        Assert.assertEquals("method(String, String)", ModelUtils.getMethodSignature(method));

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkGetOverridenMethod() throws IOException {
        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/projects/SimpleProject3.zip"));

        File file = new File("src/test/resources/projects/SimpleProject");
        assert file.exists();

        ResourceSet resourceSet = ModelUtils.buildResourceSet(file);

        Java2File j2File = resourceSet.getResources().stream()
                .map(Resource::getContents)
                .flatMap(Collection::stream)
                .filter(eObject -> eObject instanceof Java2File)
                .map(eObject -> ((Java2File) eObject))
                .filter(java2File -> java2File.getJavaUnit().getOriginalFilePath().endsWith("/App.java"))
                .findAny().get();

        System.out.println(j2File.getUnit());

        AbstractTypeDeclaration abstractTypeDeclaration = j2File.getJavaUnit().getTypes().stream().findFirst().orElseThrow(() -> new IOException("Type Declaration not found"));

        FileUtils.deleteDirectory(file);
    }

    @After
    public void cleanUp() throws IOException {
        File file = new File("src/test/resources");
        Assert.assertTrue(file.exists());

        Files.walk(file.toPath()).filter(path -> path.toString().endsWith(".xmi")).forEach(path -> path.toFile().delete());

        Files.walk(file.toPath()).filter(path -> path.toFile().isDirectory() && path.toFile().listFiles().length == 0).forEach(path -> path.toFile().delete());
    }

}