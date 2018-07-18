package com.tblf.gitdiff;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DiffUtils {

    private static final Logger LOGGER = Logger.getLogger(DiffUtils.class.getName());

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
                return lines.findFirst().orElseThrow(() -> new IOException("Could not find a line at line "+line));
            } else {
                return lines.skip(line).findFirst().orElseThrow(() -> new IOException("Could not find a line at line "+line));
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
            LOGGER.warning("Couldn't find the file: "+f);
        }

        ObjectLoader objectLoader = repository.open(treeWalk.getObjectId(0));

        return new String(objectLoader.getBytes(), "UTF-8");
    }

    /**
     * Parse a statement to create a string to display out of it
     *
     * @param statement the {@link com.github.javaparser.ast.stmt.Statement}
     * @return a {@link String}
     */
    static String statementToString(com.github.javaparser.ast.stmt.Statement statement) {
        final String[] toString = {""};

        statement.getRange().ifPresent(range1 -> toString[0] =
                String.format(" %s : line %s, from %s, to %s",
                        statement,
                        range1.begin.line,
                        range1.begin.column,
                        range1.end.column));

        if ("".equals(toString[0])) {
            toString[0] = statement.toString();
        }

        return toString[0];
    }

    /**
     * Set the line number of every statement in a block.
     * Since we're parsing only a line of code, the line number will be 1 for all the statements in this line.
     * Instead, we set the correct line number according to the compilation unit, instead of the block itself.
     *
     * @param line      the line number
     * @param blockStmt a {@link BlockStmt} containing statements located on a single line of code
     */
    static void setLineNumberInBlockStatements(int line, BlockStmt blockStmt) {
        blockStmt.getStatements().forEach(statement -> {
            if (statement.getRange().isPresent()) {
                Position begin = new Position(line, statement.getRange().get().begin.column);
                Position end = new Position(line, statement.getRange().get().end.column);
                statement.setRange(new Range(begin, end));
            }
        });
    }
}
