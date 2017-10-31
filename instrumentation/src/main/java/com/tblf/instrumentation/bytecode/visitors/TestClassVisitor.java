package com.tblf.instrumentation.bytecode.visitors;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by Thibault on 21/09/2017.
 */
public class TestClassVisitor extends ClassVisitor {
    private String name;

    public TestClassVisitor(int i) {
        super(i);
    }

    public TestClassVisitor(int i, ClassVisitor classVisitor, String name) {
        super(i, classVisitor);
        this.name = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        return new TestMethodVisitor(this.api, methodVisitor, access, name, desc, this.name);
    }

    public String getName() {
        return name;
    }
}
