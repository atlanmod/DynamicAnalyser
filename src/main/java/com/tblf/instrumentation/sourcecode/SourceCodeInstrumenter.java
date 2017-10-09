package com.tblf.instrumentation.sourcecode;

import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.sourcecode.processors.TargetProcessor;
import com.tblf.instrumentation.sourcecode.processors.TestProcessor;
import org.apache.commons.io.FileUtils;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceCodeInstrumenter implements Instrumenter {
    private static final Logger LOGGER = Logger.getLogger("Instrumenter");

    private File directory;
    private File sutDirectory;
    private File testDirectory;

    private File binDirectory;
    private File srcDirectory;

    private Collection<File> dependencies;

    public SourceCodeInstrumenter(File directory) {
        this.directory = directory;
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        Launcher spoonLauncher;
        addDependencies();
        if (FileUtils.getFile(directory, "pom.xml").exists()) {
            spoonLauncher = new MavenLauncher(directory.getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
            //maven spoon launcher. automatically fetches the dependencies using the pom.xml
            LOGGER.info("pom.xml found ! Will be used to compute the dependencies of the application under instrumentation");
        } else {
            spoonLauncher = new Launcher(); //Standard spoon launcher. needs to define the dependencies by hand
            spoonLauncher.addInputResource(testDirectory.getAbsolutePath());
            spoonLauncher.addInputResource(sutDirectory.getAbsolutePath());
            spoonLauncher.getEnvironment().setSourceClasspath(dependencies.stream().map(File::getAbsolutePath).toArray(String[]::new));
            LOGGER.info("pom.xml not found ! Dependencies have to be specified. ");
        }

        //TODO use hashsets instead of standard lists to optimize the contains() method
        spoonLauncher.getEnvironment().setLevel(String.valueOf(Level.ALL));
        spoonLauncher.getEnvironment().setShouldCompile(true);
        spoonLauncher.getEnvironment().setAutoImports(true);
        spoonLauncher.getEnvironment().setNoClasspath(false);

        spoonLauncher.setBinaryOutputDirectory(binDirectory);

        spoonLauncher.addProcessor(new TestProcessor());
        spoonLauncher.addProcessor(new TargetProcessor());

        spoonLauncher.run();
    }

    /**
     * Instrument a maven project
     * @param mvnProj
     */
    public void instrumentMavenProject(File mvnProj) {
        MavenLauncher mavenLauncher = new MavenLauncher(mvnProj.getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
        mavenLauncher.addProcessor(new TargetProcessor());
        mavenLauncher.addProcessor(new TestProcessor());
        //mavenLauncher.setSourceOutputDirectory(srcDirectory);
        mavenLauncher.setBinaryOutputDirectory(binDirectory);
        mavenLauncher.getEnvironment().setShouldCompile(true);
        mavenLauncher.run();
    }

    private void addDependencies() {
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
