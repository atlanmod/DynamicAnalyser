package com.tblf.parsing.traceReaders;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ChronicleQueueBuilder;
import net.openhft.chronicle.queue.ExcerptTailer;

import java.io.File;

/**
 * Read a ChronicleQueue file
 */
public class TraceQueueReader extends TraceReader{
    private ExcerptTailer excerptTailer;

    public TraceQueueReader() {
    }

    /**
     * Constructor
     * @param file the persisted chronicle queue
     */
    public TraceQueueReader(File file) {
        setFile(file);
    }

    @Override
    public void setFile(File file) {
        super.setFile(file);
        ChronicleQueue queue = ChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        excerptTailer = queue.createTailer();
    }

    @Override
    public String read() {
        return excerptTailer.readText();
    }
}
