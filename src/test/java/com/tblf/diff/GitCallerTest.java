package com.tblf.diff;

import com.tblf.business.Manager;
import com.tblf.util.Configuration;
import com.tblf.util.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class GitCallerTest {

    @Before
    public void setUp() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }
    }

    @Test
    public void checkCompareCommit() {
        File file = new File("src/test/resources/files");
        Assert.assertTrue(file.exists());
        GitCaller gitCaller = new GitCaller(file, new ResourceSetImpl());
        gitCaller.compareCommits("HEAD~1");
    }

    @Test
    public void checkCompareCommitStandardProject() throws IOException {
        File zip = new File("src/test/resources/fullProject/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        File file = new File("src/test/resources/fullProject/SimpleProject");
        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(file);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);

            Resource analysis = manager.parseTraces(trace);

            Assert.assertNotNull(analysis);
            analysis.getResourceSet().getResources().forEach(resource -> System.out.println(resource.getURI()));

            GitCaller gitCaller = new GitCaller(file, analysis.getResourceSet());
            gitCaller.compareCommits("HEAD~1");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(file);
    }
}
