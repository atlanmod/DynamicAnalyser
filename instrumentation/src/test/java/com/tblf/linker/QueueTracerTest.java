package com.tblf.linker;

import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.Wire;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class QueueTracerTest {

    @Test
    public void testUpdateTest() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTest("myTest1", "myMethod1");
        tracer.updateTest("myTest2", "myMethod2");
        tracer.endTrace();
        File file = tracer.getFile();
        ExcerptAppender excerptAppender = SingleChronicleQueueBuilder.binary(file).build().acquireAppender();
        ExcerptTailer excerptTailer = excerptAppender.queue().createTailer().toStart();
        Wire wire = excerptTailer.readingDocument().wire();

        Assert.assertEquals("&:myTest:myMethod", wire.read().text());
        Assert.assertEquals("&:myTest1:myMethod1", wire.read().text());
        Assert.assertEquals("&:myTest2:myMethod2", wire.read().text());
    }

    @Test
    public void testUpdateTarget() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTarget("myTarget", "myTargetMethod");
        tracer.endTrace();
        File file = tracer.getFile();
        ExcerptAppender excerptAppender = SingleChronicleQueueBuilder.binary(file).build().acquireAppender();
        ExcerptTailer excerptTailer = excerptAppender.queue().createTailer().toStart();
        Wire wire = excerptTailer.readingDocument().wire();

        Assert.assertEquals("&:myTest:myMethod", wire.read().text());
        Assert.assertEquals("%:myTarget:myTargetMethod", wire.read().text());
    }

    @Test
    public void testUpdateStatementPos() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTarget("myTarget", "myTargetMethod");
        tracer.updateStatementsUsingColumn("50", "75");
        tracer.endTrace();
        File file = tracer.getFile();
        ExcerptAppender excerptAppender = SingleChronicleQueueBuilder.binary(file).build().acquireAppender();
        ExcerptTailer excerptTailer = excerptAppender.queue().createTailer().toStart();
        Wire wire = excerptTailer.readingDocument().wire();

        Assert.assertEquals("&:myTest:myMethod", wire.read().text());
        Assert.assertEquals("%:myTarget:myTargetMethod", wire.read().text());
        Assert.assertEquals("!:50:75", wire.read().text());
    }

    @Test
    public void testUpdateStatementLine() {
        QueueTracer tracer = new QueueTracer();
        tracer.startTrace();
        tracer.updateTest("myTest", "myMethod");
        tracer.updateTarget("myTarget", "myTargetMethod");
        tracer.updateStatementsUsingLine("50");
        tracer.endTrace();
        File file = tracer.getFile();
        ExcerptAppender excerptAppender = SingleChronicleQueueBuilder.binary(file).build().acquireAppender();
        ExcerptTailer excerptTailer = excerptAppender.queue().createTailer().toStart();
        Wire wire = excerptTailer.readingDocument().wire();

        Assert.assertEquals("&:myTest:myMethod", wire.read().text());
        Assert.assertEquals("%:myTarget:myTargetMethod", wire.read().text());
        Assert.assertEquals("?:50", wire.read().text());
    }

}
