package com.tblf.instrumentation;

import com.tblf.classLoading.InstURLClassLoader;
import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.instrumentation.visitors.TargetClassVisitor;
import com.tblf.instrumentation.visitors.TestClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Logger;

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
     * The classLoader loading instrumented classes
     */
    private InstURLClassLoader instURLClassLoader = (InstURLClassLoader) SingleURLClassLoader.getInstance().getUrlClassLoader();

    public ByteCodeInstrumenter() {
        binFolder = new File(".");
    }

    public ByteCodeInstrumenter(File binFolder) {
        this.binFolder = binFolder;
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        int[] scores = new int[]{0, 0, 0, 0};

        targets.forEach(t -> {
            try {
                File target = InstrumentationUtils.getClassFile(binFolder, t);
                LOGGER.fine("Instrumenting class "+t+" of classFile "+target.toString());
                byte[] targetAsByte = instrumentTargetClass(target, t);
                ((InstURLClassLoader) SingleURLClassLoader.getInstance().getUrlClassLoader()).loadBytes(targetAsByte);
                scores[0]++;

            } catch (IOException | LinkageError e) {
                LOGGER.fine("Couldn't instrument "+t+" : "+e.getMessage());
                scores[1]++;
            }
        });

        tests.forEach(t -> {
            File target = null;
            try {
                target = InstrumentationUtils.getClassFile(binFolder, t);
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
}
