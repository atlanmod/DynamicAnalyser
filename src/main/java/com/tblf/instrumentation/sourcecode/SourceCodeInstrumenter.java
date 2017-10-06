package com.tblf.instrumentation.sourcecode;

import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.sourcecode.processors.TargetProcessor;
import com.tblf.instrumentation.sourcecode.processors.TestProcessor;
import spoon.Launcher;

import java.io.File;
import java.util.Collection;

public class SourceCodeInstrumenter implements Instrumenter {

    private File directory;
    private File sutDirectory;
    private File testDirectory;

    private File binDirectory;
    private File srcDirectory;

    private Collection<File> dependencies;

    public SourceCodeInstrumenter(File directory, Collection<File> dependencies) {
        this.directory = directory;
        this.dependencies = dependencies;
    }

    public SourceCodeInstrumenter(File directory) {
        this.directory = directory;
    }

    public SourceCodeInstrumenter(File sutDirectory, File testDirectory, Collection<File> dependencies) {
        this.sutDirectory = sutDirectory;
        this.testDirectory = testDirectory;
        this.dependencies = dependencies;
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        //TODO use hashsets instead of standard lists to optimize the contains() method
        Launcher spoonLauncher = new Launcher();

        spoonLauncher.getEnvironment().setShouldCompile(true);
        spoonLauncher.getEnvironment().setAutoImports(true);
        spoonLauncher.getEnvironment().setNoClasspath(true);

        addLinkerToDependencies();

        spoonLauncher.getEnvironment().setSourceClasspath(dependencies.stream().map(File::getAbsolutePath).toArray(String[]::new));

        spoonLauncher.addInputResource(testDirectory.toString());
        spoonLauncher.addInputResource(sutDirectory.toString());

        spoonLauncher.setBinaryOutputDirectory(binDirectory);
        spoonLauncher.setSourceOutputDirectory(srcDirectory);

        spoonLauncher.addProcessor(new TestProcessor());
        spoonLauncher.addProcessor(new TargetProcessor());

        spoonLauncher.run();
    }

    private void addLinkerToDependencies() {
        File file = new File("libs/Link-1.0.0.jar");
        if (file.exists()) {
            dependencies.add(file);
        }
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public File getTestDirectory() {
        return testDirectory;
    }

    public void setTestDirectory(File testDirectory) {
        this.testDirectory = testDirectory;
    }

    public File getSutDirectory() {
        return sutDirectory;
    }

    public void setSutDirectory(File sutDirectory) {
        this.sutDirectory = sutDirectory;
    }

    public File getBinDirectory() {
        return binDirectory;
    }

    public void setBinDirectory(File binDirectory) {
        this.binDirectory = binDirectory;
    }

    public File getSrcDirectory() {
        return srcDirectory;
    }

    public void setSrcDirectory(File srcDirectory) {
        this.srcDirectory = srcDirectory;
    }

    public Collection<File> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Collection<File> dependencies) {
        this.dependencies = dependencies;
    }
}
