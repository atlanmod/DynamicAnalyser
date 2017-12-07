package com.tblf.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FileUtils {
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    /**
     * Check if a project is analysable (i.e.) if it contains a pom with a jar packaging
     *
     * @param source the {@link File} directory
     * @return true if analysable, or false
     */
    public static boolean isAnalysable(File source) {
        File pom = new File(source, "pom.xml");

        if (!pom.exists())
            return false;

        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
        try {
            Model model = mavenXpp3Reader.read(new FileInputStream(pom));
            String packaging = model.getPackaging();
            if (packaging.equals("jar")) {
                return true;
            }
        } catch (IOException | XmlPullParserException e) {
            LOGGER.log(Level.INFO, "Cannot analyse the directory " + source.getAbsolutePath(), e);
        }

        return false;
    }

    /**
     * Check if the project is a multimodule project
     *
     * @param source the {@link File} directory
     * @return true if it contains modules, or false
     */
    public static boolean isParent(File source) {
        File pom = new File(source, "pom.xml");

        if (!pom.exists())
            return false;

        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
        try {
            Model model = mavenXpp3Reader.read(new FileInputStream(pom));
            List<String> moduleList = model.getModules();

            return moduleList.size() > 0;
        } catch (IOException | XmlPullParserException e) {
            LOGGER.log(Level.INFO, "Cannot analyse the directory " + source.getAbsolutePath(), e);
        }

        return false;
    }

    /**
     * Get all the submodule in a pomParent
     *
     * @return a {@link Collection} of {@link File}
     */
    public static Collection<? extends File> getModules(File parent) {
        Collection<File> files = new HashSet<>();

        try {
            Model model = new MavenXpp3Reader().read(new FileInputStream(new File(parent, "pom.xml")));
            Collection<String> modules = model.getModules();
            files.addAll(modules.stream().map(s -> new File(parent, s)).collect(Collectors.toSet()));
        } catch (IOException | XmlPullParserException e) {
            LOGGER.log(Level.INFO, "Cannot analyse the directory " + parent.getAbsolutePath(), e);
        }

        return files;
    }

    /**
     * Recursively get all the modules of a project
     * @param parent a {@link File} directory
     * @return a {@link Collection} of all the modules
     */
    public static Collection<? extends File> getAllModules(File parent) {
        Collection<File> files = new HashSet<>();
        files.add(parent);

        if (isParent(parent))
            getModules(parent).forEach(o -> files.addAll(getAllModules(o)));

        return files;
    }
}