package com.tblf.classloading;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ByteCodeUrlClassLoaderTest {

    @Test
    public void loadSingleClassWithNoDependency() throws IOException, ClassNotFoundException {

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
}
