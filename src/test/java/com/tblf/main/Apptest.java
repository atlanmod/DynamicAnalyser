package com.tblf.main;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Thibault on 21/09/2017.
 */
public class Apptest {

    @Test
    public void checkMain() {
        try {
            App.main(new String[]{"src/test/resources/junit_java.zip", "src/test/resources/binaries/junit/bin"});
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
