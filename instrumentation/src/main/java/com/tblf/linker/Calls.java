package com.tblf.linker;

import com.tblf.linker.tracers.FileTracer;
import com.tblf.linker.tracers.QueueTracer;
import com.tblf.linker.tracers.Tracer;
import com.tblf.utils.Configuration;

import java.util.logging.Logger;

/**
 * Is a static class. Those methods will be called by the instrumented code.
 * This class is a facade calling Tracer methods. This more complicated to directly use the Tracer in the instrumented code,
 * In fact, Strings are inserted, either in the ByteCode or SourceCode, and inserting "Calls.setTestMethod(....)"
 * for instance is simpler than adapting the instrumentation according to the Tracer.
 * And more reusable, as it is easy to change the Tracer currently used, than re-instrumenting the entire code in order to change it.
 */
public class Calls {
    private static Tracer tracer;
    private static final Logger LOGGER = Logger.getLogger("Calls");

    static {
        switch(Configuration.getProperty("trace")) {
            case "queue": {
                tracer = new QueueTracer();
                break;
            }
            case "file": {
                tracer = new FileTracer();
                break;
            }
            default: {
                tracer = new QueueTracer();
            }
        }

        tracer.startTrace();
    }

    /**
     * This private constructor forces the class to -not- be instanciated
     */
    private Calls() {
    }

    /**
     * Write the following String in the {@link Tracer}
     * @param value a {@link String}
     */
    public static void write(String value) {
        System.out.println("Writing: "+value);
        tracer.write(value);
    }

    /**
     * Set the test method currently being executed
     * @param className the name of the class being executed
     * @param method the name of the method being executed
     */
    public static void setTestMethod(String className, String method) {
        tracer.write("&:".concat(className).concat(":").concat(method));
    }

    /**
     * Set the SUT method currently being executed
     * @param className the name of the class being executed
     * @param method the name of the method being executed
     */
    public static void setTargetMethod(String className, String method) {
        tracer.write("%:".concat(className).concat(":").concat(method));
    }

    /**
     * trace the statement being executed using its position in the source code
     * @param className the name of the class
     * @param method the name of the method
     * @param startCol the start position of the statement, at the file level
     * @param endCol the end position of the statement, at the file level
     */
    public static void match(String className, String method, String startCol, String endCol) {
        tracer.write("%:".concat(className).concat(":").concat(method));
        tracer.write("!:".concat(startCol).concat(":").concat(endCol));
    }

    /**
     * trace the statement being executed using its line in the source code
     * @param className the name of the class
     * @param method the name of the method
     * @param line the line number
     */
    public static void match(String className, String method, String line) {
        tracer.write("%:".concat(className).concat(":").concat(method));
        tracer.write("?:".concat(line));
    }

    /**
     * Match the statement being executed using its position. the context will be used to re-trace the file being executed
     * @param startCol the start position at the file level
     * @param endCol the end position at the file level
     */
    public static void match(String startCol, String endCol) {
        tracer.write("!:".concat(startCol).concat(":").concat(endCol));
    }

    /**
     * Math the statement being executed using its line number
     * @param line the line number
     */
    public static void match(String line) {
        tracer.write("?:".concat(line));
    }

    public static Tracer getTracer() {
        return tracer;
    }

    public static void setTracer(Tracer tracer) {
        Calls.tracer = tracer;
    }

    /**
     * End the trace.
     */

    public static void end() {
        tracer.endTrace();
    }
}