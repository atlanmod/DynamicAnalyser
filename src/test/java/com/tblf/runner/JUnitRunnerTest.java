package com.tblf.runner;

import com.tblf.parsing.ModelParser;
import com.tblf.util.ModelUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

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
        modelParser.parse(ModelUtils.loadModel(new File("src/test/resources/junit_java.xmi")));

        JUnitRunner jUnitRunner = new JUnitRunner();
        jUnitRunner.runTests(modelParser.getTests());

    }
}
