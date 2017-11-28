package com.tblf.parsing;

import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.Model;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

        Collection<ClassDeclaration> targetCollection = ModelUtils.queryForAllClasses(model);
        Collection<ClassDeclaration> testCollection = ModelUtils.queryForTestClasses(model);
        targetCollection.removeAll(testCollection);

        targets = targetCollection  .stream()
                                    .filter(classDeclaration -> classDeclaration.getOriginalCompilationUnit() != null)
                                    .collect(Collectors.toMap(ModelUtils::getQualifiedName, ModelUtils::getSrcFromClass, (s, s2) -> s));

        tests = testCollection  .stream()
                                .filter(classDeclaration -> classDeclaration.getOriginalCompilationUnit() != null)
                                .collect(Collectors.toMap(ModelUtils::getQualifiedName, ModelUtils::getSrcFromClass, (s, s2) -> s));
    }

    public Map<String, File> getTargets() {
        return targets;
    }

    public Map<String, File> getTests() {
        return tests;
    }
}
