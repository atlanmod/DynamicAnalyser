package com.tblf.gitdiff;

import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.util.Collection;

public abstract class VersionControlCaller {
    //TODO

    protected File folder;
    protected ResourceSet resourceSet;
    protected Collection<String> testsToRun;

    public VersionControlCaller(File folder, ResourceSet resourceSet) {
        this.folder = folder;
        this.resourceSet = resourceSet;
    }

    public abstract void compareCommits(String oldId, String newId);

    public Collection<String> getTestsToRun() {
        return testsToRun;
    }
}
