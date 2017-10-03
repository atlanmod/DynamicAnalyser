package com.tblf.instrumentation.bytecode.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetMethodVisitor extends AdviceAdapter {
    private String name;
    private String className;
    private int currentLine;

    public TargetMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc);
        this.name = name;
        this.className = className;
    }

    @Override
    protected void onMethodEnter() {
        traceEnter();
        super.onMethodEnter();
    }

    @Override
    public void visitLineNumber(int i, Label label) {
        currentLine = i;
        super.visitLineNumber(i, label);
    }

    @Override
    public void visitInsn(int i) {
        trace(currentLine);
        super.visitInsn(i);
    }

    @Override
    public void visitLabel(Label label) {
        //trace(labelIntegerMap.get(label));
        super.visitLabel(label);
    }

    private void trace(int line) {
        mv.visitLdcInsn(String.valueOf(line)); //put the method name in the stack
        mv.visitMethodInsn(INVOKESTATIC, "com/tblf/Link/Calls", "match", "(Ljava/lang/String;)V", false);
    }

    private void traceEnter() {
        mv.visitCode();

        mv.visitLdcInsn(className); //put the method class name in the stack
        mv.visitLdcInsn(name); //put the method name in the stack
        mv.visitMethodInsn(INVOKESTATIC, "com/tblf/Link/Calls", "setTargetMethod", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        mv.visitEnd();
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

}
