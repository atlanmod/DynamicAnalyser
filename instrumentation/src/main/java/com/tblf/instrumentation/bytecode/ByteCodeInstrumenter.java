package com.tblf.instrumentation.bytecode;

import com.tblf.DotCP.DotCPParserBuilder;
import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.bytecode.visitors.TargetClassVisitor;
import com.tblf.instrumentation.bytecode.visitors.TestClassVisitor;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 20/09/2017.
 */
public class ByteCodeInstrumenter extends Instrumenter {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    /**
     * The folder containing the SUT binaries
     */
    private File sutDirectory;

    /**
     * The folder containing the test binaries
     */
    private File testDirectory;

    /**
     * The directory containing the project
     */
    private File directory;

    /**
     * A singleton class loading the bytes arrays instrumented
     */
    private static SingleURLClassLoader singleURLClassLoader = SingleURLClassLoader.getInstance();

    /**
     * Set the root directory of the project ton instrument
     *
     * @param project the {@link File} directory
     */
    public void setDirectory(File project) {
        this.directory = project;
        this.testDirectory = new File(project, Configuration.getProperty("testBinaries"));
        this.sutDirectory = new File(project, Configuration.getProperty("sutBinaries"));
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        SingleURLClassLoader.getInstance().clear();

        int[] scores = new int[]{0, 0, 0, 0};
        try {
            fetchDependencies();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load the dependencies", e);
        }

        Collection<File> allClasses = new HashSet<>();

        try {
            Files.walk(sutDirectory.toPath()).filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                allClasses.add(path.toFile());
            });

            Files.walk(testDirectory.toPath()).filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                allClasses.add(path.toFile());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


        HashMap<String, File> targetClasses = new HashMap<>();
        HashMap<String, File> testClasses = new HashMap<>();
        Collection<String> allClassesQualifiedNames = new ArrayList<>(targets);
        allClassesQualifiedNames.addAll(tests);

        sortClasses(allClassesQualifiedNames, targetClasses, testClasses, allClasses);

        targetClasses.forEach((s, file) -> {
            try {
                LOGGER.fine("Instrumenting class " + s + " of classFile " + file.toString());
                byte[] targetAsByte = instrumentTargetClass(file, s);
                singleURLClassLoader.loadBytes(targetAsByte);
                IOUtils.write(targetAsByte, new FileOutputStream(file));
                scores[0]++;
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't instrument " + s, e);
                scores[1]++;
            } catch (LinkageError e) {
                LOGGER.log(Level.FINE, "Error with instrumentation of: " + s, e);
            }
        });

        testClasses.forEach((s, file) -> {
            try {
                LOGGER.fine("Instrumenting class " + s + " of classFile " + file.toString());
                byte[] targetAsByte = instrumentTestClass(file, s);
                singleURLClassLoader.loadBytes(targetAsByte);
                IOUtils.write(targetAsByte, new FileOutputStream(file));
                scores[2]++;
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't instrument " + s, e);
                scores[3]++;
            } catch (LinkageError e) {
                LOGGER.log(Level.FINE, "Error with instrumentation of: " + s, e);
            }
        });

        LOGGER.info(scores[0] +
                " targets loaded " + scores[1] +
                " target fails " + scores[2] +
                " test loaded " + scores[3] +
                " test fails ");
    }

    @Override
    public void instrument(Collection<Object> processors) {

        Collection<ClassVisitor> classVisitors = processors.stream().map(o -> ((ClassVisitor) o)).collect(Collectors.toList());

        try {

            Files.walk(directory.toPath(), Integer.MAX_VALUE)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> classVisitors.forEach(classVisitor -> {

                try {
                    ClassReader classReader = new ClassReader(new FileInputStream(path.toFile()));
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    FieldUtils.writeField(classVisitor, "cv", classWriter, true );
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
                    IOUtils.write(classWriter.toByteArray(), fileOutputStream);
                    fileOutputStream.close();
                } catch (IOException | IllegalAccessException | LinkageError e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not instrument bytecode of "+directory.getAbsolutePath(), e);
        }
    }

    private byte[] instrumentTargetClass(File target, String qualifiedName) throws IOException {

        InputStream inputStream = new FileInputStream(target);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classReader.accept(new TargetClassVisitor(Opcodes.ASM5, classWriter, qualifiedName), ClassReader.EXPAND_FRAMES);

        return classWriter.toByteArray();
    }

    private byte[] instrumentTestClass(File test, String qualifiedName) throws IOException {
        InputStream inputStream = new FileInputStream(test);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classReader.accept(new TestClassVisitor(Opcodes.ASM5, classWriter, qualifiedName), ClassReader.EXPAND_FRAMES);

        return classWriter.toByteArray();
    }

    /**
     * Iterate over all the qualified names. Find their corresponding file, and classified them if they're a test of a target class.
     * @param allClassesQualifiedNames a {@link Collection} of {@link String}
     * @param targetClasses a {@link HashMap} with qualified names as key, and {@link File} as values
     * @param testClasses   a {@link HashMap} with qualified names as key, and {@link File} as values
     * @param allClasses a {@link Collection} containing all the compiled files
     */
    private void sortClasses(Collection<String> allClassesQualifiedNames,
                             HashMap<String, File> targetClasses,
                             HashMap<String, File> testClasses,
                             Collection<File> allClasses) {

        allClassesQualifiedNames.forEach(s ->
                {
                    String[] split = s.split("\\.");
                    String name = split[split.length - 1]; //last segment
                    Collection<File> filesWithAMatchingName = allClasses.stream()
                            .filter(file1 -> file1.getName().equals(name+".class"))
                            .collect(Collectors.toList());

                    filesWithAMatchingName.forEach(file -> {
                        if (file.getAbsolutePath().contains(sutDirectory.getAbsolutePath())) {
                            //is a target
                            targetClasses.put(s, file);
                        }

                        if (file.getAbsolutePath().contains(testDirectory.getAbsolutePath())) {
                            //is a test class
                            testClasses.put(s, file);
                        }
                    });
                }
        );
    }

    private void fetchDependencies() throws ParserConfigurationException, SAXException, IOException, URISyntaxException {

        //Getting the dependencies from the .classpath file, assuming it is located in the same folder as the zip
        File dotCP = FileUtils.getFile(directory, ".classpath");

        if (dotCP.exists()) {
            dependencies.addAll(new DotCPParserBuilder().create().parse(dotCP));
        } else {
            LOGGER.warning("no .classpath file in the folder: load this project within an Eclipse application or run the goal 'mvn eclipse:eclipse'");
        }

        LOGGER.info("Adding the following dependencies to the classpath: " + dependencies.toString());

        dependencies.add(sutDirectory); //This is necessary to add the SUT classes before instrumentation.
        dependencies.add(testDirectory); //This is necessary to add the test classes before instrumentation.

        URL[] dependencyArray = dependencies.stream().map(file -> {
            URL url = null;
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException ignored) {
                LOGGER.warning("Cannot add the dependency to the classpath: " + file.toURI());
            }
            return url;
        }).collect(Collectors.toList()).toArray(new URL[dependencies.size()]);

        //Instrumenting the binaries
        singleURLClassLoader.addURLs(dependencyArray);
    }

    @Override
    public ClassLoader getClassLoader() {
        return SingleURLClassLoader.getInstance().getClassLoader();
    }
}
