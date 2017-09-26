package com.tblf.util;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.Model;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

/**
 * Unit test for simple App.
 */
public class ModelUtilsTest {

    @Test
    public void checkCreateModel() {
        File f = new File("src/test/resources/junit_java.xmi");
        try {
            Resource resource = ModelUtils.loadModel(f);
            Model model = (Model) resource.getContents().get(0);
            Assert.assertEquals(model.getName(), "junit");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void checkCreateModelWithoutFile() throws Exception {
        File f = new File("");
        Resource resource = ModelUtils.loadModel(f);

    }

    @Test
    public void getQualifiedNameTest() throws Exception {

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
    public void testZipModel() {
        File modelAsZip = new File("src/test/resources/junit_java.zip");

        try {
            Resource resource = ModelUtils.loadModelFromZip(modelAsZip);
            Model model = (Model) resource.getContents().get(0);

            Assert.assertEquals("junit", model.getName());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
