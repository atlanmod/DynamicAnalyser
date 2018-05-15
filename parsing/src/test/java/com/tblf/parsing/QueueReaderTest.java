package com.tblf.parsing;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
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

        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(f).build();
        final ExcerptAppender appender = queue.acquireAppender();
        DocumentContext documentContext = appender.writingDocument();

        documentContext.wire().write().text("myText1");
        documentContext.wire().write().text("myText2");
        documentContext.wire().write().text("myText3");

        documentContext.close();

        BufferedReader reader = new QueueReader(f);

        Assert.assertEquals("myText1", reader.readLine());
        Assert.assertEquals("myText2", reader.readLine());
        Assert.assertEquals("myText3", reader.readLine());
        Assert.assertTrue(reader.readLine() == null);
    }
}
