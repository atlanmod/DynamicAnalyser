package com.tblf.parsing;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public class MultiThreadedTraceParser extends Parser {

    /**
     * Constructor defining the needed parameters for the parsing
     *
     * @param traceFile   the execution trace file. Can be either a simple file, or a folder containing multiple traces
     * @param outputModel the output model as a {@link File}
     * @param resourceSet the EMF {@link ResourceSet} containing the {@link Resource}s
     */
    public MultiThreadedTraceParser(File traceFile, File outputModel, ResourceSet resourceSet) {
        super(traceFile, outputModel, resourceSet);

        if (! traceFile.isDirectory()) {
            LOGGER.warning("The given file is not a directory. Cannot parallelize the model building");
        }
    }

    @Override
    public Resource parse() {
        try {
            Files.walk(trace.toPath()).forEach(path -> {
                this.parse(path.toFile());
            });
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot get the files inside "+trace.getAbsolutePath(), e);
        }

        return outputModel;
    }

    private void parse(File file) {

    }
}
