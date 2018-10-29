package com.tblf.business;

import com.tblf.instrumentation.InstrumentationType;
import com.tblf.parsing.TraceType;
import com.tblf.parsing.parsingBehaviors.EmptyParsingBehavior;
import com.tblf.utils.Configuration;
import com.tblf.utils.MavenUtils;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class AnalysisLauncherTest {

    /*
     SimpleProject execution output:
     Constructor1 --> TestApp
     line constructor1 --> TestApp
     method --> TestApp
     line 1 method --> TestApp
     line 2 method --> TestApp
     Constructor2 --> TestApp2
     line constructor2 --> TestApp2
     method --> TestApp2
     line 1 method --> TestApp2
     line 2 method --> TestApp2
     */

    @Before
    public void setUp() throws IOException {
        File file = new File("src/test/resources/fullprojects/SimpleProject");

        if (file.exists())
            FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkParseSCI() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        com.tblf.utils.FileUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setTraceType(TraceType.FILE);
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());
        analysisLauncher.run();

    }

    @Test
    public void checkParseSCIQueue() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        com.tblf.utils.FileUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setTraceType(TraceType.QUEUE);
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());
        analysisLauncher.run();

    }

    @Test
    public void checkParseBCI() throws IOException {

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        System.setProperty("maven.home", Configuration.getProperty("MAVEN_HOME"));

        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        com.tblf.utils.FileUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);
        analysisLauncher.setTraceType(TraceType.FILE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());
        analysisLauncher.applyBefore(MavenUtils::compilePom);
        analysisLauncher.run();

    }

    @Test
    public void checkBefore() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        com.tblf.utils.FileUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);

        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
        analysisLauncher.setTraceType(TraceType.FILE);
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());
        analysisLauncher.applyBefore(file1 -> Assert.assertFalse(new File(file1, "analysis.xmi").exists()));
        analysisLauncher.applyBefore(file1 -> {
            File file2 = new File(file1, "aRandomFileName.txt");
            try {
                Assert.assertTrue(file2.createNewFile());
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });

        analysisLauncher.applyBefore(file1 -> Assert.assertTrue(new File(file1, "aRandomFileName.txt").exists()));

        analysisLauncher.run();

        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkAfter() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        com.tblf.utils.FileUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);

        Configuration.setProperty("trace", "file");
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);
        analysisLauncher.setTraceType(TraceType.QUEUE);
        analysisLauncher.setOutputModel(new File(file, "analysis.xmi"));
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());
        analysisLauncher.applyAfter(file1 -> Assert.assertTrue(new File(file1, "trace").exists()));

        analysisLauncher.run();

        FileUtils.deleteDirectory(file);
    }



    @Test
    public void checkGenericInstrumentationSCI() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        com.tblf.utils.FileUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.SOURCECODE);

        File tmpFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".tmp");
        FileUtils.writeStringToFile(tmpFile, "0", Charset.defaultCharset());

        Integer value = Integer.valueOf(FileUtils.readFileToString(tmpFile, Charset.defaultCharset()));

        Assert.assertTrue("Wrong value in the file", value == 0);

        analysisLauncher.registerProcessor(new AbstractProcessor<CtClass>() {
            @Override
            public void process(CtClass ctClass) {
                ctClass.getMethods().forEach(o -> {
                    CtMethod ctMethod = (CtMethod) o;
                    CtCodeSnippetStatement ctCodeSnippetStatement = getFactory().Code().createCodeSnippetStatement(
                                    "{\n" +
                                    "try { \n" +
                                        "java.io.File myFile = new java.io.File(\""+tmpFile.getAbsolutePath()+"\");\n"+
                                        "Integer content = Integer.valueOf(new String(java.nio.file.Files.readAllBytes(myFile.toPath()), java.nio.charset.StandardCharsets.UTF_8));\n" +
                                        "content = content + 1;\n" +
                                        "java.nio.file.Files.write(myFile.toPath(), content.toString().getBytes());\n"+
                                    "} catch (java.io.IOException e) { \n" +
                                    "} \n" +
                                    "}");

                    ctMethod.getBody().insertBegin(ctCodeSnippetStatement);
                } );
            }
        });
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());

        analysisLauncher.run();
        value = Integer.valueOf(FileUtils.readFileToString(tmpFile, Charset.defaultCharset()));
        Assert.assertTrue("Wrong value returned: "+value, value == 4);
        FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkGenericInstrumentationBCI() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        com.tblf.utils.FileUtils.unzip(zip);

        File file = new File("src/test/resources/fullprojects/SimpleProject");

        if (System.getProperty("maven.home") == null)
            System.setProperty("maven.home", Configuration.getProperty("MAVEN_HOME"));

        MavenUtils.compilePom(file);

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5) {
            protected String className;
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor methodVisitor =  super.visitMethod(access, name, desc, signature, exceptions);
                return new AdviceAdapter(this.api, methodVisitor, access, name, desc) {
                    @Override
                    protected void onMethodEnter() {
                        super.onMethodEnter();
                        mv.visitCode();
                        mv.visitTypeInsn(Opcodes.NEW, "java/io/File");
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitLdcInsn("/tmp/oracleFile.test");
                        mv.visitFieldInsn(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V");
                        mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/File", "createNewFile", "()Z");
                    }
                };
            }
        };

        analysisLauncher.registerProcessor(classVisitor);
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());

        File oracle = new File("/tmp/oracleFile.test");
        if (oracle.exists())
            oracle.delete();

        Assert.assertFalse(oracle.exists());

        analysisLauncher.run();

        Assert.assertTrue(oracle.exists());

        FileUtils.deleteDirectory(file);
    }


}

