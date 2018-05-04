package com.tblf;

import com.tblf.monitors.BitWattsMonitor;
import com.tblf.reporters.ChronicleQueueConsumer;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class BitWattsMonitorTest {

    @Test
    public void checkConstructor() throws InterruptedException, IOException {
        int pid = 2394;
        File reportDirectory = new File("src/test/resources/BitWattsMonitorTest/report");

        if (reportDirectory.exists())
            FileUtils.deleteDirectory(reportDirectory);

        reportDirectory.mkdir();

        BitWattsMonitor bitWattsMonitor = new BitWattsMonitor(reportDirectory);
        bitWattsMonitor.startMonitor(pid);

        Thread.sleep(5000l);

        bitWattsMonitor.endMonitor(pid);

        ChronicleQueueConsumer chronicleQueueConsumer = new ChronicleQueueConsumer(reportDirectory);
        while(chronicleQueueConsumer.hasNext())
            System.out.println(chronicleQueueConsumer.nextLine());
    }
}
