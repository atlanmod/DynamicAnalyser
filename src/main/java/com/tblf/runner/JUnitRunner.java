package com.tblf.runner;

import org.junit.runner.JUnitCore;

import java.util.Collection;

/**
 * Created by Thibault on 19/09/2017.
 */
public class JUnitRunner {

    private ClassLoader classLoader;

    public JUnitRunner() {
        classLoader = ClassLoader.getSystemClassLoader();
    }

    public JUnitRunner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Run the tests of a given JUnit test list after loading them in the System {@link ClassLoader}
     * @param tests
     */
    public void runTests(Collection<String> tests) {
        JUnitCore jUnitCore = new JUnitCore();

        tests.forEach(s -> {
            try {
                jUnitCore.run(classLoader.loadClass(s));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                //TODO
            }
        });

    }
}
