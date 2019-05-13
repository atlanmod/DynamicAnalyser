package com.tblf.utils;

import java.io.*;
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
            File file = new File("config.properties");
            System.out.println("Trying to look for properties at : "+file.getAbsolutePath());
            if (file.exists())
                inputStream = new FileInputStream(file);
            else
                inputStream = Configuration.class.getClassLoader().getResourceAsStream("config.properties");

            properties.load(inputStream);
            LOGGER.log(Level.INFO, "Properties successfully loaded ");
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

    /**
     * Replace the existing configuration with a new Configuration using an external {@link File}
     * @param newConfigurationFile a {@link File} containing properties.
     */
    public static void load(File newConfigurationFile) {
        try {
            properties.load(new FileInputStream(newConfigurationFile));
            LOGGER.log(Level.INFO, "Loaded the configuration file "+newConfigurationFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load the given file "+newConfigurationFile.getAbsolutePath());
        }
    }
}
