package com.tblf.parsing;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Thibault on 02/10/2017.
 */
public class ParserUtilsTest {

    @Test
    public void checkGetPackageQNFromClassQN() {
        String classQN = "com.tblf.package.MyClass";
        Assert.assertEquals("com.tblf.package", ParserUtils.getPackageQNFromClassQN(classQN));
    }
}
