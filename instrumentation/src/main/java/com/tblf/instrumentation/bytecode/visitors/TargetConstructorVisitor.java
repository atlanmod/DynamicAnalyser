package com.tblf.instrumentation.bytecode.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetConstructorVisitor extends TargetAbstractMethodVisitor {
    private int firstLine;
    private boolean firstLineHasBeenWritten;

    public TargetConstructorVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc, className);
        firstLine = 0;
        firstLineHasBeenWritten = false;
    }

    @Override
    public void visitLineNumber(int i, Label label) {
        if(firstLine != 0) {
            if (!firstLineHasBeenWritten) {
                trace(firstLine);
                firstLineHasBeenWritten = true;
            }

            trace(i);
        } else {
            firstLine = i;
        }
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

}
