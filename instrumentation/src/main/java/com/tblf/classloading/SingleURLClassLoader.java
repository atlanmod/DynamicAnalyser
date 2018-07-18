package com.tblf.classloading;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
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

    private InstURLClassLoader urlClassLoader;

    private SingleURLClassLoader() {
        urlClassLoader = new InstURLClassLoader(new URL[]{}, this.getClass().getClassLoader());
    }

    /**
     * Singleton getInstance() method
     * @return this, a {@link SingleURLClassLoader}
     */
    public static SingleURLClassLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SingleURLClassLoader();
        }

        return INSTANCE;
    }

    /**
     * Add urls to the already existing classloader
     * @param urls an {@link URL} array
     */
    public void addURLs(URL[] urls) {
        urlClassLoader = new InstURLClassLoader(urls, urlClassLoader);
    }

    /**
     * Empty the loaded classes contained in this classloader
     */
    public void clear() {
        try {
            this.urlClassLoader.close();
            this.urlClassLoader = new InstURLClassLoader(new URL[]{}, this.getClass().getClassLoader());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot clear the classLoader", e);
        }
    }

    /**
     * Load the bytes when the {@link URLClassLoader} is a {@link InstURLClassLoader}
     * @param bytes a byte array
     * @param name the name of the class to load
     */
    public void loadBytes(byte[] bytes, String name) {
        urlClassLoader.loadBytes(bytes, name);
    }

    /**
     * Load the bytes
     * @param bytes a byte array
     */
    public void loadBytes(byte[] bytes) {
        //urlClassLoader.loadBytes(ByteBuffer.wrap(bytes));
        urlClassLoader.loadBytes(bytes, null);
    }

    /**
     * Load a {@link ByteBuffer} in the {@link ClassLoader}
     * @param byteBuffer a {@link ByteBuffer}
     */
    public void loadBytes(ByteBuffer byteBuffer){
        urlClassLoader.loadBytes(byteBuffer);
    }

    /**
     * Gets urlClassLoader
     *
     * @return value of urlClassLoader
     */
    public ClassLoader getClassLoader() {
        return urlClassLoader;
    }
}
