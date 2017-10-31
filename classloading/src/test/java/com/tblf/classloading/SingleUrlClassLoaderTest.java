package com.tblf.classloading;

import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Thibault on 20/09/2017.
 * Test suite verifying that the SingleUrlClassLoader class does what it is supposed to do
 */
public class SingleUrlClassLoaderTest {

    @Test
    public void loadSingleJar() throws MalformedURLException {
        File file = new File("src/test/resources/jars/ByteCodeModel.jar");

        if (!file.exists()) {
            Assert.fail();
        }

        SingleURLClassLoader singleURLClassLoader = SingleURLClassLoader.getInstance();

        singleURLClassLoader.addURLs(new URL[]{file.toURI().toURL()});

        try {
            Class aClass = singleURLClassLoader.getUrlClassLoader().loadClass("Main.Main");
            Assert.assertEquals("Main.Main", aClass.getName());
        } catch (Throwable t) {
            Assert.fail(t.toString());
        }
    }

    @Test
    public void loadMultipleClasses() throws IOException {
        File zip1 = new File("src/test/resources/binaries/junit.zip");
        File zip2 = new File("src/test/resources/binaries/main.zip");

        ModelUtils.unzip(zip1);
        ModelUtils.unzip(zip2);
        File directory1 = new File("src/test/resources/binaries/junit");

        File file = new File(directory1, "bin");
        File file2 = new File("src/test/resources/binaries/main");

        if (!file.exists() ||!file2.exists()) {
            Assert.fail("Can't find the folders");
        }

        SingleURLClassLoader singleURLClassLoader = SingleURLClassLoader.getInstance();

        singleURLClassLoader.addURLs(new URL[]{file2.toURI().toURL()});

        try {
            Class aClass = singleURLClassLoader.getUrlClassLoader().loadClass("Main.Main");
            Assert.assertEquals("Main.Main", aClass.getName());
        } catch (Throwable t) {
            Assert.fail(t.toString());
        }

        singleURLClassLoader.addURLs(new URL[]{file.toURI().toURL()});
        try {
            Class aClass = singleURLClassLoader.getUrlClassLoader().loadClass("Main.Main");
            Assert.assertEquals("Main.Main", aClass.getName());

            aClass = singleURLClassLoader.getUrlClassLoader().loadClass("org.junit.After");
            Assert.assertEquals("org.junit.After", aClass.getName());
        } catch (Throwable t) {
            Assert.fail(t.toString());
        }

        FileUtils.deleteDirectory(directory1);
        FileUtils.deleteDirectory(file2);
    }
}
