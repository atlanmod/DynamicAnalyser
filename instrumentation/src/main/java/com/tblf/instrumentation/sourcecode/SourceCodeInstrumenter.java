package com.tblf.instrumentation.sourcecode;

import com.tblf.DotCP.DotCPParserBuilder;
import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.sourcecode.processors.ClassProcessor;
import com.tblf.instrumentation.sourcecode.processors.TargetProcessor;
import com.tblf.instrumentation.sourcecode.processors.TestProcessor;
import com.tblf.linker.Calls;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.processing.Processor;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SourceCodeInstrumenter extends Instrumenter {
    private static final Logger LOGGER = Logger.getLogger("Instrumenter");

    private File binDirectory;

    /**
     * Set the root directory of the project ton instrument
     *
     * @param directory the {@link File} directory
     */
    public void setDirectory(File directory) {
        this.directory = directory;
        binDirectory = new File(directory, Configuration.getProperty("instrumentedBinaries"));
        sutDirectory = new File(directory, Configuration.getProperty("sut"));
        testDirectory = new File(directory, Configuration.getProperty("test"));

        if (outputDirectory == null)
            outputDirectory = new File("spooned");
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        SingleURLClassLoader.getInstance().clear();

        Launcher spoonLauncher = generateSpoonLauncher();

        //Add all the dependencies, more specifically the one needed by the instrumentation to the spoon classpath

        spoonLauncher.addProcessor(new ClassProcessor());
        spoonLauncher.addProcessor(new TestProcessor(tests));
        spoonLauncher.addProcessor(new TargetProcessor(targets));

        spoonLauncher.run();

        deleteSources();
    }


    @Override
    public void instrument(Collection<Object> processors) {
        Launcher spoonLauncher = generateSpoonLauncher();

        processors.stream().filter(o -> o instanceof Processor).map(o -> (Processor) o).forEach(spoonLauncher::addProcessor);

        spoonLauncher.run();
    }

    private void deleteSources() {
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

    /**
     * Generate the {@link Launcher} using Spoon.
     * The properties of the directory will decide if a standard {@link Launcher} is created, or the {@link MavenLauncher}
     *
     * @return a {@link Launcher}
     */
    private Launcher generateSpoonLauncher() {
        Launcher launcher;

        try {
            addDependencies();
            computeDependencies();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Couldn't add the dependencies", e);
        }

        if (FileUtils.getFile(directory, "pom.xml").exists() && "AUTO".equals(Configuration.getProperty("spoon.mode"))) {
            launcher = new MavenLauncher(directory.getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);

            //Add to the dependencies all the deps computed by spoon from the pom.xml
            dependencies.addAll(
                    Arrays.stream(launcher.getEnvironment().getSourceClasspath())
                            .map(File::new)
                            .collect(Collectors.toList())
            );

            //maven spoon launcher. automatically fetches the dependencies using the pom.xml
            LOGGER.info("pom.xml found ! Will be used to compute the dependencies of the application under instrumentation");
        } else {
            launcher = new Launcher(); //Standard spoon launcher. needs to define the dependencies by hand
            launcher.addInputResource(directory.getAbsolutePath());
            LOGGER.info("computing the dependencies without maven");
        }
        launcher.getEnvironment().setSourceClasspath(dependencies.stream().map(File::getAbsolutePath).toArray(String[]::new));

        launcher.getEnvironment().setLevel("OFF");
        launcher.getEnvironment().setShouldCompile(true);
        launcher.getEnvironment().setAutoImports(false);
        launcher.getEnvironment().setNoClasspath(false);

        launcher.setBinaryOutputDirectory(binDirectory);
        launcher.setSourceOutputDirectory(outputDirectory);

        return launcher;
    }


    @Override
    public ClassLoader getClassLoader() {
        return SingleURLClassLoader.getInstance().getClassLoader();
    }

    private void addDependencies() throws IOException, URISyntaxException {
        dependencies.add(new File(Calls.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        dependencies.add(new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
    }

    private void computeDependencies() throws IOException, ParserConfigurationException, SAXException {
        File dotCP = FileUtils.getFile(directory, ".classpath");
        if (!dotCP.exists()) {
            LOGGER.warning("no .classpath file in the folder: load this project within an Eclipse application \n" +
                    "or run the goal 'mvn eclipse:eclipse' \n" +
                    "if external dependencies have to be automatically computed \n");
        } else {
            List<File> computedDependencies = new DotCPParserBuilder().create().parse(dotCP);
            dependencies.addAll(computedDependencies);
        }
    }

    public void setBinDirectory(File binDirectory) {
        this.binDirectory = binDirectory;
    }

}
