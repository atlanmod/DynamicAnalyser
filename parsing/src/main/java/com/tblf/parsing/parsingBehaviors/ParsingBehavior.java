package com.tblf.parsing.parsingBehaviors;

import org.eclipse.emf.ecore.resource.ResourceSet;

public abstract class ParsingBehavior {

    private ResourceSet model;

    public ParsingBehavior(ResourceSet model) {
        this.model = model;
    }

    public abstract void manage(String trace);

    public ResourceSet getModel() {
        return model;
    }

    public void setModel(ResourceSet model) {
        this.model = model;
    }
}
