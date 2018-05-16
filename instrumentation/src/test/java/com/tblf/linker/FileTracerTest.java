package com.tblf.linker;

import com.tblf.utils.Configuration;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

public class FileTracerTest {
    private Collection<File> traces;

    @Before
    public void setUp() {
        traces = new ArrayList<>();
    }

    @Test
    public void checkStartTrace() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkStartTrace");
        FileTracer tracer = new FileTracer();

        Assert.assertNotNull(tracer.getFile());
        File f = tracer.getFile();

        Assert.assertNotNull(f);
        Assert.assertTrue(f.exists());
    }

    @Test
    public void checkSetTestMethod() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkSetTestMethod");
        FileTracer tracer = new FileTracer();

        File file = tracer.getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        tracer.updateTest("com.pkg.MyTest", "myMethod");

        tracer.endTrace();

        String s = IOUtils.toString(file.toURI(), "UTF-8");
        Assert.assertEquals("&:com.pkg.MyTest:myMethod\n", s);
    }

    @Test
    public void checkSetTargetMethod() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkSetTargetMethod");
        FileTracer tracer = new FileTracer();

        File file = tracer.getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        tracer.updateTarget("com.pkg.MyTarget", "myMethod");

        tracer.endTrace();

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "%:com.pkg.MyTarget:myMethod\n";
        Assert.assertEquals(oracle, s);
    }

    @Test
    public void checkUpdateStatementUsingColumn() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkUpdateStatementUsingColumn");
        FileTracer tracer = new FileTracer();

        File file = tracer.getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        tracer.updateStatementsUsingColumn("50", "70");
        tracer.endTrace();

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "!:50:70\n";
        Assert.assertEquals(oracle, s);
    }


    @Test
    public void checkUpdateStatementUsingColumnTwice() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkUpdateStatementUsingColumnTwice");
        FileTracer tracer = new FileTracer();

        File file = tracer.getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        tracer.updateTest("Test", "testMethod");
        tracer.updateTarget("Target", "targetMethod");
        tracer.updateStatementsUsingColumn("50", "70");
        tracer.updateStatementsUsingColumn("150", "170");
        tracer.updateStatementsUsingColumn("50", "70");
        tracer.endTrace();

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "&:Test:testMethod\n" +
                "%:Target:targetMethod\n" +
                "!:50:70\n" +
                "!:150:170\n";

        Assert.assertEquals(oracle, s);
    }

    @Test
    public void checkUpdateStatementUsingLineNumber() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkUpdateStatementUsingLineNumber");
        FileTracer tracer = new FileTracer();

        File file = tracer.getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        tracer.updateTest("Test", "testMethod");
        tracer.updateTarget("Target", "targetMethod");
        tracer.updateStatementsUsingLine("7");
        tracer.updateStatementsUsingLine("8");
        tracer.updateStatementsUsingLine("7");
        tracer.endTrace();

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "&:Test:testMethod\n" +
                "%:Target:targetMethod\n" +
                "?:7\n" +
                "?:8\n";

        Assert.assertEquals(oracle, s);
    }

    @Test
    public void checkUpdateStatementUsingLineNumberTwice() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkUpdateStatementUsingLineNumberTwice");
        FileTracer tracer = new FileTracer();

        File file = tracer.getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        tracer.updateStatementsUsingLine("50");

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "?:50\n";
        Assert.assertEquals(oracle, s);
    }

    @Test
    public void checkEndTrace() throws IOException {
        Configuration.setProperty("traceFile", "src/test/resources/checkEndTrace");
        FileTracer tracer = new FileTracer();

        File file = tracer.getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        tracer.updateStatementsUsingLine("50");

        tracer.endTrace();

        Assert.assertEquals("?:50\n", IOUtils.toString(file.toURI(), "UTF-8"));
    }

    @After
    public void tearDown() {
        traces.forEach(file -> Assert.assertTrue(file.delete()));
    }
}