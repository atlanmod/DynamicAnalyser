package com.tblf.instrumentation;

import com.tblf.DotCP.DotCPParserBuilder;
import com.tblf.Link.Calls;
import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.instrumentation.bytecode.ByteCodeInstrumenter;
import com.tblf.parsing.ModelParser;
import com.tblf.runner.RunnerUtils;
import com.tblf.util.Configuration;
import com.tblf.util.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 21/09/2017.
 * Test the BCI on multiple classes and projects
 * By default the projects are stored as zip. This lighten a lot the size of the app, and do not mess with the classloading step of the mvn surefire plugin
 */

public class ByteCodeInstrumenterTest {

    @Before
    public void setup() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.INFO);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.INFO);
        }
    }

    @Test
    public void checkInstrumentTarget() throws ClassNotFoundException, IOException {
        File file = new File("src/test/resources/binaries/junit.zip");
        ModelUtils.unzip(file);

        File folder = new File("src/test/resources/binaries/junit");
        Configuration.setProperty("sutBinaries", "/bin");
        Configuration.setProperty("testBinaries", "/bin");

        //Getting a TestClass
        ByteCodeInstrumenter byteCodeInstrumenter = new ByteCodeInstrumenter(folder);
        byteCodeInstrumenter.instrument(Collections.singletonList("org.junit.internal.matchers.StacktracePrintingMatcherTest"), new ArrayList<>());

        Class aClass = SingleURLClassLoader.getInstance().getUrlClassLoader().loadClass("org.junit.internal.matchers.StacktracePrintingMatcherTest");
        Assert.assertNotNull(aClass);

        File callDependency = new File("libs/Link-1.0.0.jar");
        Assert.assertTrue(callDependency.exists());

        SingleURLClassLoader.getInstance().addURLs(new URL[]{folder.toURI().toURL(), callDependency.toURI().toURL()});

        //Running the testClass instrumented with the Target instrumentation
        System.out.println(RunnerUtils.results(JUnitCore.runClasses(aClass)));

        FileUtils.deleteDirectory(folder);
    }

    @Test
    public void checkInstrumentTest() throws ClassNotFoundException, IOException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Configuration.setProperty("sutBinaries", "/");
        Configuration.setProperty("testBinaries", "/");
        File zip = new File("src/test/resources/binaries/simpleProj.zip");
        ModelUtils.unzip(zip);

        File folder = new File("src/test/resources/binaries/simpleProj");

        File callDependency = new File("libs/Link-1.0.0.jar");
        Assert.assertTrue(callDependency.exists());

        SingleURLClassLoader.getInstance().addURLs(new URL[]{folder.toURI().toURL(), callDependency.toURI().toURL()});

        //Getting a TestClass
        ByteCodeInstrumenter byteCodeInstrumenter = new ByteCodeInstrumenter(folder);
        byteCodeInstrumenter.instrument(Collections.singletonList("com.tblf.SimpleProject.App"), Collections.singletonList("com.tblf.SimpleProject.AppTest"));

        Class aClass = SingleURLClassLoader.getInstance().getUrlClassLoader().loadClass("com.tblf.SimpleProject.AppTest");
        Assert.assertNotNull(aClass);

        //Running the testClass instrumented with the Test instrumentation
        Result result = JUnitCore.runClasses(aClass);

        Assert.assertTrue(result.getFailures()
                        .stream()
                        .map(failure -> failure.getException().toString())
                        .collect(Collectors.toList()).toString(),
                result.wasSuccessful());

        Calls.end();

        FileUtils.deleteDirectory(folder);
    }

    @Test
    public void checkInstrumentModel() throws Exception {

        File assertJZip = new File("src/test/resources/binaries/assertj.zip");
        ModelUtils.unzip(assertJZip);

        Resource model = ModelUtils.loadModelFromZip(new File("src/test/resources/binaries/assertj/assertj-core_java.zip"));
        ModelParser modelParser = new ModelParser();
        modelParser.parse(model);

        Assert.assertFalse(modelParser.getTargets().isEmpty());
        Assert.assertFalse(modelParser.getTests().isEmpty());

        File assertJFolder = new File("src/test/resources/binaries/assertj");
        Configuration.setProperty("sutBinaries", "/");
        Configuration.setProperty("testBinaries", "/");

        File DotCP = new File("src/test/resources/binaries/assertj/.classpath");
        Assert.assertTrue(assertJFolder.exists() && DotCP.exists());

        List<File> jars = new DotCPParserBuilder().create().parse(DotCP);
        Assert.assertFalse(jars.isEmpty());

        jars.add(assertJFolder);

        URL[] urls = jars.stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList()).toArray(new URL[0]);

        SingleURLClassLoader.getInstance().addURLs(urls);

        ByteCodeInstrumenter byteCodeInstrumenter = new ByteCodeInstrumenter(assertJFolder);

        try {
            byteCodeInstrumenter.instrument(modelParser.getTargets().keySet(),
                    modelParser.getTests().keySet());
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

        Assert.assertNotNull(SingleURLClassLoader.getInstance().getUrlClassLoader().loadClass("org.assertj.core.api.AbstractMapSizeAssert"));
        Assert.assertNotNull(SingleURLClassLoader.getInstance().getUrlClassLoader().loadClass("org.assertj.core.api.date.AbstractDateAssertWithOneIntArg_Test"));

        FileUtils.deleteDirectory(assertJFolder);
    }

}
