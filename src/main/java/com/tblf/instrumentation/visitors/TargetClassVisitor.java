package com.tblf.instrumentation.visitors;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetClassVisitor extends ClassVisitor {
    public TargetClassVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
        System.out.println("Visiting "+s);
        MethodVisitor methodVisitor = super.visitMethod(i, s, s1, s2, strings);
        return new TargetMethodVisitor(i, methodVisitor);
    }
}
