package com.tblf.classLoading;

import com.tblf.util.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by Thibault on 20/09/2017.
 */
public class InstURLClassLoaderTest {

    @Test
    public void checkLoadBytes() throws IOException, ClassNotFoundException {
        InstURLClassLoader instURLClassLoader = new InstURLClassLoader(new URL[]{});
        File zip = new File("src/test/resources/binaries/main.zip");
        ModelUtils.unzip(zip);

        File directory = new File("src/test/resources/binaries/main");

        File file = new File(directory, "Main/Main.class");
        if (!file.exists()){
            Assert.fail();
        }

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bytes = IOUtils.toByteArray(fileInputStream);

        instURLClassLoader.loadBytes(bytes, "Main.Main");

        Assert.assertNotNull(instURLClassLoader.loadClass("Main.Main"));
        Assert.assertEquals("Main.Main", instURLClassLoader.loadClass("Main.Main").getName());

        FileUtils.deleteDirectory(directory);
    }

    @Test
    public void testLoadAllFolder() throws IOException, ClassNotFoundException {
        File zip = new File("src/test/resources/binaries/assertj.zip");
        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/binaries/assertj");
        InstURLClassLoader instURLClassLoader = new InstURLClassLoader(new URL[]{file.toURI().toURL()});

        Assert.assertNotNull(instURLClassLoader.loadClass("org.assertj.core.api.AbstractArrayAssert"));

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void testLoadByteBufferNoName() throws IOException, ClassNotFoundException {
        File zip = new File("src/test/resources/binaries/assertj.zip");
        ModelUtils.unzip(zip);

        File directory = new File("src/test/resources/binaries/assertj");

        File file = new File(directory, "org/assertj/core/api/ArraySortedAssert.class");
        Assert.assertTrue(file.exists());

        FileChannel roChannel = new RandomAccessFile(file, "r").getChannel();
        ByteBuffer bb = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());

        InstURLClassLoader instURLClassLoader = new InstURLClassLoader(new URL[]{}, ClassLoader.getSystemClassLoader());
        instURLClassLoader.loadBytes(bb);

        Assert.assertNotNull(instURLClassLoader.loadClass("org.assertj.core.api.ArraySortedAssert"));

        FileUtils.deleteDirectory(directory);
    }


    @Test
    public void testLoadByteNoName() throws IOException, ClassNotFoundException {
        File zip = new File("src/test/resources/binaries/assertj.zip");
        ModelUtils.unzip(zip);

        File directory = new File("src/test/resources/binaries/assertj");

        File file = new File(directory, "org/assertj/core/api/ArraySortedAssert.class");

        Assert.assertTrue(file.exists());

        InstURLClassLoader instURLClassLoader = new InstURLClassLoader(new URL[]{}, ClassLoader.getSystemClassLoader());
        instURLClassLoader.loadBytes(IOUtils.toByteArray(new FileInputStream(file)), null);

        Assert.assertNotNull(instURLClassLoader.loadClass("org.assertj.core.api.ArraySortedAssert"));

        FileUtils.deleteDirectory(directory);
    }

}
