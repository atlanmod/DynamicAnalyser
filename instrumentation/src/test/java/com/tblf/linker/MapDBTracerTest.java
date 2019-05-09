package com.tblf.linker;

import com.tblf.linker.tracers.MapDBTracer;
import com.tblf.utils.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ConcurrentMap;

public class MapDBTracerTest {

    private ConcurrentMap<String, HashSet<String>> concurrentMap;
    private DB db;
    private static final String dbPath = "src/test/resources/mapDbTest.db";
    private File dbFile = new File(dbPath);

    @Before
    public void setUp() {
        dbFile.delete();
        //fileDB(dbFile).fileMmapEnable()
        db = DBMaker.fileDB(dbFile).fileMmapEnable().make();
        concurrentMap = (ConcurrentMap<String, HashSet<String>>) db.hashMap("map").create();
    }

    @Test
    public void checkMapSimplePut() {
        HashSet<String> strings = concurrentMap.getOrDefault("key", new HashSet<>());
        strings.add("value");
        concurrentMap.put("key", strings);
        Assert.assertTrue(concurrentMap.get("key").contains("value"));
    }

    @Test
    public void checkMapMultiplePut() {
        put("key", "value1");
        put("key", "value2");
        Assert.assertTrue(concurrentMap.get("key").contains("value1") && concurrentMap.get("key").contains("value2"));
    }

    @Test
    public void checkMapMultiplePutSerializationAndLoading() {
        put("key", "value1");
        put("key", "value2");
        put("key2", "value1");
        put("key2", "value2");
        Assert.assertTrue(concurrentMap.get("key2").contains("value1") && concurrentMap.get("key2").contains("value2"));
        db.close();
        db = DBMaker.fileDB(dbFile).fileMmapEnable().make();
        concurrentMap = (ConcurrentMap<String, HashSet<String>>) db.hashMap("map").createOrOpen();
        Assert.assertTrue(concurrentMap.get("key2").contains("value1") && concurrentMap.get("key2").contains("value2"));
    }

    @Test
    public void checkPerformanceWriting() {
        long before = System.currentTimeMillis();
        for (int i = 0; i < 100000; ++i) {
            put("key"+i, "value"+i);
        }

        for (int i = 100000; i > 0; --i) {
            put("key"+i, "value"+i);
        }
        System.out.println(System.currentTimeMillis() - before);
        before = System.currentTimeMillis();

        for (int i = 0; i < 100000; ++i) {
            Assert.assertTrue(concurrentMap.get("key"+i).contains("value"+i));
        }

        for (int i = 100000; i > 0; --i) {
            Assert.assertTrue(concurrentMap.get("key"+i).contains("value"+i));
        }

        System.out.println(System.currentTimeMillis() - before);
    }

    @Test
    public void checkCreate() {
        Configuration.setProperty("traceFile", dbPath);
        MapDBTracer mapDBTracer = new MapDBTracer();
        mapDBTracer.startTrace();
        mapDBTracer.write("key", "value");
        mapDBTracer.write("key", "value2");
        mapDBTracer.endTrace();

        Assert.assertTrue(dbFile.exists());
        DB db = DBMaker.fileDB(dbFile).fileMmapEnable().make();
        ConcurrentMap<String, HashSet<String>> concurrentMap = (ConcurrentMap<String, HashSet<String>>) db.hashMap("map").createOrOpen();

        Assert.assertTrue(concurrentMap.get("key").contains("value"));
        Assert.assertTrue(concurrentMap.get("key").contains("value2"));

        db.close();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private void put(String key, String value) {
        HashSet<String> strings = concurrentMap.getOrDefault("key", new HashSet<>());
        strings.add(value);
        concurrentMap.put(key, strings);
    }
}
