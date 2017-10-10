package com.tblf.instrumentation.bytecode;

import com.tblf.DotCP.DotCPParserBuilder;
import com.tblf.classLoading.InstURLClassLoader;
import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.instrumentation.InstrumentationUtils;
import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.bytecode.visitors.TargetClassVisitor;
import com.tblf.instrumentation.bytecode.visitors.TestClassVisitor;
import com.tblf.util.Configuration;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 20/09/2017.
 */
public class ByteCodeInstrumenter implements Instrumenter {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    /**
     * The folder containing the binaries to instrument
     */
    private File binFolder;

    /**
     * The directory containing the project
     */
    private File projectFolder;

    /**
     * The classLoader loading instrumented classes
     */
    private InstURLClassLoader instURLClassLoader = (InstURLClassLoader) SingleURLClassLoader.getInstance().getUrlClassLoader();

    public ByteCodeInstrumenter(File project) {
        this.projectFolder = project;
        this.binFolder = new File(project, Configuration.getProperty("binaries"));
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        int[] scores = new int[]{0, 0, 0, 0};
        try {
            getDependencies();
        } catch (Exception e) {
            LOGGER.warning("Couldn't load the dependencies");
        }

        targets.forEach(t -> {
            try {
                File target = InstrumentationUtils.getClassFile(binFolder, t);
                LOGGER.fine("Instrumenting class "+t+" of classFile "+target.toString());
                byte[] targetAsByte = instrumentTargetClass(target, t);
                ((InstURLClassLoader) SingleURLClassLoader.getInstance().getUrlClassLoader()).loadBytes(targetAsByte);
                scores[0]++;

            } catch (IOException | LinkageError e) {
                LOGGER.fine("Couldn't instrument "+t+" : "+e.getMessage());
                e.printStackTrace();
                scores[1]++;
            }
        });

        tests.forEach(t -> {
            try {
                File target = InstrumentationUtils.getClassFile(binFolder, t);
                byte[] targetAsByte = instrumentTestClass(target, t);
                ((InstURLClassLoader) SingleURLClassLoader.getInstance().getUrlClassLoader()).loadBytes(targetAsByte);
                scores[2]++;
            } catch (IOException | LinkageError e) {
                LOGGER.fine("Couldn't instrument "+t+" : "+e.getMessage());
                scores[3]++;
            }
        });

        LOGGER.info(scores[0]+" targets loaded "+ scores[1]+ " target fails "+ scores[2]+" test loaded "+scores[3]+" test fails ");
    }

    public File getBinFolder() {
        return binFolder;
    }

    public void setBinFolder(File binFolder) {
        this.binFolder = binFolder;
    }

    private byte[] instrumentTargetClass(File target, String qualifiedName) throws IOException {

        InputStream inputStream = new FileInputStream(target);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        try {
            classReader.accept(new TargetClassVisitor(Opcodes.ASM5, classWriter, qualifiedName), ClassReader.EXPAND_FRAMES);
        } catch (Throwable t) {

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
        List<File> dependencies = new DotCPParserBuilder().create().parse(dotCP);

        LOGGER.info("Adding the following dependencies to the classpath: "+dependencies.toString());

        dependencies.add(new File("libs/Link-1.0.0.jar"));
        dependencies.add(binFolder); //This is necessary to add the classes before instrumentation.

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
        SingleURLClassLoader.getInstance().addURLs(dependencyArray);
    }
}
