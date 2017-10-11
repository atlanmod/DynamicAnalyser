package com.tblf.business;

import com.tblf.util.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ManagerTest {

    @Before
    public void setUp() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }
    }

    @Test
    public void checkManagerBCI() {
        Configuration.setProperty("mode", "BYTECODE");
        Configuration.setProperty("binaries", "/");

        Manager manager = new Manager();
        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(new File("src/test/resources/binaries/assertj"));

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkManagerSCI() {
        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(new File("src/test/resources/fullProject/SimpleProject"));

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkParse() {
        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(new File("src/test/resources/fullProject/SimpleProject"));

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);

            manager.parseTraces(new File("src/test/resources/fullProject/SimpleProject"));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
