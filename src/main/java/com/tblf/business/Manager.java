package com.tblf.business;

import com.tblf.Link.FileTracer;
import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.instrumentation.InstrumentationType;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.bytecode.ByteCodeInstrumenter;
import com.tblf.instrumentation.sourcecode.SourceCodeInstrumenter;
import com.tblf.parsing.ModelParser;
import com.tblf.parsing.TraceParser;
import com.tblf.runner.JUnitRunner;
import com.tblf.util.Configuration;
import com.tblf.util.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Will be used to build the traces and parse it at a higher level
 */
public class Manager {

    private static final Logger LOGGER = Logger.getLogger("Manager");
    private File project;
    private ResourceSet resourceSet;

    /**
     * Build the traces of a project by instrumenting and running the tests
     * @param project
     * @return
     */
    public File buildTraces(File project) {
        ((FileTracer) FileTracer.getInstance()).reset();
        SingleURLClassLoader.getInstance().clear();

        // getting the xmi model file
        Optional<Path> pathOptional;

        this.project = project;
        File model;
        Resource resource = null;

        try {
            pathOptional = Files.walk(project.toPath(), 1).filter(path -> path.toString().endsWith("_java."+ Configuration.getProperty("modelFormat"))).findFirst();

            if (pathOptional.isPresent()) {
                model = pathOptional.get().toFile();
            } else throw new IOException("Couldn't find the MoDisco model !");

            //Loading the model
            if (model.toString().endsWith(".xmi")) {
                resource = ModelUtils.loadModel(model);
            } else if (model.toString().endsWith(".zip")) {
                resource = ModelUtils.loadModelFromZip(model);
            }
            this.resourceSet = resource.getResourceSet();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,"Could not parse the project", e);
        }



        //Parsing the model to differentiate the SUT from the tests
        ModelParser modelParser = new ModelParser();
        modelParser.parse(resource);

        if (modelParser.getTests().isEmpty() || modelParser.getTargets().isEmpty()) {
            LOGGER.warning("Could not find test or SUT classes");
            return null;
        }

        //Instrumenting the project
        Instrumenter instrumenter;
        String mode = Configuration.getProperty("mode");
        switch(InstrumentationType.valueOf(mode)){

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

        instrumenter.instrument(modelParser.getTargets().keySet(), modelParser.getTests().keySet());

        //Running the test suites
        JUnitRunner jUnitRunner = new JUnitRunner(SingleURLClassLoader.getInstance().getUrlClassLoader());

        jUnitRunner.runTests(modelParser.getTests().keySet());

        ((FileTracer) FileTracer.getInstance()).endTrace();

        //Analyzing the traces
        File trace = ((FileTracer) FileTracer.getInstance()).getFile();
        LOGGER.info("Full execution trace located at: " + trace.getAbsolutePath());

        return trace;
    }

    /**
     * parses the traces in an external thread
     * @param trace the {@link File} containing the trace
     * @return Resource a {@link Resource}
     */
    public Resource parseTraces(File trace) {
        File outputModel = new File(project, Configuration.getProperty("outputModel")+"."+Configuration.getProperty("outputFormat"));

        try {
            ModelUtils.addJavaApplicationModelFragments(project, this.resourceSet);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.log(Level.WARNING, "Could not gather the resources", e);
        }

        TraceParser traceParser = new TraceParser(trace, outputModel, resourceSet);
        //new Thread(traceParser).start(); //Paralleled
        Resource resource = traceParser.parse();
        try {
            resource.save(Collections.EMPTY_MAP);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't save the execution traces model", e);
        }

        return resource;
        //get the model at the end
    }
}
