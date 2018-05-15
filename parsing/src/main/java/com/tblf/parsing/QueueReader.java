package com.tblf.parsing;

import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class QueueReader extends BufferedReader {
    private Wire wire;

    public QueueReader(File file) throws FileNotFoundException {
        super(new Reader(file) {
            @Override
            public int read(@NotNull char[] chars, int i, int i1) throws IOException {
                return 0;
            }

            @Override
            public void close() throws IOException {

            }
        });

        if (!file.exists())
            throw new FileNotFoundException("The file "+file.getAbsolutePath()+" does not exist");

        ExcerptAppender excerptAppender = SingleChronicleQueueBuilder.binary(file).build().acquireAppender();
        ExcerptTailer excerptTailer = excerptAppender.queue().createTailer().toStart();
        wire = excerptTailer.readingDocument().wire();
    }

    @Override
    public String readLine() throws IOException {
        return wire.read().text();
    }


}
