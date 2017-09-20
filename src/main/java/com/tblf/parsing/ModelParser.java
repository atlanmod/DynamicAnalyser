package com.tblf.parsing;

import com.tblf.util.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ClassDeclaration;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ModelParser {

    private Map<String, File> targets;
    private Map<String, File> tests;

    public ModelParser() {
        targets = new HashMap<>();
        tests = new HashMap<>();
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
                        tests.put(ModelUtils.getQualifiedName(classDeclaration), file);
                    } else {
                        targets.put(ModelUtils.getQualifiedName(classDeclaration), file);
                    }
                }
            }
        });
    }

    public Map<String, File> getTargets() {
        return targets;
    }

    public void setTargets(Map<String, File> targets) {
        this.targets = targets;
    }

    public Map<String, File> getTests() {
        return tests;
    }

    public void setTests(Map<String, File> tests) {
        this.tests = tests;
    }
}
