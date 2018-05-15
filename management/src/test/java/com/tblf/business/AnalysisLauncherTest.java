package com.tblf.business;

import com.tblf.instrumentation.InstrumentationType;
import com.tblf.linker.Calls;
import com.tblf.linker.FileTracer;
import com.tblf.utils.Configuration;
import com.tblf.utils.MavenUtils;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class AnalysisLauncherTest {

    /*
     SimpleProject execution output:
     Constructor1 --> TestApp
     line constructor1 --> TestApp
     method --> TestApp
     line 1 method --> TestApp
     line 2 method --> TestApp
     Constructor2 --> TestApp2
     line constructor2 --> TestApp2
     method --> TestApp2
     line 1 method --> TestApp2
     line 2 method --> TestApp2
     */

    @Before
    public void setUp() throws IOException {
        File file = new File("src/test/resources/fullprojects/SimpleProject");

        if (file.exists())
            FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkParseSCI() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        Configuration.setProperty("trace", "file");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.run();

        File model = new File(file, "analysis.xmi");
        Assert.assertTrue(model.exists());

        Resource analysisModel = ModelUtils.loadModel(model);

        Assert.assertEquals(10, analysisModel.getContents().size());
        FileUtils.deleteDirectory(file);
    }

    @Ignore
    @Test
    public void checkParseSCIQueue() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        Configuration.setProperty("trace", "queue");
        Configuration.setProperty("type", String.valueOf(InstrumentationType.SOURCECODE));

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.run();

        File model = new File(file, "analysis.xmi");
        Assert.assertTrue(model.exists());

        Resource analysisModel = ModelUtils.loadModel(model);

        Assert.assertEquals(10, analysisModel.getContents().size());
        FileUtils.deleteDirectory(file);
    }
    @Test
    public void checkParseBCI() throws IOException {

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        System.setProperty("maven.home", Configuration.getProperty("MAVEN_HOME"));

        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        Configuration.setProperty("trace", "file");
        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));

        analysisLauncher.applyBefore(MavenUtils::compilePom);
        analysisLauncher.run();

        File model = new File(file, "analysis.xmi");
        Assert.assertTrue(model.exists());

        Resource analysisModel = ModelUtils.loadModel(model);

        Assert.assertEquals("Not the expected number of impacts", 10, analysisModel.getContents().size());
        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkBefore() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));

        analysisLauncher.applyBefore(file1 -> {
            Assert.assertFalse(new File(file1, "analysis.xmi").exists());
        });

        analysisLauncher.applyBefore(file1 -> {
            File file2 = new File(file1, "aRandomFileName.txt");
            try {
                Assert.assertTrue(file2.createNewFile());
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });

        analysisLauncher.applyBefore(file1 -> {
            Assert.assertTrue(new File(file1, "aRandomFileName.txt").exists());
        });

        analysisLauncher.run();

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkAfter() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));

        analysisLauncher.applyAfter(file1 -> {
            Assert.assertTrue(new File(file1, "analysis.xmi").exists());
        });

        analysisLauncher.run();

        FileUtils.deleteDirectory(file);
    }
}
