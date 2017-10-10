package com.tblf.main;

import com.tblf.util.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by Thibault on 21/09/2017.
 */
public class Apptest {

    @Before
    public void setUp() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }
    }

    @Test
    public void checkMainBCI() throws IOException {
        Configuration.setProperty("mode", "BYTECODE");
        Configuration.setProperty("binaries", "/");
        try {
            long before = System.currentTimeMillis();
            App.main(new String[]{"src/test/resources/binaries/assertj/"});

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkMainSCI() {
        Configuration.setProperty("mode", "SOURCECODE");
        try {
            long before = System.currentTimeMillis();
            App.main(new String[]{"src/test/resources/fullProject/SimpleProject"});

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
