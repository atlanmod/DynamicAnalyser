package com.tblf.monitors;

import com.tblf.reporters.AbstractReporter;
import com.tblf.reporters.ChronicleQueueReporter;

import java.io.File;

public abstract class AbstractMonitor extends Thread {

    protected AbstractReporter reporter;

    public AbstractMonitor(File reportDirectory) {
        this.reporter = new ChronicleQueueReporter(reportDirectory);
    }

    public abstract void startMonitor(int pid);

    public abstract void endMonitor(int pid);

    public AbstractReporter getReporter() {
        return reporter;
    }

    public void setReporter(AbstractReporter reporter) {
        this.reporter = reporter;
    }
}
