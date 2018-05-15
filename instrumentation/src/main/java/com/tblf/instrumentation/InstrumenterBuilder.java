package com.tblf.instrumentation;

import com.tblf.instrumentation.bytecode.ByteCodeInstrumenter;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import com.tblf.linker.Calls;
import com.tblf.linker.FileTracer;
import com.tblf.linker.QueueTracer;
import com.tblf.linker.Tracer;
import com.tblf.utils.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builder creating an {@link Instrumenter}
 */
public class InstrumenterBuilder {

    private InstrumentationType instrumentationType;

    private Class tracer;

    private File onDirectory;
    private File outputDirectory;
    private Collection<File> dependencies = new ArrayList<>();
    private File testDirectory;
    private File sutDirectory;

    /**
     * @return the {@link Instrumenter}
     */
    public Instrumenter build() {

        Instrumenter instrumenter = instrumentationType.equals(InstrumentationType.BYTECODE) ?
                new ByteCodeInstrumenter() :
                new SourceCodeInstrumenter();

        instrumenter.setDirectory(onDirectory);
        instrumenter.setOutputDirectory(outputDirectory);
        instrumenter.setTestDirectory(testDirectory);
        instrumenter.setSutDirectory(sutDirectory);
        instrumenter.getDependencies().addAll(dependencies);

        return instrumenter;
    }

    /**
     * Instantiate a {@link SourceCodeInstrumenter}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withSourceCodeInstrumenter() {
        instrumentationType = InstrumentationType.SOURCECODE;
        return this;
    }

    /**
     * Instantiate a {@link ByteCodeInstrumenter}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withByteCodeInstrumenter() {
        instrumentationType = InstrumentationType.BYTECODE;
        return this;
    }

    /**
     * Instantiate a @{@link com.tblf.linker.FileTracer} to trace the execution
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withSingleFileExecutionTrace() {
        tracer = FileTracer.class;
        return this;
    }

    /**
     * Instantiate a {@link com.tblf.linker.QueueTracer} to trace the execution. Faster and safer
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withQueueExecutionTrace() {
        tracer = QueueTracer.class;
        return this;
    }

    /**
     * Specify the directory to instrument
     * @param directory a {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder onDirectory(File directory) {
        onDirectory = directory;
        return this;
    }

    /**
     * Specify the output directory for the instrumented files
     * @param outputDirectory a {@link File} directory
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    /**
     * Specify a test folder
     * @param testFolder a {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder setTestDirectory(File testFolder) {
        testDirectory = testFolder;
        return this;
    }

    /**
     * Specify a SUT folder
     * @param sutFolder a {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder setSUTDirectory(File sutFolder) {
        sutDirectory = sutFolder;
        return this;
    }

    /**
     * Specify external dependencies
     * @param dependencies a {@link Collection} of {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withDependencies(Collection<File> dependencies) {

        this.dependencies = dependencies;
        return this;
    }
}
