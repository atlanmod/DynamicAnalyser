package com.tblf.business;

import com.tblf.linker.tracers.FileTracer;
import com.tblf.linker.tracers.QueueTracer;
import com.tblf.linker.tracers.Tracer;
import com.tblf.parsing.parsers.QueueReader;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Benchmark {

    @Test
    @Ignore
    public void comparingFileAndQueue() throws IOException {
        /* File trace */

        long timeBefore = System.currentTimeMillis();
        Configuration.setProperty("trace", "file");
        Configuration.setProperty("traceFile", "src/test/resources/tracefile.txt");

        Tracer tracer = new FileTracer();

        for (int i = 0; i < 10000000; ++i) {
            tracer.updateTest("test", "testmethod");
            tracer.updateTarget("target", "targetmethod");
            tracer.updateStatementsUsingLine(String.valueOf(i));
        }

        File trace = tracer.getFile();

        System.out.println("Writing lasted: "+String.valueOf(System.currentTimeMillis() - timeBefore)+" ms");

        timeBefore = System.currentTimeMillis();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(trace));
        String line;

        while((line = bufferedReader.readLine()) != null) {
            //Empty
        }

        System.out.println("Reading lasted: "+String.valueOf(System.currentTimeMillis() - timeBefore)+" ms");

        assert trace.delete();

        /* Queue trace */

        long timeBefore2 = System.currentTimeMillis();

        Configuration.setProperty("trace", "queue");
        Configuration.setProperty("traceFile", "src/test/resources/trace");

        Tracer tracer2 = new QueueTracer();

        for (int i = 0; i < 10000000; ++i) {
            tracer.updateTest("test", "testmethod");
            tracer.updateTarget("target", "targetmethod");
            tracer.updateStatementsUsingLine(String.valueOf(i));
        }

        File trace2 = tracer2.getFile();

        System.out.println("Writing Lasted: "+String.valueOf(System.currentTimeMillis() - timeBefore2)+" ms");

        timeBefore2 = System.currentTimeMillis();

        BufferedReader bufferedReader2 = new QueueReader(trace2);
        String line2;

        while((line2 = bufferedReader2.readLine()) != null) {
            //Empty
        }

        System.out.println("Reading lasted: "+String.valueOf(System.currentTimeMillis() - timeBefore)+" ms");

        FileUtils.deleteDirectory(trace2);
    }
}
