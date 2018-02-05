package com.tblf.business;

import com.tblf.utils.Configuration;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

@Ignore
public class ManagerTest {

    @Test
    public void checkParseModel() throws IOException, NoSuchFieldException, IllegalAccessException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);
        File file = new File("src/test/resources/fullprojects/SimpleProject");

        Manager manager = new Manager();
        manager.buildModel(file);

        Field f = manager.getClass().getDeclaredField("sutClasses");
        f.setAccessible(true);
        Collection c = (Collection) f.get(manager);
        Assert.assertEquals(1, c.size());

        f = manager.getClass().getDeclaredField("testClasses");
        f.setAccessible(true);
        c = (Collection) f.get(manager);
        Assert.assertEquals(1, c.size());

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkManagerSCI() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        File file = new File("src/test/resources/fullprojects/SimpleProject");
        manager.buildModel(file);
        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(file);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkParseSCI() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        File file = new File("src/test/resources/fullprojects/SimpleProject");
        manager.buildModel(file);
        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(file);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);

            Resource resource = manager.parseTraces(trace);
            Assert.assertNotNull(resource);

            Assert.assertEquals(10, resource.getContents().size());

            /*
             Constructor1 --> TestApp
             line constructor1 --> TestApp
             method --> TestApp
             line 1 method --> TestApp
             line 2 method --> TestApp
             Constructor2 --> TestApp2
             line constructor2 --> TestApp2
             method --> TestApp2
             line 1 method --> TestApp2
             line 2 method --> TestApp2
             */
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkParseBCI() throws IOException {

        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "BYTECODE");
        Configuration.setProperty("sutBinaries", "/target/classes");
        Configuration.setProperty("testBinaries", "/target/test-classes");

        Manager manager = new Manager();
        File file = new File("src/test/resources/fullprojects/SimpleProject");
        manager.buildModel(file);
        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(file);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);

            Resource resource = manager.parseTraces(trace);

            Assert.assertEquals(10, resource.getContents().size());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(file);
    }
}
