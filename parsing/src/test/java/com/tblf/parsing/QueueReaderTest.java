package com.tblf.parsing;

import com.tblf.parsing.parsers.QueueReader;
import com.tblf.parsing.traceReaders.TraceQueueReader;
import com.tblf.parsing.traceReaders.TraceReader;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

public class QueueReaderTest {

    @Before
    public void setUp() {

    }

    @Test
    public void checkReadLine() throws IOException {
        File f = new File("src/test/resources/QueueReaderTest/trace");
        if (f.exists())
            FileUtils.deleteDirectory(f);

        f.mkdirs();

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(f.getAbsolutePath()).build();
        final ExcerptAppender appender = queue.acquireAppender();

        appender.writeText("myText1");
        appender.writeText("myText2");
        appender.writeText("myText3");

        TraceReader reader = new TraceQueueReader(f);

        Assert.assertEquals("myText1", reader.read());
        Assert.assertEquals("myText2", reader.read());
        Assert.assertEquals("myText3", reader.read());
        Assert.assertTrue(reader.read() == null);
    }

    @Test
    public void checkReadLineWithTopic() throws IOException {
        File f = new File("src/test/resources/QueueReaderTest/trace");
        if (f.exists())
            FileUtils.deleteDirectory(f);

        f.mkdirs();

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(f.getAbsolutePath()).build();
        final ExcerptAppender appender = queue.acquireAppender();

        appender.writeDocument(wire -> wire.write("topic").text("myText1"));
        appender.writeDocument(wire -> wire.write("topic").text("myText2"));
        appender.writeDocument(wire -> wire.write("topic").text("myText3"));

        TraceReader reader = new TraceQueueReader(f);

        Assert.assertEquals("myText1", reader.read());
        Assert.assertEquals("myText2", reader.read());
        Assert.assertEquals("myText3", reader.read());
        Assert.assertTrue(reader.read() == null);
    }
}
