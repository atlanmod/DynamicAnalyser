package com.tblf.utils;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utils methods to manipulate maven projects
 */
public class MavenUtils {
    private static final Logger LOGGER = Logger.getAnonymousLogger();

    /**
     * Run eclipse:eclipse, clean, and compile builds on the mvn project
     *
     * @param dir the {@link File} containing the pom.xml
     */
    public static void compilePom(File dir) {
        File pom = new File(dir, "pom.xml");
        try {
            LOGGER.info("Building the maven project");
            InputStream inputStream = new FileInputStream(File.createTempFile("mvnOutput_", ".tmp"));
            InvocationRequest invocationRequest = new DefaultInvocationRequest();
            invocationRequest.setPomFile(pom);//.setBaseDirectory()
            invocationRequest.setGoals(Arrays.asList("clean", "eclipse:eclipse", "compile", "test-compile"));
            invocationRequest.setInputStream(inputStream);
            invocationRequest.setJavaHome(new File(Configuration.getProperty("JAVA_HOME")));

            Invoker invoker = new DefaultInvoker();

            InvocationResult invocationResult = invoker.execute(invocationRequest);
            if (invocationResult.getExecutionException() != null) {
                LOGGER.info(invocationResult.getExecutionException().getMessage());
                invocationResult.getExecutionException().fillInStackTrace().printStackTrace();
            }
        } catch (IOException | MavenInvocationException e) {
            LOGGER.log(Level.WARNING, "Could not compile the maven project: "+dir.getAbsolutePath(), e);
        }
    }

    /**
     * Add a specific file in the maven surefire plugin classpath configuration
     *
     * @param file a {@link File}
     */
    public static void addFileInPomTestClassPath(File pom, File file) {

        try {
            Model model = new MavenXpp3Reader().read(new FileInputStream(pom));

            Optional<Plugin> optPlugin = model
                    .getBuild()
                    .getPlugins()
                    .stream()
                    .filter(p -> p.getArtifactId().equals("maven-surefire-plugin"))
                    .findAny();

            Plugin plugin = getSureFirePlugin(model);

            Xpp3Dom addClassPathElts = new Xpp3Dom("additionalClasspathElements");
            Xpp3Dom addClassPathElt = new Xpp3Dom("additionalClasspathElement");
            addClassPathElt.setValue(file.getAbsolutePath());
            addClassPathElts.addChild(addClassPathElt);

            Object conf = plugin.getConfiguration();
            if (conf == null)
                plugin.setConfiguration(new Xpp3Dom("configuration"));

            ((Xpp3Dom) plugin.getConfiguration()).addChild(addClassPathElts);

            MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
            mavenXpp3Writer.write(new FileOutputStream(pom), model);

        } catch (IOException | XmlPullParserException e) {
            LOGGER.log(Level.WARNING, "Could not add: "+file.getAbsolutePath()+" in the surefire classpath", e);
        }


    }

    /**
     * Run all the test cases inside a maven project, using existing sources.
     * @param pom the pom.xml {@link File}
     */
    public static void runTestsOnly(File pom) {
        InvocationRequest invocationRequest = new DefaultInvocationRequest();
        invocationRequest.setJavaHome(new File(Configuration.getProperty("JAVA_HOME")));

        invocationRequest.setPomFile(pom);
        invocationRequest.setGoals(Arrays.asList("-Djacoco.skip", "-DtestFailureIgnore=true", "surefire:test"));

        try {
            InvocationResult invocationResult = new DefaultInvoker().execute(invocationRequest);
            if (invocationResult.getExecutionException() != null)
                LOGGER.log(Level.WARNING, "Could not run all the tests", invocationResult.getExecutionException().fillInStackTrace());
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add the specified option to the surefire configuration
     * @param pom the {@link File} pom.xml
     * @param opt the jvm options
     */
    public static void addJVMOptionsToSurefireConfig(File pom, String opt) {

        Model model = null;
        try {
            model = new MavenXpp3Reader().read(new FileInputStream(pom));
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        Plugin plugin = getSureFirePlugin(model);

        Object conf = plugin.getConfiguration();
        if (conf == null)
            plugin.setConfiguration(new Xpp3Dom("configuration"));

        Xpp3Dom argLine = ((Xpp3Dom) plugin.getConfiguration()).getChild("argLine");

        if (argLine == null) {
            argLine = new Xpp3Dom("argLine");
            ((Xpp3Dom) plugin.getConfiguration()).addChild(argLine);
        }

        argLine.setValue(opt);

        MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();

        try {
            assert model != null;
            mavenXpp3Writer.write(new FileOutputStream(pom), model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Plugin getSureFirePlugin(Model model) {

        Build build = model.getBuild();
        if (build == null)
            model.setBuild(new Build());

        Optional<Plugin> optPlugin = model
                .getBuild()
                .getPlugins()
                .stream()
                .filter(p -> p.getArtifactId().equals("maven-surefire-plugin"))
                .findAny();

        Plugin plugin;
        if (optPlugin.isPresent())
            plugin = optPlugin.get();
        else {
            plugin = new Plugin();
            plugin.setGroupId("org.apache.maven.plugins");
            plugin.setArtifactId("maven-surefire-plugin");
            plugin.setVersion("2.20.1");
            model.getBuild().getPlugins().add(plugin);
        }

        return plugin;
    }

    private static void addBuild(Model model) {
    }
}
