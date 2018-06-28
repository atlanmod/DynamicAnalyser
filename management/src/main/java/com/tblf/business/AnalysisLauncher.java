package com.tblf.business;

import com.tblf.instrumentation.InstrumentationType;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.InstrumenterBuilder;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.linker.Calls;
import com.tblf.parsing.TraceType;
import com.tblf.parsing.parsers.ModelParser;
import com.tblf.parsing.parsers.Parser;
import com.tblf.parsing.parsers.TraceParser;
import com.tblf.utils.Configuration;
import com.tblf.utils.FileUtils;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AnalysisLauncher {
    public static int oracle;

    private static final Logger LOGGER = Logger.getLogger(AnalysisLauncher.class.getName());

    private File root;
    private List<? extends File> sources;
    private ResourceSet resourceSet;
    private File outputModel;
    private Instrumenter instrumenter;
    private InstrumenterBuilder instrumenterBuilder;

    private List<Consumer<File>> before;
    private List<Consumer<File>> after;

    private List<Object> processors;
    private List<File> dependencies;

    private boolean isPomAtRoot = true;

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

    public void setOutputModel(File file) {
        outputModel = file;
    }

    /**
     * Execute the impact analysis:
     * Get the SUT & Tests in the moDisco model
     * Instrument the code
     * Execute the test
     * Parse the execution trace to produce the impact analysis model
     */
    private void instrumentAndRunTests() {
        sources.forEach(source -> {

            before.forEach(fileConsumer -> fileConsumer.accept(source));

            LOGGER.info("Computing the impact analysis of " + source.getName());
            this.resourceSet = ModelUtils.buildResourceSet(source);
            instrumenterBuilder = instrumenterBuilder.onDirectory(source);

            try {
                Resource javaModel = this.resourceSet.getResources()
                        .stream()
                        .filter(resource -> resource.getURI().toString().endsWith("_java.xmi")).
                                findFirst()
                        .orElseThrow(() -> new IOException("Could not find the MoDisco java model"));

                LOGGER.info("Analysis the model: " + javaModel.getURI().toFileString());
                ModelParser modelParser = new ModelParser();
                modelParser.parse(javaModel); //Get the tests and sut classes

                LOGGER.log(Level.INFO, modelParser.getTargets().size() + " SUT classes and " + modelParser.getTests().size() + " test classes");

                LOGGER.info("Instrumenting the code in: " + source.getName());

                instrumenter = instrumenterBuilder.build();

                instrumenter.getDependencies().add(new File(Calls.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
                instrumenter.getDependencies().add(new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI()));

                instrumenter.instrument(modelParser.getTargets().keySet(), modelParser.getTests().keySet());

                LOGGER.info("Running the tests to create execution traces");
                new MavenRunner(new File(source, "pom.xml")).run();

            } catch (IOException | URISyntaxException e) {
                LOGGER.log(Level.WARNING, "An error was caught during the impact analysis", e);
            }

            after.forEach(fileConsumer -> fileConsumer.accept(source));
        });
    }

    /**
     * Parses the execution traces generated for impact analysis purposes
     */
    private void parseTestTrace() {
        sources.forEach(source -> {
            File exTrace = new File(source, Configuration.getProperty("traceFile"));

            try {
                if (!exTrace.exists())
                    throw new IOException("Cannot get the execution trace file.");

                //Parsing the traces
                new TraceParser(exTrace, outputModel, resourceSet)
                        .parse()
                        .save(Collections.EMPTY_MAP);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "An exception was caught when parsing the trace", e);
            }
            exTrace.delete();
        });

    }

    /**
     * Instrument the source code using the given processors
     */
    private void instrument() {
        sources.forEach(source -> {
            before.forEach(fileConsumer -> fileConsumer.accept(source));

            ModelUtils.buildResourceSet(source);

            instrumenterBuilder
                    .onDirectory(source)
                    .withQueueExecutionTrace()
                    .build()
                    .instrument(processors);

            LOGGER.info("Running the tests to create execution traces");
            new MavenRunner(new File(source, "pom.xml")).withPomDependencies(dependencies).run();

            after.forEach(fileConsumer -> fileConsumer.accept(source));
        });
    }

    /**
     * Set the dependencies before running the impact analysis
     */
    private void setUp() {
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
                break;
            case FILE:
                instrumenterBuilder = instrumenterBuilder.withSingleFileExecutionTrace();
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

    /**
     * Run the impact analysis using the configurations set by the user
     */
    public void runImpactAnalysis() {
        setUp();

        instrumentAndRunTests();

        parseTestTrace();
    }

    public void run() {
        setUp();

        instrument();

        parse();
    }

    /**
     * Parse the execution trace, using the right {@link Parser} on the right execution trace
     */
    private void parse() {
        //TODO
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
}
