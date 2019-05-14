package com.tblf.utils;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utils methods to manipulate maven projects
 */
public class MavenUtils {
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private static Invoker INVOKER;

    static {
        INVOKER = new DefaultInvoker();
    }
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

            DefaultInvoker defaultInvoker = new DefaultInvoker();
            InvokerLogger invokerLogger = new SystemOutLogger();
            invokerLogger.setThreshold(2);
            defaultInvoker.setLogger(invokerLogger);
            InvocationResult invocationResult = defaultInvoker.execute(invocationRequest);
            if (invocationResult.getExecutionException() != null) {
                LOGGER.info(invocationResult.getExecutionException().getMessage());
                invocationResult.getExecutionException().fillInStackTrace().printStackTrace();
            }
        } catch (IOException | MavenInvocationException e) {
            LOGGER.log(Level.WARNING, "Could not compile the maven project: "+dir.getAbsolutePath(), e);
        }
    }

    /**
     * Adds a new maven dependency to a specified pom.
     * @param pom the pom.xml {@link File} in which to add the dependency
     * @param file the pom.xml {@link File} dependency
     */
    public static void addDependencyToPom(File pom, File file) {
        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
        LOGGER.log(Level.INFO, "Adding "+file.getAbsolutePath()+" dependency to "+pom.getAbsolutePath());
        try {

            Model pomModelToUse = mavenXpp3Reader.read(new FileInputStream(file));

            String artifactId = pomModelToUse.getArtifactId();
            String groupId = pomModelToUse.getGroupId();
            String version = pomModelToUse.getVersion();

            if (groupId == null)
                groupId = pomModelToUse.getParent().getGroupId();

            if (version == null)
                version = pomModelToUse.getParent().getVersion();


            addDependencyToPom(pom, groupId, artifactId, version);
        } catch (XmlPullParserException | IOException e) {
            LOGGER.log(Level.WARNING, "Could not update the pom", e);

        }
    }

    public static void addDependencyToPom(File pom, String groupId, String artifactId, String version) {
        try {
            Model pomModelToUpdate = new MavenXpp3Reader().read(new FileInputStream(pom));
            Collection<Dependency> dependencyCollection = pomModelToUpdate.getDependencies();

            if (dependencyCollection == null)
                dependencyCollection = new ArrayList<>();

            if (! dependencyCollection.stream().anyMatch(d -> d.getArtifactId().equals(artifactId) && d.getGroupId().equals(groupId) && d.getVersion().equals(version))) {
                Dependency dependency = new Dependency();
                dependency.setArtifactId(artifactId);
                dependency.setVersion(version);
                dependency.setGroupId(groupId);

                dependencyCollection.add(dependency);
                new MavenXpp3Writer().write(new FileOutputStream(pom), pomModelToUpdate);
            }
        } catch (XmlPullParserException | IOException e) {
            LOGGER.log(Level.WARNING, "Could not update the pom", e);
        }
    }

    /**
     * Add a specific file in the maven surefire plugin classpath configuration
     *
     * @param file a {@link File}z
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

            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

            if (configuration == null) {
                configuration = new Xpp3Dom("configuration");
                plugin.setConfiguration(configuration);
            }

            Xpp3Dom addClassPathElts;
            addClassPathElts = configuration.getChild("additionalClasspathElements");

            if (addClassPathElts == null) {
                addClassPathElts = new Xpp3Dom("additionalClasspathElements");
                configuration.addChild(addClassPathElts);
            }


            Xpp3Dom addClassPathElt = new Xpp3Dom("additionalClasspathElement");
            addClassPathElt.setValue(file.getAbsolutePath());
            addClassPathElts.addChild(addClassPathElt);

            MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
            mavenXpp3Writer.write(new FileOutputStream(pom), model);

            System.out.println(FileUtils.readFileToString(pom, Charset.defaultCharset()));
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
        invocationRequest.setBaseDirectory(pom.getParentFile());
        invocationRequest.setGoals(Arrays.asList("-Djacoco.skip", "-DtestFailureIgnore=true", "surefire:test", "-fn"));

        try {
            InvocationResult invocationResult = INVOKER.execute(invocationRequest);
            if (invocationResult.getExecutionException() != null)
                LOGGER.log(Level.WARNING, "Could not run all the tests", invocationResult.getExecutionException().fillInStackTrace());
        } catch (MavenInvocationException e) {
            LOGGER.log(Level.WARNING, "Could not run the tests", e);
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
        plugin.setVersion("3.0.0-M3");	
        
        Object conf = plugin.getConfiguration();
        if (conf == null)
            plugin.setConfiguration(new Xpp3Dom("configuration"));

        Xpp3Dom argLine = ((Xpp3Dom) plugin.getConfiguration()).getChild("argLine");

        if (argLine == null) {
            argLine = new Xpp3Dom("argLine");
            ((Xpp3Dom) plugin.getConfiguration()).addChild(argLine);
            argLine.setValue(opt);
        } else {
            if (!argLine.getValue().contains(opt)) {
                argLine.setValue(argLine.getValue() + " " + opt);
            }
        }

        MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();

        try {
            assert model != null;
            mavenXpp3Writer.write(new FileOutputStream(pom), model);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not add surefire option to the pom", e);
        }
    }

    /**
     * Returns the surefire {@link Plugin} inside a {@link Model} or creates it if it does not exist
     * @param model a {@link Model}
     * @return the {@link Plugin}
     */
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
            plugin.setVersion("3.0.0-M3");
            model.getBuild().getPlugins().add(plugin);
        }

        return plugin;
    }

    private static void addBuild(Model model) {
    }

}

