package com.tblf.classLoading;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URL;

/**
 * Created by Thibault on 20/09/2017.
 */
public class InstURLClassLoaderTest {

    @Test
    public void checkLoadBytes() throws IOException, ClassNotFoundException {
        InstURLClassLoader instURLClassLoader = new InstURLClassLoader(new URL[]{});
        File file = new File("src/test/resources/binaries/main/Main/Main.class");
        if (!file.exists()){
            Assert.fail();
        }

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bytes = IOUtils.toByteArray(fileInputStream);

        instURLClassLoader.loadBytes(bytes, "Main.Main");

        Assert.assertNotNull(instURLClassLoader.loadClass("Main.Main"));
        Assert.assertEquals("Main.Main", instURLClassLoader.loadClass("Main.Main").getName());
    }
}
