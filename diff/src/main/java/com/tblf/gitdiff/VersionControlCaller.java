package com.tblf.gitdiff;

import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

public abstract class VersionControlCaller {
    //TODO

    protected File folder;
    protected ResourceSet resourceSet;
    protected Collection<String> newTests;
    protected Collection<String> impactedTests;

    public VersionControlCaller(File folder, ResourceSet resourceSet) {
        this.folder = folder;
        this.resourceSet = resourceSet;

        newTests = new HashSet<>();
        impactedTests = new HashSet<>();
    }

    public abstract void compareCommits(String oldId, String newId);

    public Collection<String> getNewTests() {
        return newTests;
    }

    public Collection<String> getImpactedTests() {
        return impactedTests;
    }
}
