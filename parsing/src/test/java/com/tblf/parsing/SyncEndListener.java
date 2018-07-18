package com.tblf.parsing;

import org.hawk.core.util.GraphChangeAdapter;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class SyncEndListener extends GraphChangeAdapter {
    private Callable r;
    private Semaphore sem;
    private Throwable ex;

    public SyncEndListener(Callable<?> r, Semaphore semaphore) {
        this.r = r;
        this.sem = semaphore;
    }

    @Override
    public void synchroniseEnd() {
        try {
            if (r != null) {
                r.call();
            }
        } catch (Throwable e) {
            ex = e;
        }
        sem.release();
    }

    public Throwable getThrowable() {
        return ex;
    }

}
