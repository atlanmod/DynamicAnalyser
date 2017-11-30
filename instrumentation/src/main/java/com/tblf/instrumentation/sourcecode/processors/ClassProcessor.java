package com.tblf.instrumentation.sourcecode.processors;

import com.tblf.Link.Calls;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collection;
import java.util.Optional;

public class ClassProcessor extends AbstractProcessor<CtClass> {


    @Override
    public void process(CtClass ctClass) {
        CtTypeReference ctTypeReference = getFactory().Code().createCtTypeReference(Calls.class);

        Collection<CtImport> collection = ctClass.getPosition().getCompilationUnit().getImports();
        Optional<CtImport> optionalCtImport = collection.stream().filter(ctImport -> ctImport.getReference().equals(ctTypeReference)).findAny();

        if (!optionalCtImport.isPresent()) {
            CtImport ctImport = getFactory().Core().createImport();
            ctImport.setReference(ctTypeReference);
            collection.add(ctImport);
        }
    }
}
