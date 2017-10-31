package com.tblf.instrumentation.bytecode.visitors;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetClassVisitor extends ClassVisitor {
    private String name;

    public TargetClassVisitor(int i, ClassVisitor classVisitor, String name) {
        super(i, classVisitor);
        this.name = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if ("<init>".equals(name)) {
            return methodVisitor;
        } else {
            return new TargetMethodVisitor(this.api, methodVisitor, access, name, desc, this.name);
        }
    }
}
