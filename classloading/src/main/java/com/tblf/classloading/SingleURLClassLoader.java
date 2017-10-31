package com.tblf.classloading;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Thibault on 19/09/2017.
 * Is a Singleton, conform to the design pattern
 * from "Design Patterns: Elements of Reusable Object-Oriented Software"
 *
 * Classic implementation
 */
public class SingleURLClassLoader {

    private static SingleURLClassLoader INSTANCE;
    private static final Logger LOGGER = Logger.getLogger("SingleURLClassLoader");

    private URLClassLoader urlClassLoader;

    private SingleURLClassLoader() {
        urlClassLoader = new InstURLClassLoader(new URL[]{}, this.getClass().getClassLoader());
    }

    /**
     * Singleton getInstance() method
     * @return
     */
    public static SingleURLClassLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SingleURLClassLoader();
        }

        return INSTANCE;
    }

    /**
     * Add urls to the already existing classloader
     * @param urls
     */
    public void addURLs(URL[] urls) {
        urlClassLoader = new InstURLClassLoader(urls, urlClassLoader);
    }

    public URLClassLoader getUrlClassLoader() {
        return urlClassLoader;
    }

    public void setUrlClassLoader(URLClassLoader urlClassLoader) {
        this.urlClassLoader = urlClassLoader;
    }

    /**
     * Empty the loaded classes contained in this classloader
     */
    public void clear() {
        try {
            this.getUrlClassLoader().close();
            this.urlClassLoader = new InstURLClassLoader(new URL[]{}, this.getClass().getClassLoader());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot clear the classLoader", e);
        }
    }
}
