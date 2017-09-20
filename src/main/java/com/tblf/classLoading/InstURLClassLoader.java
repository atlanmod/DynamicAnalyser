package com.tblf.classLoading;

import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by Thibault on 20/09/2017.
 * An {@link URLClassLoader} able to load bytes arrays
 */
public class InstURLClassLoader extends URLClassLoader {
    public InstURLClassLoader(URL[] urls) {
        super(urls);
    }

    public void loadBytes(byte[] bytes, String className) {
        defineClass(className, bytes, 0, bytes.length);
    }
}
