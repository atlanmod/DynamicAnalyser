package com.tblf.instrumentation.sourcecode.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;

public class TestProcessor extends AbstractProcessor<CtType<?>> {

    @Override
    public void process(CtType<?> ctType) {
        ctType.getMethods().forEach(ctMethod -> {
            CtBlock ctBlock = ctMethod.getBody();
            if (ctBlock != null) {
                //The block exist, we're not instrumenting an interface
                String value = String.format("\"%s - %s\"", ctMethod.getParent(CtClass.class).getQualifiedName(), ctMethod.getSimpleName());
                String toAdd = "System.out.println("+value+")";
                CtCodeSnippetStatement ctCodeSnippetStatement = getFactory().Core().createCodeSnippetStatement();
                ctCodeSnippetStatement.setValue(toAdd);
                ctBlock.insertBegin(ctCodeSnippetStatement);
            }
        });
    }
}
