package com.tblf.instrumentation;

import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.Collectors;

public class SourceCodeInstrumenterTest {

    @Test
    public void checkInstrumentMaven() throws IOException {
        File proj = new File("src/test/resources/sources/SimpleProject/");
        File binOut = new File(proj, "binOut");

        if (binOut.exists())
            FileUtils.deleteDirectory(binOut);

        Assert.assertTrue(proj.exists());
        SourceCodeInstrumenter instrumenter = new SourceCodeInstrumenter(proj);
        instrumenter.setBinDirectory(binOut);

        instrumenter.instrument(Collections.singletonList("com.tblf.SimpleProject.App"), Collections.singletonList("com.tblf.SimpleProject.AppTest"));

        //All classes have been compiled
        Assert.assertTrue(
                Files.walk((binOut.toPath())).filter(path -> path.toString().endsWith(".class")).collect(Collectors.toList()).size()
        >= Files.walk(proj.toPath()).filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList()).size());

        try {
            Class clazz = SingleURLClassLoader.getInstance().getUrlClassLoader().loadClass("com.tblf.SimpleProject.AppTest");
            JUnitCore.runClasses(clazz);
        } catch (Throwable e) {
            Assert.fail(e.toString());
        }

        FileUtils.deleteDirectory(binOut);
    }

}
