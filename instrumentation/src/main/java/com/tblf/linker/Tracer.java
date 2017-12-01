package com.tblf.linker;

import java.util.logging.Logger;

/**
 * Abstract class defining the class generating the execution traces when being called
 */
public abstract class Tracer {

    protected static final Logger LOGGER = Logger.getLogger("Tracer");

    /**
     * Update the current test being executed
     * @param className the name of the test class
     * @param methodName the name of the method
     */
    abstract void updateTest(String className, String methodName);

    /**
     * Update the target being executed
     * @param className the name of the sut class
     * @param methodName the name of the sut method
     */
    abstract void updateTarget(String className, String methodName);

    /**
     * Update the statement being executed using its column position
     * @param startPos the starting position of the statement
     * @param endPos the ending position of the statement
     */
    abstract void updateStatementsUsingColumn(String startPos, String endPos);

    /**
     * Update the statement being executed using its line number
     * Every statement on the same line would be traced the same way, which is less accurate
     * @param line the line number
     */
    abstract void updateStatementsUsingLine(String line);

    /**
     * Start the trace, create the files and reset the attributes
     */
    public abstract void startTrace();

    /**
     * terminate the trace, save the logs etc ...
     */
    public abstract void endTrace();

}

