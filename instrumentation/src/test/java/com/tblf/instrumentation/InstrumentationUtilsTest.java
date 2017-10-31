package com.tblf.instrumentation;

import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by Thibault on 20/09/2017.
 */
public class InstrumentationUtilsTest {

    @Test
    public void checkGetClassFile() throws IOException {
        ModelUtils.unzip(new File("src/test/resources/binaries/junit.zip"));
        File folder = new File("src/test/resources/binaries/junit/bin");
        try {
            File classFile = InstrumentationUtils.getClassFile(folder, "org.junit.experimental.categories.AllCategoriesTests");
            Assert.assertTrue(classFile.exists());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(folder);
    }
}
