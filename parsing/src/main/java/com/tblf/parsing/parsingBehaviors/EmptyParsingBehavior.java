package com.tblf.parsing.parsingBehaviors;

import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class EmptyParsingBehavior extends ParsingBehavior{

    public EmptyParsingBehavior() {
        super(new ResourceSetImpl());
    }

    @Override
    public void manage(String trace) {

    }
}
