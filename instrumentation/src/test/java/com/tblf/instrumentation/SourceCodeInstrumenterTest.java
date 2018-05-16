package com.tblf.instrumentation;

import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import com.tblf.linker.FileTracer;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.Collectors;

public class SourceCodeInstrumenterTest {

    @Before
    public void setup() throws IOException {
        ModelUtils.unzip(new File("src/test/resources/sources/SimpleProject.zip"));
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File("src/test/resources/sources/SimpleProject"));
    }

    @Test
    public void checkInstrumentMaven() throws IOException {


        File proj = new File("src/test/resources/sources/SimpleProject/");
        File binOut = new File(proj, "binOut");

        if (binOut.exists())
            FileUtils.deleteDirectory(binOut);

        Assert.assertTrue(proj.exists());
        SourceCodeInstrumenter instrumenter = new SourceCodeInstrumenter();
        instrumenter.setDirectory(proj);
        instrumenter.setBinDirectory(binOut);

        instrumenter.instrument(Collections.singletonList("com.tblf.SimpleProject.App"), Collections.singletonList("com.tblf.SimpleProject.AppTest"));

        //All classes have been compiled
        Assert.assertTrue(
                Files.walk((binOut.toPath())).filter(path -> path.toString().endsWith(".class")).collect(Collectors.toList()).size()
        >= Files.walk(proj.toPath()).filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList()).size());

        try {
            Class clazz = SingleURLClassLoader.getInstance().getClassLoader().loadClass("com.tblf.SimpleProject.AppTest");
            JUnitCore.runClasses(clazz);
        } catch (Throwable e) {
            Assert.fail(e.toString());
        }

        FileUtils.deleteDirectory(binOut);
    }

}
