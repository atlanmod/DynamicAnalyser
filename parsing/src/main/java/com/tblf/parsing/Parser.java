package com.tblf.parsing;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Parser {

    protected static final Logger LOGGER = Logger.getLogger("Parser");

    protected File trace;
    protected Resource outputModel;
    protected ResourceSet resourceSet;

    /**
     * Constructor defining the needed parameters for the parsing
     *
     * @param traceFile   the execution trace file. Can be either a simple file, or a folder containing multiple traces
     * @param outputModel the output model as a {@link File}
     * @param resourceSet the EMF {@link ResourceSet} containing the {@link Resource}s
     */
    public Parser(File traceFile, File outputModel, ResourceSet resourceSet) {
        this.trace = traceFile;

        try {
            if (!outputModel.exists() && !outputModel.createNewFile()) {
                throw new IOException("Cannot create the output model");
            }
            this.outputModel = resourceSet.createResource(URI.createURI(outputModel.toURI().toURL().toString()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot load the traces", e);
        }
    }

    /**
     * Parse the traces given as parameters in the constructor
     *
     * @return the traces
     */
    public abstract Resource parse();
}
