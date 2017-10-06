package com.tblf.instrumentation;

import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class SourcecodeInstrumenterTest {

    @Test
    public void checkInstrument() {
        Collection<String> targets = new HashSet<>();
        targets.add("com.tblf.SimpleProject.App");

        Collection<String> tests = new HashSet<>();
        tests.add("com.tblf.SimpleProject.AppTest");

        File srcFolder = new File("src/test/resources/sources/simpleProject/src/main/java");
        File testFolder = new File("src/test/resources/sources/simpleProject/src/test/java");
        File srcOutFolder = new File("src/test/resources/sources/simpleProject/output/src");
        File binOutFolder = new File("src/test/resources/sources/simpleProject/output/bin");

        if (srcOutFolder.exists()) {
            srcOutFolder.delete();
        }

        if (binOutFolder.exists()) {
            binOutFolder.delete();
        }

        Assert.assertTrue(srcFolder.exists() && testFolder.exists());

        SourceCodeInstrumenter instrumenter = new SourceCodeInstrumenter(srcFolder, testFolder, new ArrayList<>());
        instrumenter.setBinDirectory(binOutFolder);
        instrumenter.setSrcDirectory(srcOutFolder);

        instrumenter.instrument(targets, tests);

    }
}
