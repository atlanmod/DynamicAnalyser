package com.tblf.linker.tracers;

import com.tblf.utils.Configuration;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ConcurrentMap;

public class MapDBTracer implements Tracer {

    private DB db;
    private ConcurrentMap<String, HashSet<String>> map;
    private File dbFile;

    public MapDBTracer() {
        dbFile = new File(Configuration.getProperty("traceFile"));

        if (dbFile.exists())
            dbFile.delete();

        db = DBMaker.fileDB(dbFile).fileMmapEnable().make();
        map = (ConcurrentMap<String, HashSet<String>>) db.hashMap("map").createOrOpen();
    }

    @Override
    public void write(String value) {
        write("", value);
    }

    @Override
    public void write(String topic, String value) {
        HashSet<String> strings = map.getOrDefault(topic, new HashSet<>());
        strings.add(value);
        map.put(topic, strings);
    }

    @Override
    public void updateTest(String className, String methodName) {
        write("", "&:".concat(className).concat(":").concat(methodName));
    }

    @Override
    public void updateTarget(String className, String methodName) {
        write("", "%:".concat(className).concat(":").concat(methodName));
    }

    @Override
    public void updateStatementsUsingColumn(String startPos, String endPos) {
        write("", "!:".concat(startPos).concat(":").concat(endPos));
    }

    @Override
    public void updateStatementsUsingLine(String line) {
        write("", "?:".concat(line));
    }


    @Override
    public void startTrace() {
        if (db != null && !db.isClosed())
            db.close();

        if (dbFile.exists())
            dbFile.delete();

        db = DBMaker.fileDB(dbFile).fileMmapEnable().make();
        map = (ConcurrentMap<String, HashSet<String>>) db.hashMap("map").createOrOpen();

    }

    @Override
    public void endTrace() {
        db.close();
    }

    @Override
    public File getFile() {
        return dbFile;
    }

    @Override
    public void close() throws Exception {
        endTrace();
    }
}
