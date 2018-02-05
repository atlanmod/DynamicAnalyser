package com.tblf.junitrunner;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 19/09/2017.
 */
public class JUnitRunner {

    private static final Logger LOGGER = Logger.getLogger("JUnitRunner");
    private static final JUnitCore J_UNIT_CORE = new JUnitCore();

    private ClassLoader classLoader;

    public JUnitRunner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Run the tests of a given JUnit test list after loading them in the System {@link ClassLoader}
     *
     * @param tests a {@link Collection} of test class names
     */
    public void runTests(Collection<String> tests) {
        LOGGER.info("Running " + tests.size() + " test suites");

        List requests = tests.stream().map(s -> {
            Request request = null;
            try {
                request = Request.aClass(classLoader.loadClass(s));
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "couldn't load the test class " + s, e);
            }
            return new AbstractMap.SimpleEntry<>(s, request);
        }).collect(Collectors.toList());

        this.analyseResults(this.runRequests(requests));
    }

    /**
     * Run all the method in the @{@link Map.Entry} Collection. (MethodName, ClassName)
     *
     * @param entries a collection of Classes and methods
     */
    public void runTestMethods(Collection<Map.Entry<String, String>> entries) {
        LOGGER.info("Running" + entries.size() + " test methods");

        Collection<Map.Entry<String, Request>> requests = entries.stream().map(entry -> {
            Request request = null;
            try {
                Class aClass = classLoader.loadClass(entry.getValue());
                request = Request.method(aClass, entry.getKey());

            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "couldn't load the test class " + entry.getValue(), e);
            }
            return new AbstractMap.SimpleEntry<>(entry.getKey(), request);
        }).collect(Collectors.toList());

        this.analyseResults(this.runRequests(requests));
    }

    /**
     * Run a collection of {@link Request} and returns a collection of {@link Result}
     *
     * @param requests a collection of {@link Request}
     * @return a collection of {@link Result}
     */
    private Collection<Map.Entry<String, Result>> runRequests(Collection<Map.Entry<String, Request>> requests) {

        return requests
                .stream()
                .filter(stringRequestEntry -> stringRequestEntry.getKey() != null && stringRequestEntry.getValue() != null)
                .map(stringRequestEntry -> new AbstractMap.SimpleEntry<>(stringRequestEntry.getKey(), J_UNIT_CORE.run(stringRequestEntry.getValue())))
                .collect(Collectors.toList());
    }

    /**
     * Iterate over a collection of {@link Result} and log the overall results
     * @param results a {@link Collection} of {@link Result}
     */
    private void analyseResults(Collection<Map.Entry<String, Result>> results) {
        AtomicInteger failure = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger ignore = new AtomicInteger(0);

        results.forEach(result -> {
            failure.addAndGet(result.getValue().getFailureCount());
            success.addAndGet(result.getValue().getRunCount() - result.getValue().getFailureCount());
            ignore.addAndGet(result.getValue().getIgnoreCount());
            LOGGER.info(result.getKey()+": \n"+RunnerUtils.results(result.getValue()));
        });

        LOGGER.fine((failure.get() + success.get()) + " tests run : " + success.get() + " succeeded - " + failure.get() + " failed - " + ignore.get() + " ignored");
    }
}
