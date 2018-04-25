package com.tblf;

import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.InstrumenterBuilder;
import com.tblf.processors.EnergyMethodProcessor;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;

public class EnergyMethodProcessorTest {

    @Test
    public void checkInstrumentationMethod() throws IOException {
        File file = new File("src/test/resources/energyMethodProcessorTestTargets");
        File outputDirectory = new File("src/test/resources/energyMethodProcessorTestTargets/output");

        if (! outputDirectory.exists())
            Assert.assertTrue(outputDirectory.mkdir());

        Assert.assertTrue(file.exists());

        Instrumenter instrumenter = new InstrumenterBuilder()
                .withSourceCodeInstrumenter()
                .onDirectory(file)
                .withOutputDirectory(outputDirectory)
                .build();

        instrumenter.instrument(Collections.singletonList(new EnergyMethodProcessor()));

        byte[] encoded = Files.readAllBytes(new File(outputDirectory, "MyClass.java").toPath());

        CtClass ctClass = Launcher.parseClass(new String(encoded, Charset.defaultCharset()));

        ctClass.getMethods().forEach(System.out::println);

        FileUtils.deleteDirectory(outputDirectory);
    }
}
