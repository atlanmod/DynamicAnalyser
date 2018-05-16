package com.tblf.linker;

import com.tblf.utils.Configuration;
import com.tblf.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trace the execution of an application and store it into a file
 * Is a singleton. Indeed, this class would be called by the instrumented code, and instantiating it at the beginning of each call would be really costly
 */
public class FileTracer implements Tracer {

    private static final Logger LOGGER = Logger.getLogger("FileTracer");

    private String currentTarget;
    private String currentTest;
    private String currentTestMethod;
    private String currentTargetMethod;
    private Writer writer;
    private File file;
    private StringBuilder stringBuilder = new StringBuilder();

    private Map<String, Collection<Integer>> lineWritten; //store a collection of written lines , in order to limit the size of the trace
    private Map<String, Collection<AbstractMap.SimpleEntry<Integer, Integer>>> statementWritten; //store a collection of written statement, in order to limit the size of the trace

    /**
     * Private constructor. This class must not be instanciated by the client
     */
    public FileTracer() {
        try {

            //this.file = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".extr");
            this.file = new File(Configuration.getProperty("traceFile"));

            if (this.file.exists())
                FileUtils.clearFile(this.file);

            this.writer = new FlushingBufferedWriter(new FileWriter(this.file));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't reset the trace file", e);
        }

        this.lineWritten = new HashMap<>();
        this.statementWritten = new HashMap<>();
        LOGGER.info("Now writing execution trace in: " + this.file.getAbsolutePath());

    }

    @Override
    public void startTrace() {

    }


    /**
     * Return the file being lineWritten
     *
     * @return
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Write in the file the current test being executed and set the field
     *
     * @param className the name of the test class
     * @param method    the name of the sut method
     */
    @Override
    public void updateTest(String className, String method) {
        if (!className.equals(this.currentTest) || !method.equals(this.currentTestMethod)) {
            this.lineWritten.clear();
            this.statementWritten.clear();

            this.currentTest = className;
            this.currentTestMethod = method;
            this.currentTarget = null;
            this.currentTargetMethod = null;
            this.stringBuilder = new StringBuilder();
            this.stringBuilder.append("&:").append(className).append(":").append(method).append("\n");
            try {
                this.writer.write(this.stringBuilder.toString());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cannot update the test " + className, e);
            }
        }

    }

    /**
     * Write in the file the current target being executed and set the field
     *
     * @param className the name of the sut class
     * @param method    the name of the sut method
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
                LOGGER.warning("Couldn't write the current target " + className + " - " + method + " in trace" + Arrays.toString(e.getStackTrace()));
            }
        }

    }

    /**
     * Write in the file the current statement being executed
     *
     * @param startCol the starting position of the column, at the file level
     * @param endCol   the ending position of the column, at the file level
     */
    @Override
    public void updateStatementsUsingColumn(String startCol, String endCol) {
        this.statementWritten.computeIfAbsent(this.currentTarget, (k) -> new HashSet<>());

        //Check if the current statement has already been covered, so the test impact is not computed multiple times for this specific method
        if (!this.statementWritten.get(this.currentTarget)
                .contains(new AbstractMap.SimpleEntry<>(Integer.valueOf(startCol), Integer.valueOf(endCol)))) {

            this.stringBuilder = new StringBuilder();
            this.stringBuilder.append("!:").append(startCol).append(":").append(endCol).append("\n");

            try {
                this.writer.write(this.stringBuilder.toString());
                this.statementWritten.get(this.currentTarget).add(new AbstractMap.SimpleEntry<>(Integer.valueOf(startCol), Integer.valueOf(endCol)));
            } catch (IOException e) {
                LOGGER.warning("Couldn't write the current statement " + startCol + ":" + endCol + " in the file" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    /**
     * Write in the file the current statement being executed
     *
     * @param line the line number at the line level
     */
    @Override
    public void updateStatementsUsingLine(String line) {
        this.lineWritten.computeIfAbsent(this.currentTarget, (k) -> new HashSet<>());

        if (!this.lineWritten.get(this.currentTarget).contains(Integer.valueOf(line))) {
            this.stringBuilder = new StringBuilder();
            this.stringBuilder.append("?:").append(line).append("\n");

            try {
                this.writer.write(this.stringBuilder.toString());
                this.lineWritten.get(this.currentTarget).add(Integer.valueOf(line));
            } catch (IOException e) {
                LOGGER.warning("Couldn't write the current line " + line + " in the file" + Arrays.toString(e.getStackTrace()));
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
            LOGGER.info("trace file saved in " + this.file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.warning("Couldn't write in the file" + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void close() throws Exception {
        endTrace();
    }
}
