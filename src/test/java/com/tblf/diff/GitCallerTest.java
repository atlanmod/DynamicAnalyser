package com.tblf.diff;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
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
        GitCaller gitCaller = new GitCaller(file);
        gitCaller.compareCommits("HEAD~1");
    }
}
