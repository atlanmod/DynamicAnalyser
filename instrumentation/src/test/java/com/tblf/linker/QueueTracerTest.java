package com.tblf.linker;

import com.tblf.linker.tracers.QueueTracer;
import com.tblf.linker.tracers.Tracer;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class QueueTracerTest {

    @Test
    public void testWriteWithTopic() {
        Tracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.write("test", "value");
        tracer.write("test", "value2");
        tracer.write("test", "value3");
        tracer.endTrace();

        File file = tracer.getFile();

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();
        Assert.assertEquals("value", excerptTailer.readingDocument().wire().read("test").text());
        Assert.assertEquals("value2", excerptTailer.readingDocument().wire().read("test").text());
        Assert.assertEquals("value3", excerptTailer.readingDocument().wire().read("test").text());
    }

    @Test
    public void testWrite() {
        Tracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.write("value");

        File file = tracer.getFile();

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();
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

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();
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

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();
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

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();
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

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        ExcerptTailer excerptTailer = queue.createTailer();

        Assert.assertEquals("&:myTest:myMethod", excerptTailer.readText());
        Assert.assertEquals("%:myTarget:myTargetMethod", excerptTailer.readText());
        Assert.assertEquals("?:50", excerptTailer.readText());
    }


}
