package com.tblf.parser;

import com.tblf.util.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ModelParserTest {

    @Test
    public void checkParse() throws Exception {
        Resource model = ModelUtils.loadModel(new File("src/test/resources/junit_java.xmi"));
        ModelParser modelParser = new ModelParser();
        modelParser.parse(model);

        Assert.assertFalse(modelParser.getTargets().isEmpty());
        Assert.assertTrue("Some files don't exist", modelParser.getTargets().stream().allMatch(File::exists));

        Assert.assertFalse(modelParser.getTests().isEmpty());
        Assert.assertTrue("Some files don't exist", modelParser.getTests().stream().allMatch(File::exists));
    }
}
