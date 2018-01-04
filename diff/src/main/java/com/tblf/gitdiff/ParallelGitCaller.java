package com.tblf.gitdiff;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParallelGitCaller extends GitCaller {

    private ConcurrentHashMap<String, Java2File> stringJava2FileMap;

    public ParallelGitCaller(File folder, ResourceSet resourceSet) {
        super(folder, resourceSet);

        System.out.println(folder.getAbsolutePath());
        stringJava2FileMap = new ConcurrentHashMap<>();
        resourceSet.getResources()
                .forEach(resource -> resource.getContents() //put all the resources java2files inside a concurrent map
                        .stream()
                        .filter(eObject -> eObject instanceof Java2File)
                        .map(eObject -> (Java2File) eObject)
                        .forEach(java2File -> stringJava2FileMap.put(java2File.getJavaUnit().getOriginalFilePath().replace(folder.getAbsolutePath()+"/", ""), java2File)));
    }

    @Override
    protected Collection<Map.Entry<String, String>> analyseDiffs(List<DiffEntry> diffEntries) {
        diffEntries.parallelStream().forEach(diffEntry -> {
            try {

                FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);

                String uri = fileHeader.getOldPath();

                if (!uri.endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: "+uri+" does not concern a Java file");

                LOGGER.fine("Analyzing impacts of " + uri + " modification");

                Java2File java2File = stringJava2FileMap.get(uri);

                if (java2File == null)
                    throw new FileNotFoundException("Could not found file "+uri+" in the model");

                diffFormatter.format(diffEntry);

                fileHeader.toEditList().forEach(edit -> {
                    manageEdit(diffEntry, edit, java2File);
                });

            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        return null;
    }
}
