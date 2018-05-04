package com.tblf.reporters;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import org.powerapi.PowerDisplay;
import org.powerapi.core.power.Power;
import org.powerapi.core.target.Target;
import scala.collection.immutable.Set;

import java.io.File;
import java.util.UUID;

public class ChronicleQueueReporter  extends AbstractReporter implements PowerDisplay {
    private final ExcerptAppender excerptAppender;

    public ChronicleQueueReporter(File file) {

        try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(file).build()) {
            excerptAppender = queue.acquireAppender();
        }

    }


    @Override
    public void report(String report) {
        try (DocumentContext dc = excerptAppender.writingDocument()) {
            dc.wire().write().text(report);
        }
    }

    @Override
    public void display(UUID muid, long timestamp, Set<Target> targets, Set<String> devices, Power power) {
        report(timestamp+" - "+power.toString());
    }
}
