package com.tblf.instrumentation;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Thibault on 20/09/2017.
 */
public class InstrumentationUtils {

    /**
     * Fetch inside a the folder containing the binaries, the compile .class file
     * corresponding to the qualifiedName
     * @param rootBinFolder
     * @param qualifiedName
     * @return the .class {@link File}
     */
    public static File getClassFile(File rootBinFolder, String qualifiedName) throws IOException {

        if (!rootBinFolder.exists() || !rootBinFolder.isDirectory()) {
            throw new IOException("The binaries folder is incorrect");
        }

        StringBuilder stringBuilder = new StringBuilder();

        if (! rootBinFolder.toString().endsWith("/")) {
            stringBuilder.append("/");
        }

        stringBuilder.append(qualifiedName.replace(".", "/"));
        stringBuilder.append(".class");

        File file = FileUtils.getFile(rootBinFolder, stringBuilder.toString());

        if (file.exists()) {
            return file;
        } else {
            throw new FileNotFoundException("Error while finding the class file: "+file.toURL());
        }
    }
}
