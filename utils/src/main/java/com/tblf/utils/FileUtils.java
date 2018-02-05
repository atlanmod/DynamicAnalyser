package com.tblf.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
            if (packaging.equals("jar") || packaging.equals("bundle")) {
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

    /**
     * Walk the directory to find pom.xml files. This is used when the user does not know where the poms are
     * @param root the {@link File} directory
     * @return a {@link Collection} of all the folders containing poms
     */
    public static Collection<? extends File> searchForPoms(File root) {
        Collection<File> files = new ArrayList<>();

        try {
            files.addAll(Files.walk(root.toPath())
                    .filter(path -> new File(path.toFile(), "pom.xml").exists())
                    .map(Path::toFile)
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not walk the directory to find pom.xml", e);
        }

        return files;
    }


    /**
     * Copy one file to another using NIO
     */
    public static void doCopy(final File source, final File destination)
            throws IOException {
        final Closer closer = new Closer();
        final RandomAccessFile src, dst;
        final FileChannel in, out;

        try {
            src = closer.add(new RandomAccessFile(source.getCanonicalFile(), "r"));
            dst = closer.add(new RandomAccessFile(destination.getCanonicalFile(), "rw"));
            in = closer.add(src.getChannel());
            out = closer.add(dst.getChannel());
            in.transferTo(0L, in.size(), out);
            out.force(false);
        } finally {
            closer.close();
        }
    }

    public static void clearFile(File file) throws FileNotFoundException {
        PrintWriter printWriter = new PrintWriter(file);
        printWriter.print("");
        printWriter.close();
    }
}