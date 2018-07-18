package com.tblf.junitrunner;

import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by Thibault on 19/09/2017.
 */
public class JUnitRunnerTest {

    @Test
    public void runJUnitTestClass() throws IOException, ClassNotFoundException {

        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/binaries/simpleProj.zip"));

        File file = new File("src/test/resources/binaries/simpleProj");
        if (! file.exists()){
            Assert.fail("Cannot find the binaries");
        }

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, ClassLoader.getSystemClassLoader());

        Configuration.setProperty("traceFile", "src/test/resources/runJUnitTestClass");

        JUnitCore.runClasses(urlClassLoader.loadClass("com.tblf.SimpleProject.AppTest"));

        FileUtils.deleteDirectory(file);
    }
}
