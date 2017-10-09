package com.tblf.instrumentation.sourcecode.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtStatement;

public class TargetProcessor extends AbstractProcessor<CtStatement> {
    @Override
    public void process(CtStatement ctStatement) {

            if (!ctStatement.isImplicit() && ! (ctStatement instanceof CtBlock) && !(ctStatement instanceof CtCodeSnippetStatement)) {
                //The ctStatement is not a super(). we can insert an other just before it
                String value = String.format("\"%s - %s\"", ctStatement.getPosition().getSourceStart(), ctStatement.getPosition().getSourceEnd());
                String toAdd = "System.out.println(" + value + ")";
                CtCodeSnippetStatement ctCodeSnippetStatement = getFactory().Core().createCodeSnippetStatement();
                ctCodeSnippetStatement.setValue(toAdd);
                try {
                    ctStatement.insertBefore(ctCodeSnippetStatement);
                } catch (Exception e) {
                    System.out.println("Couldn't instrument statement: "+ctStatement);
                    e.printStackTrace();
                }
            }
    }
}
