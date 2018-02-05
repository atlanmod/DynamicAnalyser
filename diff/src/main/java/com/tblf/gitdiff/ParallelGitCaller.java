package com.tblf.gitdiff;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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

    //TODO: wont work if the models have been generated on a different machine
    @Override
    protected void analyseDiffs(List<DiffEntry> diffEntries) {

        diffEntries.parallelStream().forEach(diffEntry -> {
            try {

                FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);

                String uri = fileHeader.getOldPath();

                if (!uri.endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: "+uri+" does not concern a Java file");

                Java2File java2File = stringJava2FileMap.get(folder.getAbsolutePath()+"/"+uri);

                if (java2File == null)
                    throw new FileNotFoundException("Could not find file "+uri+" in the model");

                diffFormatter.format(diffEntry);

                fileHeader.toEditList().forEach(edit -> manageEdit(diffEntry, edit, java2File));

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "An error was caught when analysis the diffentries", e);
            }

        });
    }
}
