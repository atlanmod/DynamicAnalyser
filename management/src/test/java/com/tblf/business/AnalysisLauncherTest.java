package com.tblf.business;

import com.tblf.instrumentation.InstrumentationType;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

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

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.run();

        ResourceSet resourceSet = analysisLauncher.getResourceSet();

        Resource analysisModel = resourceSet.getResource(URI.createURI(new File(file, "analysis.xmi").toURI().toString()), true);

        Assert.assertEquals(10, analysisModel.getContents().size());
        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkParseBCI() throws IOException {

        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.run();

        ResourceSet resourceSet = analysisLauncher.getResourceSet();

        Resource analysisModel = resourceSet.getResource(URI.createURI(new File(file, "analysis.xmi").toURI().toString()), true);

        Assert.assertEquals(10, analysisModel.getContents().size());
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
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));

        analysisLauncher.applyAfter(file1 -> {
            Assert.assertTrue(new File(file1, "analysis.xmi").exists());
        });

        analysisLauncher.run();

        FileUtils.deleteDirectory(file);
    }
}
