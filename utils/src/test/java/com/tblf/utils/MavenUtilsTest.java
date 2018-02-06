package com.tblf.utils;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class MavenUtilsTest {

    @Test
    public void checkCompilePom() throws IOException {
        ModelUtils.unzip(new File("src/test/resources/projects/SimpleProject.zip"));
        File project = new File("src/test/resources/projects/SimpleProject");

        Assert.assertTrue(! (new File(project, "target")).exists());

        if (System.getProperty("maven.home") == null)
            System.setProperty("maven.home", Configuration.getProperty("MAVEN_HOME"));

        MavenUtils.compilePom(project);

        Assert.assertTrue(new File(project, "target").exists());
        Assert.assertTrue(new File(project, "target").isDirectory());
        Assert.assertTrue(new File(project, ".classpath").exists());

        FileUtils.deleteDirectory(project);
    }
}
