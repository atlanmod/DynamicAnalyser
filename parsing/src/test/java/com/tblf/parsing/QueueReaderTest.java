package com.tblf.parsing;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ChronicleQueueBuilder;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.DocumentContext;
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

        ChronicleQueue queue = ChronicleQueueBuilder.single(f.getAbsolutePath()).build();
        final ExcerptAppender appender = queue.acquireAppender();

        appender.writeText("myText1");
        appender.writeText("myText2");
        appender.writeText("myText3");

        BufferedReader reader = new QueueReader(f);

        Assert.assertEquals("myText1", reader.readLine());
        Assert.assertEquals("myText2", reader.readLine());
        Assert.assertEquals("myText3", reader.readLine());
        Assert.assertTrue(reader.readLine() == null);
    }
}
