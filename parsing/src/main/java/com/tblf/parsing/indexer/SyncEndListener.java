package com.tblf.parsing.indexer;

import org.hawk.core.util.GraphChangeAdapter;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class SyncEndListener extends GraphChangeAdapter {
    private Callable r;
    private Semaphore sem;
    private Throwable ex;
    private Object returnValue;

    public SyncEndListener(Callable<?> r, Semaphore semaphore) {
        this.r = r;
        this.sem = semaphore;
    }

    @Override
    public void synchroniseEnd() {
        try {
            if (r != null) {
                returnValue = r.call();
            }
        } catch (Throwable e) {
            ex = e;
        }
        sem.release();
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Throwable getThrowable() {
        return ex;
    }

}
