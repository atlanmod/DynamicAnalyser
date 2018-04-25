package com.tblf.instrumentation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Thibault on 20/09/2017.
 */
public abstract class Instrumenter {

    protected File directory;
    protected File sutDirectory;
    protected File testDirectory;
    protected File outputDirectory;

    protected Collection<File> dependencies;

    protected Instrumenter() {
        dependencies = new ArrayList<>();
    }

    public abstract void instrument(Collection<String> targets, Collection<String> tests);

    public abstract void instrument(Collection<Object> processors);

    public abstract ClassLoader getClassLoader();

    public File getDirectory() {
        return directory;
    }

    public File getSutDirectory() {
        return sutDirectory;
    }

    public File getTestDirectory() {
        return testDirectory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public void setSutDirectory(File sutDirectory) {
        this.sutDirectory = sutDirectory;
    }

    public void setTestDirectory(File testDirectory) {
        this.testDirectory = testDirectory;
    }

    public Collection<File> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Collection<File> dependencies) {
        this.dependencies = dependencies;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
}
