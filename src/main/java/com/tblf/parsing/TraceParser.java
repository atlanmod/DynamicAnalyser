package com.tblf.parsing;

import com.tblf.Model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.eclipse.gmt.modisco.java.Model;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;

/**
 * Created by Thibault on 26/09/2017.
 */
public class TraceParser {
    private File file;
    private Model javaModel;
    private com.tblf.Model.Model analysisModel;
    private static final ModelFactory MODEL_FACTORY = ModelFactory.eINSTANCE;

    public TraceParser(File file, Model model) {
        this.file = file;
        this.javaModel = model;
        this.analysisModel = MODEL_FACTORY.createModel();
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
}
