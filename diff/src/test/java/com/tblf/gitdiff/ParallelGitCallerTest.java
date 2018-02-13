package com.tblf.gitdiff;

import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ParallelGitCallerTest {

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

        //Set the uris of the Java2File elements in the model, in order to run this test on any machine
        //indeed the model uses absolute uri, and has been generated on a specific machine
        //since it is not generated in this test case, those uris remain the same
        resourceSet.getAllContents().forEachRemaining(notifier ->{
            if (notifier instanceof Java2File) {
                String path = ((Java2File) notifier).getJavaUnit().getOriginalFilePath();
                int index = path.indexOf(file.getName());
                ((Java2File) notifier).getJavaUnit().setOriginalFilePath(file.getAbsolutePath()+"/"+path.substring(index+file.getName().length()+1));
            }
        });

        GitCaller gitCaller = new ParallelGitCaller(file, resourceSet);
        gitCaller.compareCommits("HEAD~1", "HEAD");

        Assert.assertEquals(1, gitCaller.getNewTests().size());

        FileUtils.deleteDirectory(file);
    }
}
