package com.tblf.parser;

import com.tblf.util.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ClassDeclaration;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ModelParser {

    private List<File> targets;
    private List<File> tests;

    public ModelParser() {
        targets = new LinkedList<>();
        tests = new LinkedList<>();
    }

    /**
     * Build the lists containing the source files & the binaries
     * @param model
     */
    public void parse(Resource model) {
        model.getAllContents().forEachRemaining(object -> {
            if (object instanceof ClassDeclaration) {
                ClassDeclaration classDeclaration = (ClassDeclaration) object;

                if (classDeclaration.getOriginalCompilationUnit() != null) {
                    File file = ModelUtils.getSrcFromClass(classDeclaration);

                    if (ModelUtils.isATestClass(classDeclaration)) {
                        tests.add(file);
                    } else {
                        targets.add(file);
                    }
                }
            }
        });
    }

    public List<File> getTargets() {
        return targets;
    }

    public void setTargets(List<File> targets) {
        this.targets = targets;
    }

    public List<File> getTests() {
        return tests;
    }

    public void setTests(List<File> tests) {
        this.tests = tests;
    }
}
