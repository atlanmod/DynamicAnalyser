package com.tblf.instrumentation.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetMethodVisitor extends AdviceAdapter {

    public TargetMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    @Override
    public void visitLineNumber(int i, Label label) {
        System.out.println("Visiting line "+i+" "+label.getOffset());
        trace(i);
        super.visitLineNumber(i, label);
    }

    private void trace(int line) {
        mv.visitCode();

        //TODO
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("------" + line + " " + "executed");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);


        mv.visitEnd();
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitLabel(Label label) {

        super.visitLabel(label);
    }
}
