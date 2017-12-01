package com.tblf.instrumentation.bytecode.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetMethodVisitor extends TargetAbstractMethodVisitor {

    public TargetMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc, className);
    }

    @Override
    public void visitLineNumber(int i, Label label) {
        trace(i);
        super.visitLineNumber(i, label);
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

}
