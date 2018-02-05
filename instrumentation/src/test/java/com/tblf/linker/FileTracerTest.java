package com.tblf.linker;

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
        FileTracer.getInstance().startTrace();
        Assert.assertNotNull(((FileTracer) FileTracer.getInstance()).getFile());
        File f = ((FileTracer) FileTracer.getInstance()).getFile();

        Assert.assertNotNull(f);
        Assert.assertTrue(f.exists());
    }

    @Test
    public void checkSetTestMethod() throws IOException {
        FileTracer.getInstance().startTrace();

        File file = ((FileTracer) FileTracer.getInstance()).getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        FileTracer.getInstance().updateTest("com.pkg.MyTest", "myMethod");

        FileTracer.getInstance().endTrace();

        String s = IOUtils.toString(file.toURI(), "UTF-8");
        Assert.assertEquals("&:com.pkg.MyTest:myMethod\n", s);
    }

    @Test
    public void checkSetTargetMethod() throws IOException {
        FileTracer.getInstance().startTrace();

        File file = ((FileTracer) FileTracer.getInstance()).getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        FileTracer.getInstance().updateTarget("com.pkg.MyTarget", "myMethod");

        FileTracer.getInstance().endTrace();

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "%:com.pkg.MyTarget:myMethod\n";
        Assert.assertEquals(oracle, s);
    }

    @Test
    public void checkUpdateStatementUsingColumn() throws IOException {
        FileTracer.getInstance().startTrace();

        File file = ((FileTracer) FileTracer.getInstance()).getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        FileTracer.getInstance().updateStatementsUsingColumn("50", "70");
        FileTracer.getInstance().endTrace();

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "!:50:70\n";
        Assert.assertEquals(oracle, s);
    }

    @Test
    public void checkUpdateStatementUsingLineNumber() throws IOException {
        FileTracer.getInstance().startTrace();

        File file = ((FileTracer) FileTracer.getInstance()).getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        FileTracer.getInstance().updateStatementsUsingLine("50");
        FileTracer.getInstance().endTrace();

        String s = IOUtils.toString(file.toURI(), Charset.defaultCharset());
        String oracle = "?:50\n";
        Assert.assertEquals(oracle, s);
    }

    @Test
    public void checkEndTrace() throws IOException {
        FileTracer.getInstance().startTrace();
        File file = ((FileTracer) FileTracer.getInstance()).getFile();
        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        traces.add(file);

        FileTracer.getInstance().updateStatementsUsingLine("50");

        FileTracer.getInstance().endTrace();

        Assert.assertEquals("?:50\n", IOUtils.toString(file.toURI(), "UTF-8"));
    }

    @After
    public void tearDown() {
        traces.forEach(file -> Assert.assertTrue(file.delete()));
    }
}