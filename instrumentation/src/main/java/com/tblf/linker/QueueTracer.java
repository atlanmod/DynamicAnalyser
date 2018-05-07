package com.tblf.linker;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;

import java.io.File;
import java.io.IOException;

public class QueueTracer implements Tracer {
    private DocumentContext documentContext;
    private File file = new File("trace");

    @Override
    public void updateTest(String className, String methodName) {
        documentContext.wire().write("test").text("&:".concat(className).concat(":").concat(methodName));
    }

    @Override
    public void updateTarget(String className, String methodName) {
        documentContext.wire().write("target").text("%:".concat(className).concat(":").concat(methodName));
    }

    @Override
    public void updateStatementsUsingColumn(String startPos, String endPos) {
        documentContext.wire().write("statement").text("!:".concat(startPos).concat(":").concat(endPos));
    }

    @Override
    public void updateStatementsUsingLine(String line) {
        documentContext.wire().write("line").text("?:".concat(line));
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

        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build();
        final ExcerptAppender appender = queue.acquireAppender();
        documentContext = appender.writingDocument();
    }

    @Override
    public void endTrace() {
        documentContext.close();
    }

    public File getFile() {
        return file;
    }
}
