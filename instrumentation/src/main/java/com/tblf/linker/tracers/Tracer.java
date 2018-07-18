package com.tblf.linker.tracers;

import java.io.File;

/**
 * Abstract class defining the class generating the execution traces when being called
 */
public interface Tracer extends AutoCloseable{

    /**
     * Write a raw value in the tracer for future postprocessing
     * @param value a {@link String}
     */
    void write(String value);

    /**
     * Write a value along with a topic in the tracer. May be use for more accurate traces.
     * e.g.: write("energy", "50uJ")
     * @param topic a {@link String}
     * @param value a {@link String}
     */
    void write(String topic, String value);

    /**
     * Update the current test being executed
     * @param className the name of the test class
     * @param methodName the name of the method
     */
    void updateTest(String className, String methodName);

    /**
     * Update the target being executed
     * @param className the name of the sut class
     * @param methodName the name of the sut method
     */
    void updateTarget(String className, String methodName);

    /**
     * Update the statement being executed using its column position
     * @param startPos the starting position of the statement
     * @param endPos the ending position of the statement
     */
    void updateStatementsUsingColumn(String startPos, String endPos);

    /**
     * Update the statement being executed using its line number
     * Every statement on the same line would be traced the same way, which is less accurate
     * @param line the line number
     */
    void updateStatementsUsingLine(String line);

    /**
     * Start the trace, create the files and reset the attributes
     */
    void startTrace();

    /**
     * terminate the trace, save the logs etc ...
     */
    void endTrace();

    /**
     * Return the trace {@link File}
     * @return a {@link File}
     *
     */
    File getFile();
}

