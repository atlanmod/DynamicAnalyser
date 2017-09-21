package com.tblf.instrumentation;
import com.tblf.classLoading.InstURLClassLoader;
import com.tblf.classLoading.SingleURLClassLoader;
import com.tblf.instrumentation.visitors.TargetClassVisitor;
import com.tblf.instrumentation.visitors.TestClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.util.List;

/**
 * Created by Thibault on 20/09/2017.
 */
public class ByteCodeInstrumenter implements Instrumenter {

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
    public void instrument(List<String> targets, List<String> tests) {
        targets.forEach(t -> {
            try {
                File target = InstrumentationUtils.getClassFile(binFolder, t);
                byte[] targetAsByte = instrumentTargetClass(target);
                instURLClassLoader.loadBytes(targetAsByte, t);
            } catch (IOException e) {
                e.printStackTrace();
                //TODO log
            }
        });

        tests.forEach(t -> {
            File target = null;
            try {
                target = InstrumentationUtils.getClassFile(binFolder, t);
                byte[] targetAsByte = instrumentTestClass(target);
                instURLClassLoader.loadBytes(targetAsByte, t);
            } catch (IOException e) {
                e.printStackTrace();
                //TODO log
            }
        });
    }

    public File getBinFolder() {
        return binFolder;
    }

    public void setBinFolder(File binFolder) {
        this.binFolder = binFolder;
    }

    private byte[] instrumentTargetClass(File target) throws IOException {

        InputStream inputStream = new FileInputStream(target);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classReader.accept(new TargetClassVisitor(Opcodes.ASM5, classWriter), ClassReader.EXPAND_FRAMES);


        return classWriter.toByteArray();
    }

    private byte[] instrumentTestClass(File test) throws IOException {
        InputStream inputStream = new FileInputStream(test);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classReader.accept(new TestClassVisitor(Opcodes.ASM5, classWriter), ClassReader.EXPAND_FRAMES);


        return classWriter.toByteArray();
    }
}
