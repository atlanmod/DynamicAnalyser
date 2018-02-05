package com.tblf.instrumentation.bytecode;

import com.tblf.DotCP.DotCPParserBuilder;
import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.InstrumentationUtils;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.bytecode.visitors.TargetClassVisitor;
import com.tblf.instrumentation.bytecode.visitors.TestClassVisitor;
import com.tblf.linker.Calls;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 20/09/2017.
 */
public class ByteCodeInstrumenter implements Instrumenter {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

        /**
     * The folder containing the SUT binaries
     */
    private File sutBinFolder;

    /**
     * The folder containing the test binaries
     */
    private File testBinFolder;

    /**
     * The directory containing the project
     */
    private File projectFolder;

    /**
     * A singleton class loading the bytes arrays instrumented
     */
    private static SingleURLClassLoader singleURLClassLoader = SingleURLClassLoader.getInstance();

    public ByteCodeInstrumenter() {

    }

    public ByteCodeInstrumenter(File project) {
        this.setProjectFolder(project);
    }

    /**
     * Set the root directory of the project ton instrument
     * @param project the {@link File} directory
     */
    public void setProjectFolder(File project) {
        this.projectFolder = project;
        this.testBinFolder = new File(project, Configuration.getProperty("testBinaries"));
        this.sutBinFolder = new File(project, Configuration.getProperty("sutBinaries"));
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        SingleURLClassLoader.getInstance().clear();

        int[] scores = new int[]{0, 0, 0, 0};
        try {
            getDependencies();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load the dependencies", e);
        }

        targets.forEach(t -> {
            try {
                File target = InstrumentationUtils.getClassFile(sutBinFolder, t);
                LOGGER.fine("Instrumenting class "+t+" of classFile "+target.toString());
                byte[] targetAsByte = instrumentTargetClass(target, t);
                singleURLClassLoader.loadBytes(targetAsByte, t);
                IOUtils.write(targetAsByte, new FileOutputStream(target));
                scores[0]++;
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't instrument " + t, e);
                scores[1]++;
            } catch (LinkageError e) {
                LOGGER.log(Level.FINE, "Error with instrumentation of: "+t, e);
            }
        });

        tests.forEach(t -> {
            try {
                File target = InstrumentationUtils.getClassFile(testBinFolder, t);
                LOGGER.fine("Instrumenting class "+t+" of classFile "+target.toString());
                byte[] targetAsByte = instrumentTestClass(target, t);
                singleURLClassLoader.loadBytes(targetAsByte, t);
                IOUtils.write(targetAsByte, new FileOutputStream(target));
                scores[2]++;
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't instrument "+t, e);
                scores[3]++;
            } catch (LinkageError e) {
                LOGGER.log(Level.FINE, "Error with instrumentation of: "+t, e);
            }
        });

        LOGGER.info(scores[0]+
                " targets loaded "+ scores[1]+
                " target fails "+ scores[2]+
                " test loaded "+scores[3]+
                " test fails ");
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

    private void getDependencies() throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        List<File> dependencies = new ArrayList<>();

        //Getting the dependencies from the .classpath file, assuming it is located in the same folder as the zip
        File dotCP = FileUtils.getFile(projectFolder, ".classpath");

        if (dotCP.exists()) {
            dependencies.addAll(new DotCPParserBuilder().create().parse(dotCP));
        } else {
            LOGGER.warning("no .classpath file in the folder: load this project within an Eclipse application or run the goal 'mvn eclipse:eclipse'");
        }

        LOGGER.info("Adding the following dependencies to the classpath: "+dependencies.toString());

        dependencies.add(new File(Calls.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        dependencies.add(sutBinFolder); //This is necessary to add the SUT classes before instrumentation.
        dependencies.add(testBinFolder); //This is necessary to add the test classes before instrumentation.

        URL[] dependencyArray = dependencies.stream().map(file -> {
            URL url = null;
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException ignored) {
                LOGGER.warning("Cannot add the dependency to the classpath: "+file.toURI());
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
