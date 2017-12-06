package com.tblf.parsing;

import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ModelParserTest {

    @Test
    public void checkParse() throws Exception {
        Resource model = ModelUtils.loadModelFromZip(new File("src/test/resources/models/junit_java.zip"));

        Map<String, File> tests = new HashMap<>();
        Map<String, File> targets = new HashMap<>();

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

        ModelParser modelParser = new ModelParser();
        modelParser.parse(model);

        Assert.assertFalse("No targets", modelParser.getTargets().isEmpty());
        Assert.assertFalse("No tests", modelParser.getTests().isEmpty());

        Assert.assertEquals("Different number of targets", targets.size(), modelParser.getTargets().size());
        Assert.assertEquals("Different number of tests", tests.size(), modelParser.getTests().size());

        FileUtils.forceDelete(new File("src/test/resources/models/junit_java.xmi"));
    }

    @Test
    public void checkParseOldJUnit() throws IOException {
        Resource model = ModelUtils.loadModelFromZip(new File("src/test/resources/models/joda-time_java.xmi.zip"));
        Map<String, File> tests = new HashMap<>();
        Map<String, File> targets = new HashMap<>();

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

        ModelParser modelParser = new ModelParser();
        modelParser.parse(model);

        Assert.assertFalse("No targets", modelParser.getTargets().isEmpty());
        Assert.assertFalse("No tests", modelParser.getTests().isEmpty());

        Assert.assertEquals("Different number of targets", targets.size(), modelParser.getTargets().size());
        Assert.assertEquals("Different number of tests", tests.size(), modelParser.getTests().size());

        FileUtils.forceDelete(new File("src/test/resources/models/joda-time_java.xmi"));
    }
}
