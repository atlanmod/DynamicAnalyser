package com.tblf.parsing;

import com.tblf.Model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.Model;
import org.eclipse.gmt.modisco.java.Statement;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;

/**
 * Created by Thibault on 26/09/2017.
 */
public class TraceParser implements Runnable {
    private File file;
    private com.tblf.Model.Model analysisModel;
    private static final ModelFactory MODEL_FACTORY = ModelFactory.eINSTANCE;
    private ResourceSet resourceSet;

    public TraceParser(File file, ResourceSet resourceSet) {
        this.file = file;
        this.resourceSet = resourceSet;
        this.analysisModel = MODEL_FACTORY.createModel();
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

        throw new NotImplementedException();
    }

    public com.tblf.Model.Model parse() {
        try {
            LineIterator lineIterator = FileUtils.lineIterator(file);
            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                String[] split = line.split(":");

                switch (split[0]){
                    case "&": //set the test

                        break;
                    case "?": //set the SUT

                        break;
                    case "%": //get the statement using its line

                        break;
                    case "!":
                        throw new NotImplementedException();
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
