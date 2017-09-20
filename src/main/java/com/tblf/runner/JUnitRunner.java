package com.tblf.runner;

import org.junit.runner.JUnitCore;

import java.util.List;

/**
 * Created by Thibault on 19/09/2017.
 */
public class JUnitRunner {

    /**
     * Run the tests of a given JUnit test list after loading them in the System {@link ClassLoader}
     * @param tests
     */
    public void runTests(List<String> tests) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        JUnitCore jUnitCore = new JUnitCore();

        tests.forEach(s -> {
            try {
                jUnitCore.run(classLoader.loadClass(s));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

    }
}
