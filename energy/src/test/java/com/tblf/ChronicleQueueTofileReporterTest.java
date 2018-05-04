package com.tblf;

import com.tblf.reporters.AbstractReporter;
import com.tblf.reporters.ChronicleQueueReporter;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.MethodReader;
import net.openhft.chronicle.wire.WireParselet;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ChronicleQueueTofileReporterTest {

    @Test
    public void checkReport() throws IOException {
        File file = new File("src/test/resources/report");
        if (file.exists())
            FileUtils.deleteDirectory(file);

        file.mkdir();

        AbstractReporter abstractReporter = new ChronicleQueueReporter(file);
        abstractReporter.report("this is a test method");

        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build();
        final ExcerptTailer tailer = queue.createTailer();

        final AtomicInteger atomicInteger = new AtomicInteger(0);

        MethodReader methodReader = tailer.methodReader((WireParselet<String>) (charSequence, valueIn, o) -> {
            Assert.assertEquals("this is a test method", valueIn.text());
            atomicInteger.incrementAndGet();
        });

        Assert.assertEquals(0, atomicInteger.get());
        methodReader.readOne();
        Assert.assertEquals(1, atomicInteger.get());
    }
}
