package com.tblf.instrumentation.sourcecode;

import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.sourcecode.processors.TargetProcessor;
import com.tblf.instrumentation.sourcecode.processors.TestProcessor;
import org.apache.commons.io.FileUtils;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SourceCodeInstrumenter implements Instrumenter {
    private static final Logger LOGGER = Logger.getLogger("Instrumenter");

    private File directory;
    private File sutDirectory;
    private File testDirectory;

    private File binDirectory;

    private Collection<File> dependencies;

    public SourceCodeInstrumenter(File directory) {
        this.directory = directory;
        dependencies = new ArrayList<>();
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

        File file = new File("spooned");

        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                LOGGER.warning("Cannot delete the temp files created by the instrumentation at URI: "+file.getAbsolutePath());
            }
        }

        try {
            SingleURLClassLoader.getInstance().addURLs(new URL[]{binDirectory.toURI().toURL()});
        } catch (MalformedURLException e) {
            LOGGER.warning("Cannot add the instrumented classes to the classpath: "+e.getMessage());
        }
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

    public Collection<File> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Collection<File> dependencies) {
        this.dependencies = dependencies;
    }
}
