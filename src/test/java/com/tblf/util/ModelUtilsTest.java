package com.tblf.util;

import com.tblf.util.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.Model;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
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
    public void getQualifiedName() throws Exception {
        File f = new File("src/test/resources/junit_java.xmi");
        Resource resource = ModelUtils.loadModel(f);
        resource.getAllContents().forEachRemaining(eObject -> {
            if (eObject instanceof ClassDeclaration) {
                System.out.println(ModelUtils.getQualifiedName(eObject));
            }
        });


    }

}
