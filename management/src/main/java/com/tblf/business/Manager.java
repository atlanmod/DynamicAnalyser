package com.tblf.business;

import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.InstrumentationType;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.bytecode.ByteCodeInstrumenter;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import com.tblf.junitrunner.JUnitRunner;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.linker.FileTracer;
import com.tblf.parsing.ModelParser;
import com.tblf.parsing.TraceParser;
import com.tblf.utils.Configuration;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import spoon.MavenLauncher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Will be used to build the traces and parse it at a higher level
 */
@Deprecated
public class Manager {

    private static final Logger LOGGER = Logger.getLogger("Manager");
    private File project;
    private ResourceSet resourceSet;

    private Collection<String> sutClasses;
    private Collection<String> testClasses;

    /**
     * Generate the model out of the source code using MoDisco
     * @param project the {@link File} containing the project
     * @return the {@link ResourceSet} loaded
     */
    public ResourceSet buildModel(File project) {
        this.project = project;

        try {
            this.resourceSet = ModelUtils.buildResourceSet(project);

            Resource javaModel = this.resourceSet.getResources().stream().filter(resource -> resource.getURI().toString().endsWith("_java.xmi")).findFirst().orElseThrow(() -> new IOException("Couldnt' find the MoDisco java model"));
            ModelParser modelParser = new ModelParser();
            modelParser.parse(javaModel);

            sutClasses = modelParser.getTargets().keySet();
            testClasses = modelParser.getTests().keySet();

            LOGGER.info(sutClasses.size()+" sut classes");
            LOGGER.info(testClasses.size()+" test classes");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the Modisco model", e);
        }

        return this.resourceSet;
    }

    /**
     * Build the traces of a project by instrumenting and running the tests
     *
     * @param project the {@link File} containing the project executed to generate traces
     * @return the file containing the execution trace
     */
    public File buildTraces(File project) {
        FileTracer.getInstance().startTrace();
        SingleURLClassLoader.getInstance().clear();

        //Instrumenting the project
        Instrumenter instrumenter;
        String mode = Configuration.getProperty("mode");
        switch (InstrumentationType.valueOf(mode)) {

            case BYTECODE:
                instrumenter = new ByteCodeInstrumenter(project);
                break;
            case SOURCECODE:
                instrumenter = new SourceCodeInstrumenter(project);
                break;
            default:
                LOGGER.warning("No instrumentation chosen");
                return null;
        }

        instrumenter.instrument(this.sutClasses, this.testClasses);

        //Running the test suites
        new MavenRunner(project).run();
        FileTracer.getInstance().endTrace();

        //Analyzing the traces
        File trace = new File(project, "executionTrace.extr");
        LOGGER.info("Full execution trace located at: " + trace.getAbsolutePath());

        return trace;
    }

    /**
     * parses the traces in an external thread
     *
     * @param trace the {@link File} containing the trace
     * @return Resource a {@link Resource}
     */
    public Resource parseTraces(File trace) {
        File outputModel = new File(project, Configuration.getProperty("outputModel") + "." + Configuration.getProperty("outputFormat"));

        try {
            ModelUtils.addJavaApplicationModelFragments(project, this.resourceSet);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not gather the resources", e);
        }

        TraceParser traceParser = new TraceParser(trace, outputModel, resourceSet);

        Resource resource = traceParser.parse();

        try {
            resource.save(Collections.EMPTY_MAP);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't save the model", e);
        }

        return resource;
    }
}