package com.tblf.parsing.traceReaders;

import com.tblf.utils.Configuration;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentMap;

public class TraceMapReader extends TraceReader{
    private ConcurrentMap<String, HashSet<String>> map;

    public TraceMapReader() {

    }


    public TraceMapReader(File file) {
        setFile(file);
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        map = (ConcurrentMap<String, HashSet<String>>) DBMaker.fileDB(file).fileMmapEnable().checksumHeaderBypass().make().hashMap("map").createOrOpen();
    }


    @Override
    public String read() {
        return null;
    }

}
