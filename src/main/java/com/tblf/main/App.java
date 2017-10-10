package com.tblf.main;

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
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Main method of the Application
 */
public class App 
{
    private static final Logger LOGGER = Logger.getAnonymousLogger();


    /**
     * Main method
     * @param args
     * args[0]: project URI
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public static void main( String[] args ) throws Exception {
        //Checking the inputs
        if (args.length < 1) {
            LOGGER.warning("Incorrect arguments. Expected the project URI as an argument");
            return;
        }

        File project = new File(args[0]);

        // getting the xmi model file
        Optional<Path> pathOptional = Files.walk(project.toPath(), 1).filter(path -> path.toString().endsWith("_java."+Configuration.getProperty("modelFormat"))).findFirst();
        File model = null;

        Resource resource = null;

        if (pathOptional.isPresent()) {
            model = pathOptional.get().toFile();
        } else {
            LOGGER.warning("Couldn't find the MoDisco model !");
            System.exit(1);
        }

        //Loading the model
        if (model.toString().endsWith(".xmi")) {
            resource = ModelUtils.loadModel(model);
        } else if (model.toString().endsWith(".zip")) {
            resource = ModelUtils.loadModelFromZip(model);
        }

        //Parsing the model
        ModelParser modelParser = new ModelParser();
        modelParser.parse(resource);

        if (modelParser.getTests().isEmpty() || modelParser.getTargets().isEmpty()) {
            LOGGER.warning("Could not find test or SUT classes");
            System.exit(1);
        }

        Instrumenter instrumenter = null;
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
                System.exit(1);
                break;
        }

        instrumenter.instrument(modelParser.getTargets().keySet(), modelParser.getTests().keySet());

        //Running the test suites
        JUnitRunner jUnitRunner = new JUnitRunner(SingleURLClassLoader.getInstance().getUrlClassLoader());

        jUnitRunner.runTests(modelParser.getTests().keySet());

        ((FileTracer) FileTracer.getInstance()).endTrace();
        //Analyzing the traces
        File trace = ((FileTracer) FileTracer.getInstance()).getFile();
        LOGGER.info("Full execution trace located at: " + trace.getAbsolutePath());

        File outputModel = new File(project, Configuration.getProperty("outputModel")+"."+Configuration.getProperty("outputFormat"));

        ResourceSet resourceSet = ModelUtils.addJavaApplicationModelFragments(project, resource.getResourceSet());
        TraceParser traceParser = new TraceParser(trace, outputModel, resourceSet);
        traceParser.parse();

    }
}
