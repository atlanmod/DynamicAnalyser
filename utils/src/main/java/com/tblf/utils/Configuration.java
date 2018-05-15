package com.tblf.utils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {

    private static Properties properties;
    private static File propertyFile;
    private static InputStream inputStream;
    private static final Logger LOGGER = Logger.getLogger("Configuration");

    static {
        properties = new Properties();
        try {
            propertyFile = new File("config.properties");
            if (! propertyFile.exists()) {
                inputStream = Configuration.class.getClassLoader().getResourceAsStream("config.properties");
            } else {
                inputStream = new FileInputStream(propertyFile);
            }
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading the properties", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * save the current config inside the config file.
     */
    public static void save() {
        try {
            properties.store(new FileOutputStream(propertyFile), null);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save the properties", e);
        }
    }

    /**
     * Save the current configuration in a new file
     * @param newConfigurationFile the new {@link File}
     */
    public static void save(File newConfigurationFile) {
        try {
            properties.store(new FileOutputStream(newConfigurationFile), null);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save the properties", e);
        }
    }
}
