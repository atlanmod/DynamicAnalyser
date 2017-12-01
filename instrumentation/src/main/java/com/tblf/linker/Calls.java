package com.tblf.linker;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Is a static class. Those methods will be called by the instrumented code
 */
public class Calls {
    private static Tracer tracer = FileTracer.getInstance();
    private static final Logger LOGGER = Logger.getLogger("Calls");

    /**
     * This private constructor forces the class to -not- be instanciated
     */
    private Calls() {
    }

    /**
     * Set the test method currently being executed
     * @param className the name of the class being executed
     * @param method the name of the method being executed
     */
    public static void setTestMethod(String className, String method) {
        try {
            tracer.updateTest(className, method);
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

    }

    /**
     * Set the SUT method currently being executed
     * @param className the name of the class being executed
     * @param method the name of the method being executed
     */
    public static void setTargetMethod(String className, String method) {
        try {
            tracer.updateTarget(className, method);
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

    }

    /**
     * trace the statement being executed using its position in the source code
     * @param className the name of the class
     * @param method the name of the method
     * @param startCol the start position of the statement, at the file level
     * @param endCol the end position of the statement, at the file level
     */
    public static void match(String className, String method, String startCol, String endCol) {
        tracer.updateTarget(className, method);
        tracer.updateStatementsUsingColumn(startCol, endCol);
    }

    /**
     * trace the statement beinf executed using its line in the source code
     * @param className the name of the class
     * @param method the name of the method
     * @param line the line number
     */
    public static void match(String className, String method, String line) {
        tracer.updateTarget(className, method);
        tracer.updateStatementsUsingLine(line);
    }

    /**
     * Match the statement being executed using its position. the context will be used to re-trace the file being executed
     * @param startCol the start position at the file level
     * @param endCol the end position at the file level
     */
    public static void match(String startCol, String endCol) {
        tracer.updateStatementsUsingColumn(startCol, endCol);
    }

    /**
     * Math the statement being executed using its line number
     * @param line the line number
     */
    public static void match(String line) {
        try {
            tracer.updateStatementsUsingLine(line);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Cannot trace the line "+line, t);
        }

    }

    /**
     * End the trace.
     */
    public static void end() {
        tracer.endTrace();
    }
}