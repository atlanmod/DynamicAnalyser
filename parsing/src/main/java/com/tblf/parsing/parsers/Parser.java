package com.tblf.parsing.parsers;

import com.tblf.model.ModelPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationPackage;
import org.eclipse.modisco.kdm.source.extension.ExtensionPackage;

import java.io.File;
import java.io.IOException;
import java.util.Map;
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

        JavaPackage.eINSTANCE.eClass();
        JavaapplicationPackage.eINSTANCE.eClass();
        ExtensionPackage.eINSTANCE.eClass();
        KdmPackage.eINSTANCE.eClass();
        ModelPackage.eINSTANCE.eClass();

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

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
