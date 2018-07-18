package com.tblf.parsing.traceReaders;

import java.io.File;

public abstract class TraceReader {

    private File file;

    public abstract String read();

    public TraceReader() {

    }

    public TraceReader(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
