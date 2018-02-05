package com.tblf.linker;

import java.io.*;

public class FlushingBufferedWriter extends BufferedWriter {

    public FlushingBufferedWriter(Writer writer) {
        super(writer);
    }

    @Override
    public void write(String s) throws IOException {
        super.write(s);
        super.flush();
    }
}
