package com.tblf.classloading;

import com.tblf.utils.ModelUtils;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ByteCodeUrlClassLoaderTest {

    @Test
    public void loadSingleJarWithNoDependency() throws IOException, ClassNotFoundException {

        File file = new File("src/test/resources/jars/ByteCodeModel.jar");

        if (!file.exists()) {
            Assert.fail("Jar or class file does not exist");
        }

        URL[] urls = new URL[]{file.toURI().toURL()};
        Assert.assertFalse(new FastClasspathScanner().scan().getNamesOfAllClasses().contains("Main.Main"));

        URLClassLoader urlClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

        Assert.assertTrue(new FastClasspathScanner().addClassLoader(urlClassLoader).scan().getNamesOfAllClasses().contains("Main.Main"));

        try {
            Class aClass = urlClassLoader.loadClass("Main.Main");
            Assert.assertEquals(aClass.getName(), "Main.Main");
        } catch (ClassNotFoundException c) {
            Assert.fail("Can't find the file");
        }
    }

    @Test
    public void loadSingleClassFileWithNoDependency() throws IOException {
        ModelUtils.unzip(new File("src/test/resources/binaries/main.zip"));

        File file = new File("src/test/resources/binaries/main");

        if (!file.exists()) {
            Assert.fail("Jar or class file does not exist");
        }

        URL[] urls = new URL[]{file.toURI().toURL()};

        Assert.assertFalse(new FastClasspathScanner().scan().getNamesOfAllClasses().contains("Main.Main"));

        URLClassLoader urlClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

        Assert.assertTrue(new FastClasspathScanner().addClassLoader(urlClassLoader).scan().getNamesOfAllClasses().contains("Main.Main"));

        try {
            Class aClass = urlClassLoader.loadClass("Main.Main");
            Assert.assertEquals(aClass.getName(), "Main.Main");
        } catch (ClassNotFoundException c) {
            Assert.fail("Can't find the class");
        }

        FileUtils.deleteDirectory(file);
    }



}
