package com.tblf.linker.tracers;

import com.tblf.utils.Configuration;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.WireKey;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class QueueTracer implements Tracer {
    private File file;
    private ExcerptAppender excerptAppender;

    public QueueTracer() {
        file = new File(Configuration.getProperty("traceFile"));
        if (!file.exists()) {
            file.mkdir();
        } else if (file.exists()) {
            if (file.isFile()) {
                file.delete();
                file.mkdir();
            }
        }
    }

    @Override
    public void startTrace() {
        if (file.exists())
            if (file.isDirectory())
                try {
                    org.apache.commons.io.FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            else
                file.delete();

        file.mkdir();

        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file.getAbsolutePath()).build();

        excerptAppender = queue.acquireAppender();
    }

    @Override
    synchronized public void write(String value) {
        excerptAppender.writeText(value);
    }

    @Override
    synchronized public void write(String topic, String value) {
        excerptAppender.writeMessage(() -> topic, value);
    }

    @Override
    public void updateTest(String className, String methodName) {
        excerptAppender.writeText("&:".concat(className).concat(":").concat(methodName));
    }

    @Override
    public void updateTarget(String className, String methodName) {
        excerptAppender.writeText("%:".concat(className).concat(":").concat(methodName));
    }

    @Override
    public void updateStatementsUsingColumn(String startPos, String endPos) {
        excerptAppender.writeText("!:".concat(startPos).concat(":").concat(endPos));
    }

    @Override
    public void updateStatementsUsingLine(String line) {
        excerptAppender.writeText("?:".concat(line));
    }

    @Override
    public void endTrace() {
        excerptAppender.queue().close();
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws Exception {
        endTrace();
    }
}
