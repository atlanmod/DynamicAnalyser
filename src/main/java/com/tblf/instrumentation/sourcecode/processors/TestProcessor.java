package com.tblf.instrumentation.sourcecode.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtType;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class TestProcessor extends AbstractProcessor<CtType<?>> {
    private static final Logger LOGGER = Logger.getLogger("Processor");
    private Collection<String> tests;

    /**
     * Constructor
     * @param tests
     */
    public TestProcessor(Collection<String> tests) {
        super();
        this.tests = tests;
    }

    @Override
    public void process(CtType<?> ctType) {

        if (tests.contains(ctType.getQualifiedName())) {
            //test behaviour
            instrumentTest(ctType);
        } else {
            //target behaviour
            instrumentTarget(ctType);
        }
    }

    /**
     * Instrument a test class by adding a method call at the beginning of every method having a body
     * @param ctType
     */
    private void instrumentTest(CtType<?> ctType) {
        LOGGER.fine("Instrumenting the test class: "+ctType.getQualifiedName());
        ctType.getMethods().forEach(ctMethod -> {
            CtBlock ctBlock = ctMethod.getBody();
            if (ctBlock != null) {
                System.out.println("Instrumenting the test");
                CtCodeSnippetStatement ctCodeSnippetStatement = getFactory().Core().createCodeSnippetStatement();
                ctCodeSnippetStatement.setValue(String.format("com.tblf.Link.Calls.setTestMethod(\"%s\", \"%s\")", ctType.getQualifiedName(), ctMethod.getSimpleName()));
                ctBlock.insertBegin(ctCodeSnippetStatement);
            }
        });
    }

    /**
     * Add the right method at the beginning of the SUT method in order to build the trace during the execution
     * @param ctType
     */
    private void instrumentTarget(CtType<?> ctType) {
        LOGGER.fine("Instrumenting the SUT class: "+ctType.getQualifiedName());
        ctType.getMethods().forEach(ctMethod -> {
            CtBlock ctBlock = ctMethod.getBody();
            if (ctBlock != null) {
                System.out.println("Instrumenting the target");
                CtCodeSnippetStatement ctCodeSnippetStatement = getFactory().Core().createCodeSnippetStatement();
                ctCodeSnippetStatement.setValue(String.format("com.tblf.Link.Calls.setTargetMethod(\"%s\", \"%s\")", ctType.getQualifiedName(), ctMethod.getSimpleName()));
                ctBlock.insertBegin(ctCodeSnippetStatement);
            }
        });
    }


}
