package com.tblf.business;

import com.tblf.util.Configuration;
import com.tblf.util.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ManagerTest {

    @Test
    public void checkManagerBCI() throws IOException {
        ModelUtils.unzip(new File("src/test/resources/binaries/assertj.zip"));

        File project = new File("src/test/resources/binaries/assertj");
        Configuration.setProperty("mode", "BYTECODE");
        Configuration.setProperty("sutBinaries", "/");
        Configuration.setProperty("testBinaries", "/");
        Manager manager = new Manager();
        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(project);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(project);
    }

    @Test
    public void checkManagerSCI() throws IOException {
        File zip = new File("src/test/resources/fullProject/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        File file = new File("src/test/resources/fullProject/SimpleProject");
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
        File zip = new File("src/test/resources/fullProject/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        File file = new File("src/test/resources/fullProject/SimpleProject");
        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(file);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);

            Resource resource = manager.parseTraces(trace);
            Assert.assertNotNull(resource);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkParseBCI() throws IOException {

        File zip = new File("src/test/resources/fullProject/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "BYTECODE");
        Configuration.setProperty("sutBinaries", "/target/classes");
        Configuration.setProperty("testBinaries", "/target/test-classes");

        Manager manager = new Manager();
        File file = new File("src/test/resources/fullProject/SimpleProject");

        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(file);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);

            manager.parseTraces(trace);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(file);
    }

}
