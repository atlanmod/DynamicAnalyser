package com.tblf.instrumentation.bytecode.visitors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

public abstract class TargetAbstractMethodVisitor extends AdviceAdapter {
    protected String name;
    protected String className;

    /**
     * Creates a new {@link AdviceAdapter}.
     *
     * @param api    the ASM API version implemented by this visitor. Must be one
     *               of {@link org.objectweb.asm.Opcodes}
     * @param mv     the method visitor to which this adapter delegates calls.
     * @param access the method's access flags (see {@link org.objectweb.asm.Opcodes}).
     * @param name   the method's name.
     * @param desc   the method's descriptor (see {@link org.objectweb.asm.Type Type}).
     * @param className the class name
     */
    public TargetAbstractMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc);
        this.name = name;
        this.className = className;
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        traceEnter();
    }

    protected void traceEnter() {
        mv.visitCode();
        mv.visitLdcInsn(className); //put the method class name in the stack
        mv.visitLdcInsn(name); //put the method name in the stack
        mv.visitMethodInsn(INVOKESTATIC, "com/tblf/linker/Calls", "setTargetMethod", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        mv.visitEnd();
    }

    protected void trace(int line) {
        mv.visitLdcInsn(String.valueOf(line)); //put the method name in the stack
        mv.visitMethodInsn(INVOKESTATIC, "com/tblf/linker/Calls", "match", "(Ljava/lang/String;)V", false);
    }
}
