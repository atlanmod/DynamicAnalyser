package com.tblf.instrumentation;

import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class SourcecodeInstrumenterTest {

    @Test
    public void checkInstrumentMaven() throws IOException {
        File proj = new File("src/test/resources/sources/SimpleProject/");
        File binOut = new File(proj, "binOut");

        if (binOut.exists())
            FileUtils.deleteDirectory(binOut);

        Assert.assertTrue(proj.exists());
        SourceCodeInstrumenter instrumenter = new SourceCodeInstrumenter(proj);
        instrumenter.setBinDirectory(binOut);

        instrumenter.instrumentMavenProject(proj);

        //All classes have been compiled
        Assert.assertTrue(
                Files.walk((binOut.toPath())).filter(path -> path.toString().endsWith(".class")).collect(Collectors.toList()).size()
        >= Files.walk(proj.toPath()).filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList()).size());

        FileUtils.deleteDirectory(binOut);
    }

}
