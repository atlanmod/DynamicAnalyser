package com.tblf.instrumentation;

import com.tblf.instrumentation.bytecode.ByteCodeInstrumenter;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;

import java.io.File;
import java.util.Collection;

/**
 * Builder creating an {@link Instrumenter}
 */
public class InstrumenterBuilder {

    private Instrumenter instrumenter;

    /**
     * @return the {@link Instrumenter}
     */
    public Instrumenter build() {
        return instrumenter;
    }

    /**
     * Instantiate a {@link SourceCodeInstrumenter}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withSourceCodeInstrumenter() {
        instrumenter = new SourceCodeInstrumenter();
        return this;
    }

    /**
     * Instantiate a {@link ByteCodeInstrumenter}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withByteCodeInstrumenter() {
        instrumenter = new ByteCodeInstrumenter();
        return this;
    }

    /**
     * Specify the directory to instrument
     * @param directory a {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder onDirectory(File directory) {
        instrumenter.setDirectory(directory);
        return this;
    }

    public InstrumenterBuilder withOutputDirectory(File outputDirectory) {
        instrumenter.setOutputDirectory(outputDirectory);
        return this;
    }

    /**
     * Specify a test folder
     * @param testFolder a {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder setTestDirectory(File testFolder) {
        instrumenter.setTestDirectory(testFolder);
        return this;
    }

    /**
     * Specify a SUT folder
     * @param sutFolder a {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder setSUTDirectory(File sutFolder) {
        instrumenter.setSutDirectory(sutFolder);
        return this;
    }

    /**
     * Specify external dependencies
     * @param dependencies a {@link Collection} of {@link File}
     * @return this, the current {@link InstrumenterBuilder}
     */
    public InstrumenterBuilder withDependencies(Collection<File> dependencies) {
        instrumenter.getDependencies().addAll(dependencies);
        return this;
    }
}
