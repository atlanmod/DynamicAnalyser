package com.tblf.parsing;

import com.tblf.Model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationPackage;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.ocl.options.ParsingOptions;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by Thibault on 26/09/2017.
 */
public class TraceParser implements Runnable {
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private File file;
    private com.tblf.Model.Model analysisModel;
    private static final ModelFactory MODEL_FACTORY = ModelFactory.eINSTANCE;
    private ResourceSet resourceSet;
    private OCLHelper OCL_HELPER;
    private OCL ocl;

    /**
     *
     * @param file the file containing the execution trace
     * @param resourceSet a resource set containing the fragments
     */
    public TraceParser(File file, ResourceSet resourceSet) {
        this.file = file;
        this.resourceSet = resourceSet;
        this.analysisModel = MODEL_FACTORY.createModel();

        ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        OCL_HELPER = ocl.createOCLHelper();
        OCL_HELPER.setContext(JavaapplicationPackage.eINSTANCE.getEClassifier("Model"));

        ParsingOptions.setOption(ocl.getEnvironment(),
                ParsingOptions.implicitRootClass(ocl.getEnvironment()),
                EcorePackage.Literals.EOBJECT);
    }

    /**
     * With the resourceSet:
     * Find the correct package model using the qualifiedName of the class
     * Find the right class, then the right method, and finally the exact statement using position
     * @param qualifiedClass
     * @param method
     * @param startCol
     * @param endCol
     * @return The statement corresponding to the parameters entered
     */
    public Statement findStatementUsingPosition(String qualifiedClass, String method, int startCol, int endCol) {

        throw new RuntimeException("not implemented");
    }

    /**
     * Parse the trace file line by line.
     * Depending of the trace type, will either find the test being executed, or the SUT being executed, or the statement being executed
     * @return a impact analysis model
     */
    public com.tblf.Model.Model parse() {
        try {
            LineIterator lineIterator = FileUtils.lineIterator(file);
            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                String[] split = line.split(":");

                switch (split[0]){
                    case "&": //set the test
                        String packageName = ParserUtils.getPackageQNFromClassQN(split[1]);
                        LOGGER.info("Getting the test class: "+split[1]);

                        LOGGER.info("Getting the test method: "+split[2]);
                        break;
                    case "%": //set the SUT
                        LOGGER.info("Getting the target class: "+split[1]);

                        LOGGER.info("Getting the target method: "+split[2]);
                        break;
                    case "?": //get the statement using its line

                        break;
                    case "!": //get the statement using its position
                        throw new RuntimeException("not implemented");
                }
            }

            LineIterator.closeQuietly(lineIterator);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.analysisModel;
    }

    @Override
    public void run() {

    }
}
