package com.tblf.classLoading;

import com.tblf.parsing.ModelParser;
import com.tblf.util.ModelUtils;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ByteCodeUrlClassLoaderTest {

    @Test
    public void loadSingleJarWithNoDependency() throws IOException, ClassNotFoundException {

        File file = new File("src/test/resources/ByteCodeModel.jar");

        if (!file.exists()) {
            Assert.fail("Jar or class file does not exist");
        }

        URL[] urls = new URL[]{file.toURI().toURL()};

        URLClassLoader urlClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

        try {
            Class aClass = urlClassLoader.loadClass("Main.Main");
            Assert.assertEquals(aClass.getName(), "Main.Main");
        } catch (ClassNotFoundException c) {
            Assert.fail("Can't find the file");
        }
    }

    @Test
    public void loadSingleClassFileWithNoDependency() throws MalformedURLException {
        File file = new File("src/test/resources/binaries/main");

        if (!file.exists()) {
            Assert.fail("Jar or class file does not exist");
        }

        URL[] urls = new URL[]{file.toURI().toURL()};

        URLClassLoader urlClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

        try {
            Class aClass = urlClassLoader.loadClass("Main.Main");
            Assert.assertEquals(aClass.getName(), "Main.Main");
        } catch (ClassNotFoundException c) {
            Assert.fail("Can't find the class");
        }
    }

    @Test
    public void loadJUnitBins() throws Exception {
        File file = new File("src/test/resources/binaries/junit/bin");
        if (! file.exists()){
            Assert.fail("Cannot find the junit binaries");
        }

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, ClassLoader.getSystemClassLoader());

        Assert.assertTrue(new FastClasspathScanner()
                .scan()
                .getNamesOfAllClasses()
                .stream()
                .anyMatch(s -> s.contains("org.junit.runners.Parameterized")));

        ModelParser modelParser = new ModelParser();
        modelParser.parse(ModelUtils.loadModel(new File("src/test/resources/junit_java.xmi")));

        modelParser.getTargets().forEach((s, f) -> {
            try {
                urlClassLoader.loadClass(s);
            } catch (IllegalAccessError | ClassNotFoundException e) {
                System.out.println("cannot find "+s);
            }
        });

        modelParser.getTests().forEach((s, f) -> {
            try {
                urlClassLoader.loadClass(s);
            } catch (IllegalAccessError | ClassNotFoundException e) {
                System.out.println("cannot find "+s);
            }
        });
    }

}
