package com.tblf.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;

public class MavenUtilsTest {

    @Test
    public void checkCompilePom() throws IOException {
        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/projects/SimpleProject.zip"));
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
        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/projects/SimpleProject.zip"));
        File project = new File("src/test/resources/projects/SimpleProject");

        Assert.assertTrue(! (new File(project, "target")).exists());

        File pom = new File(project, "pom.xml");
        Assert.assertTrue(pom.exists());

        Assert.assertFalse(IOUtils.toString(new FileInputStream(pom), Charset.defaultCharset()).contains("THE_OPTION"));
        MavenUtils.addJVMOptionsToSurefireConfig(pom, "THE_OPTION");

        Assert.assertTrue(IOUtils.toString(new FileInputStream(pom), Charset.defaultCharset()).contains("THE_OPTION"));
        FileUtils.deleteDirectory(project);
    }

    @Test
    public void checkMavenAddDependency() throws IOException {
        Model source = new Model();

        source.setArtifactId("artifactId");
        source.setGroupId("groupId");
        source.setVersion("1.0.0");

        File folder = new File("src/test/resources/checkMavenAddDependency");

        assert folder.exists() || folder.mkdir();

        File pomSource = new File(folder, "pomSource.xml");

        assert pomSource.exists() || pomSource.createNewFile();

        new MavenXpp3Writer().write(new FileOutputStream(pomSource), source);

        String pomSourceAsString = FileUtils.readFileToString(pomSource, Charset.defaultCharset());

        Assert.assertFalse(pomSourceAsString.contains("dependencyArtifactId"));
        Assert.assertFalse(pomSourceAsString.contains("dependencyGroupId"));
        Assert.assertFalse(pomSourceAsString.contains("2.0.0"));

        Model depToAdd = new Model();

        depToAdd.setArtifactId("dependencyArtifactId");
        depToAdd.setGroupId("dependencyGroupId");
        depToAdd.setVersion("2.0.0");

        File pom = new File(folder, "pom.xml");

        assert pom.exists() || pom.createNewFile();

        new MavenXpp3Writer().write(new FileOutputStream(pom), depToAdd);

        MavenUtils.addDependencyToPom(pomSource, pom);

        pomSourceAsString = FileUtils.readFileToString(pomSource, Charset.defaultCharset());

        Assert.assertTrue(pomSourceAsString.contains("dependencyArtifactId"));
        Assert.assertTrue(pomSourceAsString.contains("dependencyGroupId"));
        Assert.assertTrue(pomSourceAsString.contains("2.0.0"));

    }
}
