package com.tblf.parsing.parsers;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class QueueReader extends BufferedReader {
    private ExcerptTailer excerptTailer;

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

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();
        excerptTailer = queue.createTailer();
    }

    @Override
    public String readLine() throws IOException {
        return excerptTailer.readText();
    }


}
