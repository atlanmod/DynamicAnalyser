package com.tblf.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

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

    @Test
    public void checkModifyPom() throws IOException {
        ModelUtils.unzip(new File("src/test/resources/projects/SimpleProject.zip"));
        File project = new File("src/test/resources/projects/SimpleProject");

        Assert.assertTrue(! (new File(project, "target")).exists());

        File pom = new File(project, "pom.xml");
        Assert.assertTrue(pom.exists());

        Assert.assertFalse(IOUtils.toString(new FileInputStream(pom), Charset.defaultCharset()).contains("THE_OPTION"));
        MavenUtils.addJVMOptionsToSurefireConfig(pom, "THE_OPTION");

        Assert.assertTrue(IOUtils.toString(new FileInputStream(pom), Charset.defaultCharset()).contains("THE_OPTION"));
        FileUtils.deleteDirectory(project);
    }
}
