package com.tblf.junitrunner;

import com.tblf.linker.Calls;
import com.tblf.utils.Configuration;
import com.tblf.utils.MavenUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MavenRunner {
    private static final Logger LOGGER = Logger.getLogger(MavenRunner.class.getName());

    private File pom;

    public MavenRunner(File pom) {
        this.pom = pom;
    }

    /**
     * Run the all the test cases using maven. The instrumented binaries are merged inside the /target/classes folder.
     * The instrumentation jar is added to the test classpath
     *
     */
    public void run() {
        if (System.getProperty("maven.home") == null)
            System.setProperty("maven.home", Configuration.getProperty("MAVEN_HOME"));

        File pomDir = pom.getParentFile();

        //check that the project has been compiled
        if (! (new File(pomDir, "target").exists()
                && new File(pomDir, "target/classes").exists()
                && new File(pomDir, "target/test-classes").exists())) {

            MavenUtils.compilePom(pom);
        }


        // add the com.tblf.instrumentation jar in the project classpath

        File instrumentedBinaries = new File(pomDir, Configuration.getProperty("instrumentedBinaries"));
        File targetClasses = new File(pomDir, Configuration.getProperty("sutBinaries"));
        File testClasses = new File(pomDir, Configuration.getProperty("testBinaries"));
        if (instrumentedBinaries.exists() && instrumentedBinaries.isDirectory() && instrumentedBinaries.listFiles() != null) {

            //Moves all the instrumented binaries inside the maven compiled classes folder.
            //This way, when the surefire plugin loads the classes, it loads the instrumented ones instead
            try {
                Files.walk(instrumentedBinaries.toPath()).forEach(path -> {
                    if (!path.toFile().isDirectory()) {
                        String s = path.toAbsolutePath().toString().replace(instrumentedBinaries.getAbsolutePath(), testClasses.getAbsolutePath());
                        try {
                            Files.copy(path, new File(s).toPath(), StandardCopyOption.REPLACE_EXISTING);
                            LOGGER.fine("Replaced "+path.toString()+" by "+s);
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Could not write: "+s, e);
                        }
                    }
                });

                MavenUtils.addFileInPomTestClassPath(pom, new File(Calls.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not replace the existing classes with the instrumented ones", e);
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Could not get the Jar", e);
            }
        }

        MavenUtils.runTestsOnly(pom);
    }


}
