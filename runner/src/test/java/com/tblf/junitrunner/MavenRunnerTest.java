package com.tblf.junitrunner;

import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class MavenRunnerTest {

    @Test
    public void testRun() throws IOException {

        ModelUtils.unzip(new File("src/test/resources/binaries/SimpleProject.zip"));
        File project = new File("src/test/resources/binaries/SimpleProject");

        System.setProperty("mode", "SOURCECODE");
        File pom = new File(project, "pom.xml");

        new MavenRunner(pom).run();

        FileUtils.deleteDirectory(project);
    }
}
