package com.tblf.instrumentation;

import com.tblf.classLoading.InstURLClassLoader;
import com.tblf.classLoading.SingleURLClassLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Thibault on 21/09/2017.
 */
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
    public void checkInstrumentTest() throws ClassNotFoundException, MalformedURLException {
        //Redirecting the standard output to an other outputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        PrintStream old = System.out;
        System.setOut(printStream);

        File folder = new File("src/test/resources/binaries/junit/bin");

        //Getting a TestClass
        ByteCodeInstrumenter byteCodeInstrumenter = new ByteCodeInstrumenter(folder);
        byteCodeInstrumenter.instrument(new ArrayList<>(), Arrays.asList("org.junit.internal.matchers.StacktracePrintingMatcherTest"));

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

        System.out.println(output);
        Assert.assertTrue(output.contains("method"));
    }

}
