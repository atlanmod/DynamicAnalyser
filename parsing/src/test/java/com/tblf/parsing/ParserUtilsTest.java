package com.tblf.parsing;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Created by Thibault on 02/10/2017.
 */
public class ParserUtilsTest {

    @Test
    public void checkGetPackageQNFromClassQN() {
        String classQN = "com.tblf.package.MyClass";
        Assert.assertEquals("com.tblf.package", ParserUtils.getPackageQNFromClassQN(classQN));
    }

    @Test
    public void checkGetPackageQNFromSUTClassUri() {
        File file = new File("src/main/java/com/tblf/SimpleProject/App.java");

        String packageQN = ParserUtils.getPackageQNFromSUTFile(file);
        Assert.assertEquals("com.tblf.SimpleProject", packageQN);
    }

    @Test
    public void checkPrintProgress() throws InterruptedException {
        long time = System.currentTimeMillis();
        for (int i = 0; i <= 100; ++i) {
            ParserUtils.printProgress(time,100, i);
            Thread.sleep(50);
        }
    }
}
