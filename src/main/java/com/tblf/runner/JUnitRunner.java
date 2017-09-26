package com.tblf.runner;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by Thibault on 19/09/2017.
 */
public class JUnitRunner {
    private ClassLoader classLoader;
    private static final Logger LOGGER = Logger.getAnonymousLogger();

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

        LOGGER.info("Running "+tests.size()+" test suites");
        JUnitCore jUnitCore = new JUnitCore();

        tests.forEach(s -> {
            try {
                LOGGER.info("Running test "+s);
                Result result = jUnitCore.run(classLoader.loadClass(s));
                LOGGER.info(RunnerUtils.results(result));
            } catch (Throwable e) {
                LOGGER.warning("Couldn't run the tests of class: "+s+" : "+e.getMessage());
            }
        });


    }
}
