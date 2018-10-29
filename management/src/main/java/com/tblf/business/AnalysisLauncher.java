package com.tblf.business;

import com.tblf.instrumentation.InstrumentationType;
import com.tblf.instrumentation.InstrumenterBuilder;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.parsing.TraceType;
import com.tblf.parsing.parsers.Parser;
import com.tblf.parsing.parsingBehaviors.ParsingBehavior;
import com.tblf.parsing.traceReaders.TraceFileReader;
import com.tblf.parsing.traceReaders.TraceMqttReader;
import com.tblf.parsing.traceReaders.TraceQueueReader;
import com.tblf.parsing.traceReaders.TraceReader;
import com.tblf.utils.Configuration;
import com.tblf.utils.FileUtils;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AnalysisLauncher {
    private static final Logger LOGGER = Logger.getLogger(AnalysisLauncher.class.getName());

    private File root;
    private List<? extends File> sources;
    private ResourceSet resourceSet;
    private File outputModel;
    private InstrumenterBuilder instrumenterBuilder;

    private List<Consumer<File>> before;
    private List<Consumer<File>> after;

    private List<Object> processors;
    private List<File> dependencies;

    private boolean isPomAtRoot = true;

    private TraceReader traceReader;
    private ParsingBehavior behavior;

    /**
     * Constructor getting all the analysable module inside the {@link File} directory
     *
     * @param source a directory as a {@link File}
     */
    public AnalysisLauncher(File source) {
        root = source;
        before = new ArrayList<>();
        after = new ArrayList<>();
        processors = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

    /**
     * Set the instrumentation mode
     *
     * @param instrumentationType an {@link InstrumentationType} can be either sourcecode or bytecode
     */
    public void setInstrumentationType(InstrumentationType instrumentationType) {
        Configuration.setProperty("mode", instrumentationType.toString());
    }

    public void setTraceType(TraceType traceType) {
        Configuration.setProperty("trace", traceType.toString());
    }

    /**
     * Register a custom processor for the instrumentation
     *
     * @param processor a processor. Can be a source code processor such as a @{@link spoon.processing.Processor}
     *                  or a bytecode visitor such as {@link org.objectweb.asm.commons.AdviceAdapter}
     */
    public void registerProcessor(Object processor) {
        processors.add(processor);
    }

    public void registerDependencies(Collection<File> deps) {
        dependencies.addAll(deps);
    }

    /**
     * Register a behavior for the analysis. The {@link ParsingBehavior} will be called for each trace,
     * and will define how the analyzer is supposed to act according to the execution trace.
     * @param parsingBehavior a {@link ParsingBehavior}
     */
    public void registerBehavior(ParsingBehavior parsingBehavior) { behavior = parsingBehavior;}
    
    public void setOutputModel(File file) {
        outputModel = file;
    }

    /**
     * Instrument the source code using the given processors
     */
    private void instrument() {
        sources.forEach(source -> {

            ModelUtils.buildResourceSet(source);

            instrumenterBuilder
                    .onDirectory(source)
                    .withQueueExecutionTrace()
                    .build()
                    .instrument(processors);

            LOGGER.info("Running the tests to create execution traces");
            new MavenRunner(new File(source, "pom.xml")).withPomDependencies(dependencies).run();

        });
    }

    /**
     * Set the dependencies before running the impact analysis
     */
    private void setUpInstrumentation() {
        instrumenterBuilder = new InstrumenterBuilder();

        if (isPomAtRoot)
            sources = FileUtils.getAllModules(root).stream().filter(FileUtils::isAnalysable).collect(Collectors.toList());
        else
            sources = FileUtils.searchForPoms(root).stream().filter(FileUtils::isAnalysable).collect(Collectors.toList());

        switch (InstrumentationType.valueOf(Configuration.getProperty("mode"))) {
            case BYTECODE:
                instrumenterBuilder = instrumenterBuilder.withByteCodeInstrumenter();
                break;
            case SOURCECODE:
                instrumenterBuilder = instrumenterBuilder.withSourceCodeInstrumenter();
                break;
            default:
                LOGGER.warning("No instrumentation chosen");
        }

        switch (TraceType.valueOf(Configuration.getProperty("trace"))) {
            case QUEUE:
                instrumenterBuilder = instrumenterBuilder.withQueueExecutionTrace();
                traceReader = new TraceQueueReader();
                break;
            case FILE:
                instrumenterBuilder = instrumenterBuilder.withSingleFileExecutionTrace();
                traceReader = new TraceFileReader();
                break;
            case MQTT:

                break;
            default:
                LOGGER.warning("No instrumentation chosen");
        }

        if (outputModel == null)
            outputModel = new File(root, Configuration.getProperty("outputModel") + "." + Configuration.getProperty("outputFormat"));

        instrumenterBuilder = instrumenterBuilder.withOutputDirectory(new File(root, Configuration.getProperty("instrumentedSources")));
    }

    /**
     * Depending on the context, some external methods could be executed on the sources before running the impact analysis
     *
     * @param method a {@link Consumer} of {@link File}
     */
    public void applyBefore(Consumer<File> method) {
        before.add(method);
    }

    /**
     * Depending on the context, some external methods could be executed on the sources after running the impact analysis
     *
     * @param method a {@link Consumer} of {@link File}
     */
    public void applyAfter(Consumer<File> method) {
        after.add(method);
    }


    public void run() {
        setUpInstrumentation();

        sources.forEach(source -> before.forEach(fileConsumer -> fileConsumer.accept(source)));

        instrument();

        parse();

        sources.forEach(source -> after.forEach(fileConsumer -> fileConsumer.accept(source)));
    }

    /**
     * Parse the execution trace, using the right {@link Parser} on the right execution trace
     */
    private void parse() {
        switch(TraceType.valueOf(Configuration.getProperty("trace"))) {
            case FILE:
                traceReader = new TraceFileReader();
                break;
            case MQTT:
                traceReader = new TraceMqttReader();
                break;
            case QUEUE:
                traceReader = new TraceQueueReader();
                break;
            default:
                return;
        }

        File trace = new File(root, Configuration.getProperty("traceFile"));

        traceReader.setFile(trace);

        if (behavior == null)
            throw new RuntimeException("No parsing behavior specified. Cannot run the analysis.");

        new Parser(traceReader, behavior).parse();
    }

    public void setIsPomAtRoot(boolean bool) {
        isPomAtRoot = bool;
    }

    public List<? extends File> getSources() {
        return sources;
    }

    public void setSources(List<? extends File> sources) {
        this.sources = sources;
    }

    public ResourceSet getResourceSet() {
        return resourceSet;
    }

    public void setResourceSet(ResourceSet resourceSet) {
        this.resourceSet = resourceSet;
    }

    public ParsingBehavior getBehavior() {
        return behavior;
    }
}
