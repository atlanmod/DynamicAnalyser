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
            long before = System.currentTimeMillis();
            App.main(new String[]{"src/test/resources/binaries/assertj/assertj-core_java.zip", "src/test/resources/binaries/assertj"});

            System.out.println(System.currentTimeMillis() - before);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
