package com.tblf.discovery;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TestDiscoverer {

    private File project;

    @Before
    public void setUp() throws IOException {
        File before = new File("src/test/resources/SimpleProject");

        if (before.exists()) {
            FileUtils.deleteDirectory(before);
        }

        File f = new File("src/test/resources/SimpleProject.zip");
        unzip(f);

        project = new File("src/test/resources/SimpleProject");
        Files.walk(project.toPath()).filter(path -> path.toString().endsWith(".xmi")).forEach(path -> path.toFile().delete());
    }

    @Test
    public void check() {
        Assert.assertNotNull(project);

        try {
            Discoverer.generateFullModel(project);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertTrue(Arrays.stream(project.listFiles()).filter(file -> file.getName().endsWith(".xmi")).count() > 0);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(project);
    }

    /**
     * @param zip
     * @return
     * @throws IOException
     */
    private List<File> unzip(File zip) throws IOException {
        BufferedOutputStream bufferedOutputStream;
        FileInputStream fileInputStream = new FileInputStream(zip);
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(fileInputStream));
        ZipEntry zipEntry;

        List<File> filesUnzipped = new ArrayList<>();

        final int BUFFER = 2048;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            File file = FileUtils.getFile(zip.getParentFile(), zipEntry.toString());

            if (zipEntry.toString().endsWith("/")) {
                file.mkdir();
            } else {
                filesUnzipped.add(file);

                int count;
                byte data[] = new byte[BUFFER];

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream, BUFFER);

                while ((count = zipInputStream.read(data, 0, BUFFER)) != -1) {
                    bufferedOutputStream.write(data,0, count);
                }

                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            }

        }
        zipInputStream.close();

        return filesUnzipped;
    }
}
