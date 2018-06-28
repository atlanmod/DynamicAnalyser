package com.tblf.linker;

import com.tblf.linker.tracers.QueueTracer;
import com.tblf.linker.tracers.Tracer;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ChronicleQueueBuilder;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class QueueTracerTest {

    private boolean asserted = false;

    @Test
    public void testWriteWithTopic() {
        Tracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.write("test", "value");
        tracer.endTrace();

        File file = tracer.getFile();

        ChronicleQueue queue = ChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();

        Assert.assertEquals("value", excerptTailer.readingDocument().wire().read("test").text());
    }

    @Test
    public void testWrite() {
        Tracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.write("value");

        File file = tracer.getFile();

        ChronicleQueue queue = ChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();

        Assert.assertEquals("value", excerptTailer.readText());
    }

    @Test
    public void testUpdateTest() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTest("myTest1", "myMethod1");
        tracer.updateTest("myTest2", "myMethod2");
        //tracer.endTrace();

        File file = tracer.getFile();

        ChronicleQueue queue = ChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();

        Assert.assertEquals("&:myTest:myMethod", excerptTailer.readText());
        Assert.assertEquals("&:myTest1:myMethod1", excerptTailer.readText());
        Assert.assertEquals("&:myTest2:myMethod2", excerptTailer.readText());
    }

    @Test
    public void testUpdateTarget() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTarget("myTarget", "myTargetMethod");

        File file = tracer.getFile();

        ChronicleQueue queue = ChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();

        Assert.assertEquals("&:myTest:myMethod", excerptTailer.readText());
        Assert.assertEquals("%:myTarget:myTargetMethod", excerptTailer.readText());
    }

    @Test
    public void testUpdateStatementPos() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTarget("myTarget", "myTargetMethod");
        tracer.updateStatementsUsingColumn("50", "75");

        File file = tracer.getFile();

        ChronicleQueue queue = ChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();

        Assert.assertEquals("&:myTest:myMethod", excerptTailer.readText());
        Assert.assertEquals("%:myTarget:myTargetMethod", excerptTailer.readText());
        Assert.assertEquals("!:50:75", excerptTailer.readText());
    }

    @Test
    public void testUpdateStatementLine() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTarget("myTarget", "myTargetMethod");
        tracer.updateStatementsUsingLine("50");

        File file = tracer.getFile();

        ChronicleQueue queue = ChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();

        Assert.assertEquals("&:myTest:myMethod", excerptTailer.readText());
        Assert.assertEquals("%:myTarget:myTargetMethod", excerptTailer.readText());
        Assert.assertEquals("?:50", excerptTailer.readText());
    }


}
