package com.tblf.gitdiff;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class ParallelGitCaller extends GitCaller {

    private ConcurrentHashMap<String, Java2File> stringJava2FileMap;

    public ParallelGitCaller(File folder, ResourceSet resourceSet) {
        super(folder, resourceSet);

        stringJava2FileMap = new ConcurrentHashMap<>();

        resourceSet.getResources()
                .forEach(resource -> resource.getContents() //put all the resources java2files inside a concurrent map
                        .stream()
                        .filter(eObject -> eObject instanceof Java2File)
                        .map(eObject -> (Java2File) eObject)
                        .forEach(java2File -> stringJava2FileMap.put(java2File.getJavaUnit().getOriginalFilePath(), java2File)));

    }

}
