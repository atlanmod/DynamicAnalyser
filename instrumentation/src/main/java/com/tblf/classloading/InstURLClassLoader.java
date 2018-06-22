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
    InstURLClassLoader(URL[] urls) {
        super(urls);
    }

    InstURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    void loadBytes(byte[] bytes, String name) {
        defineClass(name, bytes, 0, bytes.length);
    }

    void loadBytes(ByteBuffer byteBuffer) {
        defineClass(null, byteBuffer, (ProtectionDomain)null);
    }
}
