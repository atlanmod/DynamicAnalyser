package com.tblf.instrumentation.bytecode.visitors;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Thibault on 21/09/2017.
 */
public class TestMethodVisitor extends AdviceAdapter {

    private String className;
    private String name;

    protected TestMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc);
        this.name = name;
        this.className = className;
    }

    private void traceEnter() {
        mv.visitCode();
        //TODO

        mv.visitLdcInsn(className); //put the method class name in the stack
        mv.visitLdcInsn(name); //put the method name in the stack
        mv.visitMethodInsn(INVOKESTATIC, "com/tblf/Link/Calls", "setTestMethod", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        mv.visitEnd();
    }

    @Override
    protected void onMethodEnter() {
        traceEnter();
        super.onMethodEnter();
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute);
    }
}
