package com.tblf.instrumentation.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetMethodVisitor extends MethodVisitor{
    public TargetMethodVisitor(int i, MethodVisitor methodVisitor) {
        super(i, methodVisitor);
    }

    @Override
    public void visitLineNumber(int i, Label label) {
        System.out.println("Visiting line "+i+" "+label);
        visitLabel(label);
        super.visitLineNumber(i, label);
    }

}
