package com.tblf.diff;

import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Stream;

public class DiffUtils {

    /**
     * Parse a file line by line until getting to the expected line
     *
     * @param fileAsString the file, as a String
     * @param line         the line number wnated
     * @return the line as a {@link String}
     * @throws IOException if the string can't be parsed
     */
    static String getLineFromFile(String fileAsString, int line) throws IOException {
        try (Stream<String> lines = new BufferedReader(new StringReader(fileAsString)).lines()) {
            if (line == 0) {
                return lines.findFirst().get();
            } else {
                return lines.skip(line).findFirst().get();
            }
        }
    }

    /**
     * Return the content of a file form a previous commit
     * @param repository the git {@link Repository}
     * @param rev a {@link RevTree}
     * @param f the Filename as a {@link String}
     * @return the content of the file, as a {@link String}
     * @throws IOException if the File cant be found
     */
    static String getFileContentFromCommit(Repository repository, RevTree rev, String f) throws IOException {
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(rev);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(f));

        if (!treeWalk.next()) {
            System.out.println("Couldn't find the file");
        }

        ObjectLoader objectLoader = repository.open(treeWalk.getObjectId(0));

        return new String(objectLoader.getBytes(), "UTF-8");
    }
}
