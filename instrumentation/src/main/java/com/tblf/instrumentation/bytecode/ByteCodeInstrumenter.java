package com.tblf.instrumentation.bytecode;

import com.tblf.DotCP.DotCPParserBuilder;
import com.tblf.classloading.SingleURLClassLoader;
import com.tblf.instrumentation.InstrumentationUtils;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.bytecode.visitors.TargetClassVisitor;
import com.tblf.instrumentation.bytecode.visitors.TestClassVisitor;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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


    public ByteCodeInstrumenter(File project) {
        this.projectFolder = project;
        this.testBinFolder = new File(project, Configuration.getProperty("testBinaries"));
        this.sutBinFolder = new File(project, Configuration.getProperty("sutBinaries"));
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
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
                scores[0]++;

            } catch (IOException | LinkageError e) {
                LOGGER.fine("Couldn't instrument "+t+" : "+e.getMessage());
                scores[1]++;
            }
        });

        tests.forEach(t -> {
            try {
                File target = InstrumentationUtils.getClassFile(testBinFolder, t);
                byte[] targetAsByte = instrumentTestClass(target, t);
                singleURLClassLoader.loadBytes(targetAsByte, t);
                scores[2]++;
            } catch (IOException | LinkageError e) {
                LOGGER.fine("Couldn't instrument "+t+" : "+e.getMessage());
                scores[3]++;
            }
        });

        LOGGER.info(scores[0]+" targets loaded "+ scores[1]+ " target fails "+ scores[2]+" test loaded "+scores[3]+" test fails ");
    }

    private byte[] instrumentTargetClass(File target, String qualifiedName) throws IOException {

        InputStream inputStream = new FileInputStream(target);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        try {
            classReader.accept(new TargetClassVisitor(Opcodes.ASM5, classWriter, qualifiedName), ClassReader.EXPAND_FRAMES);
        } catch (Throwable ignored) {

        }

        return classWriter.toByteArray();
    }

    private byte[] instrumentTestClass(File test, String qualifiedName) throws IOException {
        InputStream inputStream = new FileInputStream(test);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classReader.accept(new TestClassVisitor(Opcodes.ASM5, classWriter, qualifiedName), ClassReader.EXPAND_FRAMES);

        return classWriter.toByteArray();
    }

    private void getDependencies() throws ParserConfigurationException, SAXException, IOException {
        //Getting the dependencies from the .classpath file, assuming it is located in the same folder as the zip
        File dotCP = FileUtils.getFile(projectFolder, ".classpath");
        if (! dotCP.exists()) {
            throw new IOException("no .classpath file in the folder: load this project within an Eclipse application or run the goal 'mvn eclipse:eclipse'");
        }

        List<File> dependencies = new DotCPParserBuilder().create().parse(dotCP);

        LOGGER.info("Adding the following dependencies to the classpath: "+dependencies.toString());

        dependencies.add(new File("../instrumentation/src/main/resources/Link-1.0.0.jar"));
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
}
