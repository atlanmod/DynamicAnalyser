package com.tblf.linker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

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
