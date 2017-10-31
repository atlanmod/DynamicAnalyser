package com.tblf.classloading;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

/**
 * Created by Thibault on 20/09/2017.
 * An {@link URLClassLoader} able to load bytes arrays
 */
public class InstURLClassLoader extends URLClassLoader {
    public InstURLClassLoader(URL[] urls) {
        super(urls);
    }

    public InstURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void loadBytes(byte[] bytes) {
        Class aClass = defineClass(null, bytes, 0, bytes.length);
    }

    public void loadBytes(byte[] bytes, String name) {
        defineClass(name, bytes, 0, bytes.length);
    }

    public void loadBytes(ByteBuffer byteBuffer) {
        Class aClass = defineClass((String)null, byteBuffer, (ProtectionDomain)null);
    }
}
