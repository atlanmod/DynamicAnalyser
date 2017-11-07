package com.tblf.gitdiff;

import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

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
}
