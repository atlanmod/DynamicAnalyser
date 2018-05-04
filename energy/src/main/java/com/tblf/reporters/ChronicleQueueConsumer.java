package com.tblf.reporters;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

import java.io.File;

public class ChronicleQueueConsumer {
    private ExcerptTailer tailer;
    private long lastIndex;

    public ChronicleQueueConsumer(File reportDirectory) {
        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(reportDirectory).build();
        tailer = queue.createTailer();
        lastIndex = tailer.toEnd().index();
        tailer = tailer.toStart();

    }

    public boolean hasNext() {
        return tailer.index() < lastIndex;
    }

    public String nextLine() {
        return tailer.readText();
    }
}
