package com.tblf.instrumentation;

import java.io.File;
import java.util.Collection;

/**
 * Created by Thibault on 20/09/2017.
 */
public interface Instrumenter {

    void instrument(Collection<String> targets, Collection<String> tests);

    ClassLoader getClassLoader();

    void setProjectFolder(File file);
}
