package com.tblf.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {

    private static Properties properties;
    private static final Logger LOGGER = Logger.getLogger("Configuration");

    static {
        properties = new Properties();
        File file = new File("../utils/src/main/resources/config.properties");
        System.out.println(file.getAbsolutePath());
        try {
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading the properties", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static void setProperty(String key, String value) {properties.setProperty(key, value);
    }
}
