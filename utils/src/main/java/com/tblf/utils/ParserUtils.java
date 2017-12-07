package com.tblf.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 02/10/2017.
 */
public class ParserUtils {

    private static final Logger LOGGER = Logger.getLogger("ParserUtils");

    /**
     * Parse the qualified name (QN) of a class and return the package it is contained in
     *
     * @param classQualifiedName the qualified name of the class
     * @return the package qualifiedName
     */
    public static String getPackageQNFromClassQN(String classQualifiedName) {
        String[] split = classQualifiedName.split("\\.");
        return classQualifiedName.replace(".".concat(split[split.length - 1]), "");
    }


    /**
     * Return the package QN from the class file
     * Assume that packages are located in the Java folder
     *
     * @param file a class such as "src/test/java/com/package/package2/MyClass.java"
     * @return a String representing the QN of the package : com.package.package2
     */
    public static String getPackageQNFromFile(File file) {
        String uri = file.getAbsolutePath();
        String javaStr = "java";

        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(uri.split("/")));
        arrayList.remove(arrayList.size() - 1); //removing the file name

        //  at this point: {src; test; java; com; package1; package2; package3}

        return String.join(".", arrayList.subList(arrayList.lastIndexOf(javaStr) + 1, arrayList.size()));
    }

    /**
     * Parse the URI of a SUT Class file and return the package QN of its container. Uses the {@link Configuration} file
     *
     * @param file a {@link File} such as "src/main/java/com/tblf/SimpleProject/App.java"
     * @return the package qualified name (e.g.) "com.tblf.SimpleProject"
     */
    public static String getPackageQNFromSUTFile(File file) {
        String sutUri = Configuration.getProperty("sut");
        String filePath = file.getPath();

        String[] path = filePath.split("/");
        String[] sut = sutUri.split("/");

        List<String> pathList = new LinkedList<>(Arrays.asList(path));

        pathList.remove(pathList.size() - 1);
        for (String repo : sut) {
            int item = pathList.indexOf(repo);
            if (item != -1) {
                pathList.remove(item);
            }
        }

        return pathList.stream().collect(Collectors.joining("."));
    }

    /**
     * Parse the URI of a test Class file and return the package QN of its container. Uses the {@link Configuration} file
     *
     * @param file a {@link File} such as "src/main/java/com/tblf/SimpleProject/App.java"
     * @return the package qualified name (e.g.) "com.tblf.SimpleProject"
     */
    public static String getPackageQNFromTestFile(File file) {
        String sutUri = Configuration.getProperty("test");
        String filePath = file.getPath();

        String[] path = filePath.split("/");
        String[] sut = sutUri.split("/");

        List<String> pathList = new LinkedList<>(Arrays.asList(path));

        pathList.remove(pathList.size() - 1);
        for (String repo : sut) {
            int item = pathList.indexOf(repo);
            if (item != -1) {
                pathList.remove(item);
            }
        }

        return pathList.stream().collect(Collectors.joining("."));
    }

    /**
     * Print the progression of the parsing
     *
     * @param total   the max progression
     * @param current the current progression
     */
    public static void printProgress(long startTime, long total, long current) {
        long eta = current == 0 ? 0 :
                (total - current) * (System.currentTimeMillis() - startTime) / current;

        String etaHms = current == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        StringBuilder string = new StringBuilder(140);
        int percent = (int) (current * 100 / total);

        string
                .append('\r')
                .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                .append(String.format(" %d%% [", percent))
                .append(String.join("", Collections.nCopies(percent, "=")))
                .append('>')
                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                .append(']')
                //  .append(String.join("", Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %d/%d, ETA: %s", current, total, etaHms));

        System.out.print(string);
    }


    /**
     * Print a nice progression
     *
     * @param maxLine the maximum progression
     */
    public static void endProgress(long maxLine) {
        StringBuilder stringBuilder = new StringBuilder(140);
        int percent = 100;
        stringBuilder
                .append('\r')
                .append(String.format(" %d%% [", percent))
                .append(String.join("", Collections.nCopies(percent, "=")))
                .append('>')
                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                .append(']')
                //  .append(String.join("", Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %d/%d, ETA: %s", maxLine, maxLine, "00:00:00"));

        System.out.println(stringBuilder);
    }

    /**
     * Get the number of line in a file
     *
     * @param file
     * @return a {@link Long} with the number of lines
     */
    public static long getLineNumber(File file) {
        long lineNumber = 0;
        try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file))) {
            lineNumberReader.skip(Long.MAX_VALUE);
            lineNumber = lineNumberReader.getLineNumber();
            lineNumberReader.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot get the line number in file " + file.getName(), e);
        }
        return lineNumber;
    }

}
