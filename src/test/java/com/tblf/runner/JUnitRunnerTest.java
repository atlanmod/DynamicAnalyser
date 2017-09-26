package com.tblf.runner;

import com.tblf.parsing.ModelParser;
import com.tblf.util.ModelUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 19/09/2017.
 */
public class JUnitRunnerTest {

    @Test
    public void runJUnitTestSuite() throws Exception {
        File file = new File("src/test/resources/binaries/junit/bin");
        if (! file.exists()){
            Assert.fail("Cannot find the junit binaries");
        }

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, ClassLoader.getSystemClassLoader());

        ModelParser modelParser = new ModelParser();
        modelParser.parse(ModelUtils.loadModelFromZip(new File("src/test/resources/junit_java.zip")));

        JUnitRunner jUnitRunner = new JUnitRunner(urlClassLoader);
        jUnitRunner.runTests(modelParser.getTests().keySet().stream().filter(s -> s.contains("org.junit.rules")).collect(Collectors.toList()));
    }

    @Test
    public void runJUnitTestClass() throws MalformedURLException, ClassNotFoundException {
        File file = new File("src/test/resources/binaries/simpleProj");
        if (! file.exists()){
            Assert.fail("Cannot find the junit binaries");
        }

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, ClassLoader.getSystemClassLoader());

        JUnitCore.runClasses(urlClassLoader.loadClass("com.tblf.SimpleProject.AppTest"));
    }
}
