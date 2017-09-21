package com.tblf.instrumentation.visitors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Thibault on 21/09/2017.
 */
public class TestMethodVisitor extends AdviceAdapter {
    protected TestMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    @Override
    public void visitCode() {
        System.out.println("Visiting the code of the method");

        //TODO
        super.visitCode();
    }
}
