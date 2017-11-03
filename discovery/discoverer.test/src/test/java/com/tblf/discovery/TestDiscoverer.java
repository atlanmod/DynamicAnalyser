package com.tblf.discovery;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

@Ignore
public class TestDiscoverer {

    //FIXME: wont work on travis
    static final String URI = "/home/thibault/Documents/workspace/SimpleProject";

    @Before
    public void setUp() throws IOException {
        File project = new File(URI);
        Files.walk(project.toPath()).filter(path -> path.toString().endsWith(".xmi")).forEach(path -> path.toFile().delete());
    }

    @Test
    public void check() {
        //TODO
        try {
            Discoverer.generateFullModel("/home/thibault/Documents/workspace/SimpleProject");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertTrue(Arrays.asList(new File(URI).listFiles()).stream().filter(file -> file.getName().endsWith(".xmi")).count() > 0);
    }
}
