package com.tblf.instrumentation;

import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.parsing.ModelParser;
import com.tblf.util.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 21/09/2017.
 */
@Ignore
public class ByteCodeInstrumenterTest {

    @Test
    public void checkInstrumentTarget() throws ClassNotFoundException, MalformedURLException {

        //Redirecting the standard output to an other outputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        PrintStream old = System.out;
        System.setOut(printStream);

        File folder = new File("src/test/resources/binaries/junit/bin");

        //Getting a TestClass
        ByteCodeInstrumenter byteCodeInstrumenter = new ByteCodeInstrumenter(folder);
        byteCodeInstrumenter.instrument(Arrays.asList("org.junit.internal.matchers.StacktracePrintingMatcherTest"), new ArrayList<>());

        Class aClass = SingleURLClassLoader.getInstance().getUrlClassLoader().loadClass("org.junit.internal.matchers.StacktracePrintingMatcherTest");
        Assert.assertNotNull(aClass);
        SingleURLClassLoader.getInstance().addURLs(new URL[]{folder.toURI().toURL()});

        //Running the testClass instrumented with the Target instrumentation
        JUnitCore.runClasses(aClass);

        //Saving the output to a String
        String output = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        //Reseting the standard output to the normal one
        System.out.flush();
        System.setOut(old);

        Assert.assertTrue(output.contains("executed"));
    }

    @Test
    public void checkInstrumentTest() throws ClassNotFoundException, MalformedURLException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        File folder = new File("src/test/resources/binaries/simpleProj");

        SingleURLClassLoader.getInstance().addURLs(new URL[]{folder.toURI().toURL()});

        //Getting a TestClass
        ByteCodeInstrumenter byteCodeInstrumenter = new ByteCodeInstrumenter(folder);
        byteCodeInstrumenter.instrument(Arrays.asList("com.tblf.SimpleProject.App"), Arrays.asList("com.tblf.SimpleProject.AppTest"));

        Class aClass = SingleURLClassLoader.getInstance().getUrlClassLoader().loadClass("com.tblf.SimpleProject.AppTest");
        Assert.assertNotNull(aClass);

        //Running the testClass instrumented with the Test instrumentation
        Result result = JUnitCore.runClasses(aClass);

        Assert.assertTrue(result.getFailures()
                        .stream()
                        .map(failure -> failure.getException().toString())
                        .collect(Collectors.toList()).toString(),
                result.wasSuccessful());
    }

    @Before
    public void setup( ) {

    }

    @Test
    public void checkInstrumentModel() throws Exception {
        Resource model = ModelUtils.loadModel(new File("src/test/resources/junit_java.xmi"));
        ModelParser modelParser = new ModelParser();
        modelParser.parse(model);

        Assert.assertFalse(modelParser.getTargets().isEmpty());
        Assert.assertFalse(modelParser.getTests().isEmpty());

        File junitFolder = new File("src/test/resources/binaries/junit/bin");

        //SingleURLClassLoader.getInstance().addURLs(new URL[]{junitFolder.toURI().toURL()});


        Field scl = ClassLoader.class.getDeclaredField("scl"); // Get system class loader
        scl.setAccessible(true); // Set accessible
        scl.set(null, SingleURLClassLoader.getInstance().getUrlClassLoader());

        ByteCodeInstrumenter byteCodeInstrumenter = new ByteCodeInstrumenter(junitFolder);

        try {
            byteCodeInstrumenter.instrument(modelParser.getTargets().keySet(),
                    modelParser.getTests().keySet());
        } catch (Throwable t) {
            Assert.fail(t.getMessage());
        }

    }

}
