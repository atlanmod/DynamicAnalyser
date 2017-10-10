package com.tblf.util;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void checkGetProperty() {
        Assert.assertEquals(Configuration.getProperty("name"), "Dynamic-Analyser");
    }
}
