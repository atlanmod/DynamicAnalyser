package com.tblf.parsing;

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
        Resource model = ModelUtils.loadModelFromZip(new File("src/test/resources/junit_java.zip"));
        ModelParser modelParser = new ModelParser();
        modelParser.parse(model);

        Assert.assertFalse(modelParser.getTargets().isEmpty());

        Assert.assertFalse(modelParser.getTests().isEmpty());
    }
}
