package com.tblf.linker;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trace the execution of an application and store it into a file
 * Is a singleton. Indeed, this class would be called by the instrumented code, and instantiating it at the beginning of each call would be really costly
 */
public class FileTracer extends Tracer {

    private static FileTracer INSTANCE;
    private static final Logger LOGGER = Logger.getLogger("FileTracer");
    private String currentTarget;
    private String currentTest;
    private String currentTestMethod;
    private String currentTargetMethod;
    private Writer writer;
    private File file;
    private StringBuilder stringBuilder = new StringBuilder();
    private Map<String, Collection<Integer>> written;

    /**
     * Private constructor. This class must not be instanciated by the client
     */
    private FileTracer() {
        this.reset();
    }

    /**
     * Singleton getInstance method
     * @return the current instance of {@link Tracer}
     */
    public static Tracer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FileTracer();
            LOGGER.info("New FileTracer created");
        }

        return INSTANCE;
    }

    @Override
    public void startTrace() {
        reset();
    }

    /**
     * Reset the {@link FileTracer}. creates a new trace and reset all the current tests and targets being executed
     */
    private void reset() {
        try {

            this.file = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".extr");
            this.writer = Files.newBufferedWriter(this.file.toPath());
            this.currentTarget = null;
            this.currentTest = null;
            this.currentTestMethod = null;
            this.currentTargetMethod = null;
            this.written = new HashMap<>();
            LOGGER.info("Reseted the output file of the current Tracer. Now writing in: "+this.file.getAbsolutePath());
        } catch (IOException var2) {
            LOGGER.warning("Couldn't write in the file" + Arrays.toString(var2.getStackTrace()));
        }

    }

    /**
     * Return the file being written
     * @return
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Write in the file the current test being executed and set the field
     * @param className the name of the test class
     * @param method the name of the sut method
     */
    @Override
    public void updateTest(String className, String method) {
        if (!className.equals(this.currentTest) || !method.equals(this.currentTestMethod)) {
            this.written.clear();
            this.currentTest = className;
            this.currentTestMethod = method;
            this.currentTarget = null;
            this.currentTargetMethod = null;
            this.stringBuilder = new StringBuilder();
            this.stringBuilder.append("&:").append(className).append(":").append(method).append("\n");

            try {
                this.writer.write(this.stringBuilder.toString());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cannot update the test "+className, e);
            }
        }

    }

    /**
     * Write in the file the current target being executed and set the field
     * @param className the name of the sut class
     * @param method the name of the sut method
     */
    @Override
    public void updateTarget(String className, String method) {
        if (!className.equals(this.currentTarget) || !method.equals(this.currentTargetMethod)) {
            this.currentTarget = className;
            this.currentTargetMethod = method;
            this.stringBuilder = new StringBuilder();
            this.stringBuilder.append("%:").append(className).append(":").append(method).append("\n");

            try {
                this.writer.write(this.stringBuilder.toString());
            } catch (IOException e) {
                LOGGER.warning("Couldn't write in the file" + Arrays.toString(e.getStackTrace()));
            }
        }

    }

    /**
     * Write in the file the current statement being executed
     * @param startCol the starting position of the column, at the file level
     * @param endCol the ending position of the column, at the file level
     */
    @Override
    public void updateStatementsUsingColumn(String startCol, String endCol) {
        this.stringBuilder = new StringBuilder();
        this.stringBuilder.append("!:").append(startCol).append(":").append(endCol).append("\n");

        try {
            this.writer.write(this.stringBuilder.toString());
        } catch (IOException e) {
            LOGGER.warning("Couldn't write in the file" + Arrays.toString(e.getStackTrace()));
        }

    }

    /**
     * Write in the file the current statement being executed
     * @param line the line number at the line level
     */
    @Override
    public void updateStatementsUsingLine(String line) {
        this.written.computeIfAbsent(this.currentTarget, (k) -> new HashSet<>());

        if (!this.written.get(this.currentTarget).contains(Integer.valueOf(line))) {
            this.stringBuilder = new StringBuilder();
            this.stringBuilder.append("?:").append(line).append("\n");

            try {
                this.writer.write(this.stringBuilder.toString());
                this.written.get(this.currentTarget).add(Integer.valueOf(line));
            } catch (IOException e) {
                LOGGER.warning("Couldn't write in the file" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    /**
     * End the trace by closing the writer
     */
    @Override
    public void endTrace() {
        try {
            this.writer.close();
        } catch (IOException e) {
            LOGGER.warning("Couldn't write in the file" + Arrays.toString(e.getStackTrace()));
        }

    }
}
