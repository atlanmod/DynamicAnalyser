package com.tblf.utils;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FileUtilsTest {
    private File directory;
    private File pom;

    @Before
    public void setUp() throws IOException {
        directory = new File("src/test/resources/pomDir");
        Assert.assertTrue(directory.exists() || directory.mkdir());

        pom = new File(directory, "pom.xml");
        if (pom.exists())
            pom.delete();

        Assert.assertTrue(pom.createNewFile());
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(directory);
    }

    @Test
    public void checkIsAnalysableFalse() throws IOException {

        String pomStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +"    <groupId>com.tblf</groupId>\n" +
                "    <artifactId>Analysis</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>pom</packaging>" +
                "</project>";

        org.apache.commons.io.FileUtils.write(pom, pomStr, "UTF-8");

        Assert.assertFalse(com.tblf.utils.FileUtils.isAnalysable(directory));
    }

    @Test
    public void checkIsAnalysableTrue() throws IOException {

        String pomStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +"    <groupId>com.tblf</groupId>\n" +
                "    <artifactId>Analysis</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>jar</packaging>" +
                "</project>";

        org.apache.commons.io.FileUtils.write(pom, pomStr, "UTF-8");

        Assert.assertTrue(com.tblf.utils.FileUtils.isAnalysable(directory));
    }

    @Test
    public void checkIsParentTrue() throws IOException {
        String pomStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +"    <groupId>com.tblf</groupId>\n" +
                "    <artifactId>Analysis</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>jar</packaging>" +
                "    <modules>\n" +
                "        <module>module1</module>\n" +
                "        <module>module2</module>\n" +
                "    </modules>\n" +
                "</project>";

        org.apache.commons.io.FileUtils.write(pom, pomStr, "UTF-8");

        Assert.assertTrue(com.tblf.utils.FileUtils.isParent(directory));
    }

    @Test
    public void checkIsParentFalse() throws IOException {
        String pomStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +"    <groupId>com.tblf</groupId>\n" +
                "    <artifactId>Analysis</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>jar</packaging>" +
                "</project>";

        org.apache.commons.io.FileUtils.write(pom, pomStr, "UTF-8");

        Assert.assertFalse(com.tblf.utils.FileUtils.isParent(directory));
    }

    @Test
    public void checkGetModules() throws IOException {
        String pomStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +"    <groupId>com.tblf</groupId>\n" +
                "    <artifactId>Analysis</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>jar</packaging>" +
                "    <modules>\n" +
                "        <module>module1</module>\n" +
                "        <module>module2</module>\n" +
                "    </modules>\n" +
                "</project>";

        org.apache.commons.io.FileUtils.write(pom, pomStr, "UTF-8");
        Assert.assertTrue(com.tblf.utils.FileUtils.isParent(directory));

        File module1 = new File(directory, "module1");
        Assert.assertTrue(module1.mkdir());

        File module2 = new File(directory, "module2");
        Assert.assertTrue(module2.mkdir());

        Assert.assertEquals(3, com.tblf.utils.FileUtils.getAllModules(directory).size());
    }

    @Test
    public void checkGetModulNone() throws IOException {
        String pomStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +"    <groupId>com.tblf</groupId>\n" +
                "    <artifactId>Analysis</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>jar</packaging>" +
                "</project>";

        org.apache.commons.io.FileUtils.write(pom, pomStr, "UTF-8");

        Assert.assertEquals(1, com.tblf.utils.FileUtils.getAllModules(directory).size());
    }
}
