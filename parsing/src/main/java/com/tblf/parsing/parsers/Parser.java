package com.tblf.parsing.parsers;

import com.tblf.parsing.parsingBehaviors.ParsingBehavior;
import com.tblf.parsing.traceReaders.TraceReader;

import java.io.File;
import java.util.logging.Logger;

public class Parser {

    protected static final Logger LOGGER = Logger.getLogger("Parser");

    protected File trace;

    protected TraceReader traceReader;
    protected ParsingBehavior parsingBehavior;

    /**
     * Constructor defining the parameters needed for the parsing
     * @param traceReader a {@link TraceReader} defining method to read a trace.
     * @param behavior a {@link ParsingBehavior} defining the behaviour to adapt when parsing the trace
     */
    public Parser(TraceReader traceReader, ParsingBehavior behavior) {
        this.parsingBehavior = behavior;
        this.traceReader = traceReader;
    }

    /**
     * Parse the traces given as parameters in the constructor
     *
     * @return the traces
     */
    public void parse() {

        String trace;

        while ((trace = traceReader.read()) != null) {
            parsingBehavior.manage(trace);
        }
    }

    public TraceReader getTraceReader() {
        return traceReader;
    }

    public void setTraceReader(TraceReader traceReader) {
        this.traceReader = traceReader;
    }

    public ParsingBehavior getParsingBehavior() {
        return parsingBehavior;
    }

    public void setParsingBehavior(ParsingBehavior parsingBehavior) {
        this.parsingBehavior = parsingBehavior;
    }
}
