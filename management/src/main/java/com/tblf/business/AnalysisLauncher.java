package com.tblf.business;

import com.tblf.instrumentation.InstrumentationType;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.InstrumenterBuilder;
import com.tblf.instrumentation.bytecode.ByteCodeInstrumenter;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.parsing.ModelParser;
import com.tblf.parsing.TraceParser;
import com.tblf.utils.Configuration;
import com.tblf.utils.FileUtils;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AnalysisLauncher {

    private static final Logger LOGGER = Logger.getLogger(AnalysisLauncher.class.getName());

    private File root;
    private List<? extends File> sources;
    private ResourceSet resourceSet;
    private File outputModel;
    private Instrumenter instrumenter;
    private InstrumenterBuilder instrumenterBuilder;

    private List<Consumer<File>> before;
    private List<Consumer<File>> after;

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
    }

    /**
     * Set the instrumentation mode
     *
     * @param instrumentationType an {@link InstrumentationType} can be either sourcecode or bytecode
     */
    public void setInstrumentationType(InstrumentationType instrumentationType) {
        Configuration.setProperty("mode", instrumentationType.toString());
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
    private void impactAnalysis() {
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
                instrumenterBuilder.build().instrument(modelParser.getTargets().keySet(), modelParser.getTests().keySet());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "An error was caught during the impact analysis", e);
            }

            LOGGER.info("Running the tests ");
            // Running the tests to build the execution trace

            try {
                File exTrace;

                //new JUnitRunner(instrumenter.getClassLoader()).runTests(modelParser.getTests().keySet());
                new MavenRunner(new File(source, "pom.xml")).run();
                exTrace = new File(source, "executionTrace.extr");

                if (!exTrace.exists())
                    throw new IOException("Cannot get the execution trace file.");

                //Parsing the traces
                new TraceParser(exTrace, outputModel, resourceSet)
                        .parse()
                        .save(Collections.EMPTY_MAP);

                exTrace.delete();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "An error was caught during the impact analysis", e);
            }

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

        if (outputModel == null)
            outputModel = new File(root, Configuration.getProperty("outputModel") + "." + Configuration.getProperty("outputFormat"));

        instrumenterBuilder = instrumenterBuilder.withOutputDirectory(outputModel);
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
    public void run() {
        setUp();

        impactAnalysis();

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
