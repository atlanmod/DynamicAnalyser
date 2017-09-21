package com.tblf.instrumentation.visitors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Thibault on 21/09/2017.
 */
public class TestMethodVisitor extends AdviceAdapter {

    String name;

    protected TestMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
        this.name = name;
        trace();
    }

    private void trace() {
        mv.visitCode();

        //TODO
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(name + " ");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);


        mv.visitEnd();
    }
}
