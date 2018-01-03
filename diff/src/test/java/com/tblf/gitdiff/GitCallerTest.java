package com.tblf.gitdiff;

import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class GitCallerTest {

    @Test
    public void checkCompareCommit() throws IOException {
        File zip = new File("src/test/resources/files.zip");

        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/files");
        Assert.assertTrue(file.exists());
        GitCaller gitCaller = new GitCaller(file, new ResourceSetImpl());
        gitCaller.compareCommits("HEAD~1");

        FileUtils.deleteDirectory(file);

        //Does not expect any results. Method is tested in the manager pkg
    }

    @Test
    public void checkCompareCommitsRealProject() throws IOException {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        ModelUtils.unzip(new File("src/test/resources/fullprojects/SimpleProject.zip"));

        File file = new File("src/test/resources/fullprojects/SimpleProject");
        assert file.exists();

        ResourceSet resourceSet = ModelUtils.buildResourceSet(file);

        GitCaller gitCaller = new GitCaller(file, resourceSet);
        gitCaller.compareCommits("HEAD~1", "HEAD");

        Assert.assertEquals(1, gitCaller.getTestsToRun().size());

        FileUtils.deleteDirectory(file);
    }
}
