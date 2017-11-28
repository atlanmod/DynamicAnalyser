package com.tblf.instrumentation.sourcecode;

import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.sourcecode.processors.TargetProcessor;
import com.tblf.instrumentation.sourcecode.processors.TestProcessor;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        binDirectory = new File(directory, Configuration.getProperty("instrumentedBinaries"));
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        Launcher spoonLauncher;

        try {
            addDependencies();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't add the dependencies", e);
        }

        if (FileUtils.getFile(directory, "pom.xml").exists()) {
            spoonLauncher = new MavenLauncher(directory.getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);

            //Add to the dependencies all the deps computed by spoon from the pom.xml
            dependencies.addAll(
                    Arrays.stream(spoonLauncher.getEnvironment().getSourceClasspath())
                            .map(File::new)
                            .collect(Collectors.toList())
            );

            //maven spoon launcher. automatically fetches the dependencies using the pom.xml
            LOGGER.info("pom.xml found ! Will be used to compute the dependencies of the application under instrumentation");
        } else {
            spoonLauncher = new Launcher(); //Standard spoon launcher. needs to define the dependencies by hand
            //FIXME test&sut directory not initialized
            spoonLauncher.addInputResource(testDirectory.getAbsolutePath());
            spoonLauncher.addInputResource(sutDirectory.getAbsolutePath());
            LOGGER.info("pom.xml not found ! Dependencies have to be specified. ");
        }

        //Add all the dependencies, more specifically the one needed by the instrumentation to the spoon classpath
        spoonLauncher.getEnvironment().setSourceClasspath(dependencies.stream().map(File::getAbsolutePath).toArray(String[]::new));

        //TODO use hashsets instead of standard lists to optimize the contains() method
        spoonLauncher.getEnvironment().setLevel(String.valueOf(Level.ALL));
        spoonLauncher.getEnvironment().setShouldCompile(true);
        spoonLauncher.getEnvironment().setAutoImports(true);
        spoonLauncher.getEnvironment().setNoClasspath(false);

        spoonLauncher.setBinaryOutputDirectory(binDirectory);

        spoonLauncher.addProcessor(new TestProcessor(tests));
        spoonLauncher.addProcessor(new TargetProcessor(targets));

        spoonLauncher.run();

        File file = new File("spooned");

        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                LOGGER.warning("Cannot delete the temp files created by the instrumentation at URI: " + file.getAbsolutePath());
            }
        }

        try {
            SingleURLClassLoader.getInstance().addURLs(new URL[]{binDirectory.toURI().toURL()});
        } catch (MalformedURLException e) {
            LOGGER.warning("Cannot add the instrumented classes to the classpath: " + e.getMessage());
        }
    }

    private void addDependencies() throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("Link-1.0.0.jar");
        File link = new File("./Link-1.0.0.jar");
        IOUtils.copy(inputStream, new FileOutputStream(link));

        if (link.exists()) {
            dependencies.add(link);
        } else {
            throw new IOException("Cannot find the linker dependency");
        }
    }

    public void setBinDirectory(File binDirectory) {
        this.binDirectory = binDirectory;
    }

}
