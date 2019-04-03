package com.tblf.utils;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void checkGetProperty() {
        Assert.assertEquals(Configuration.getProperty("name"), "Dynamic-Analyser");
    }

    @Test
    public void checkDynamicProperty() {
        Assert.assertNotNull(Configuration.getProperty("version"));
        Assert.assertNotNull(Configuration.getProperty("groupId"));
        Assert.assertNotNull(Configuration.getProperty("artifactId"));
    }
}
