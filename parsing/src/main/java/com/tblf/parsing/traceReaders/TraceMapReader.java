package com.tblf.parsing.traceReaders;

import com.tblf.utils.Configuration;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ConcurrentMap;

public class TraceMapReader extends TraceReader{
    private ConcurrentMap<String, HashSet<String>> map;

    public TraceMapReader() {
        this(new File(Configuration.getProperty("traceFile")));

    }

    public TraceMapReader(File file) {
        map = (ConcurrentMap<String, HashSet<String>>) DBMaker.fileDB(file).fileMmapEnable().make().hashMap("map").createOrOpen();
    }

    @Override
    public String read() {
        return null;
    }

}
