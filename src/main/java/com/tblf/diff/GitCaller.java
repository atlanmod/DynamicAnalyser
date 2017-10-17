package com.tblf.diff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class call basic git methods using the JGit library
 */
public class GitCaller {

    private static final Logger LOGGER = Logger.getLogger("GitCaller");

    private Repository repository;
    private DiffFormatter diffFormatter;

    private RevTree oldTree;
    private RevTree newTree;

    private ResourceSet resourceSet;

    /**
     * Constructor initializing the {@link Git}
     *
     * @param pomFolder
     */
    public GitCaller(File pomFolder, ResourceSet set) {
        try {
            Git git = Git.open(pomFolder);
            repository = git.getRepository();
            resourceSet = set;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compare the statements between two commits
     *
     * @param oldCommitID the ID of the previous commit, such as HEAD~4 for example
     */
    public void compareCommits(String oldCommitID) {
        try {
            ObjectId previous = repository.resolve(oldCommitID);
            ObjectId current = repository.resolve("HEAD");

            if (previous == null || current == null) {
                throw new IOException("Cannot resolve the commits");
            }

            oldTree = new RevWalk(repository).parseCommit(previous).getTree();
            newTree = new RevWalk(repository).parseCommit(current).getTree();
            diffFormatter = new DiffFormatter(new LogOutputStream(LOGGER, Level.FINE));
            diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);

            List<DiffEntry> diffEntryList = diffFormatter.scan(previous, current);
            analyseDiffs(diffEntryList);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the revision tree", e);
        }
    }

    /**
     * Iterate over a {@link DiffEntry} list and, get the impacted test cases out of it
     * @param diffEntries
     */
    private void analyseDiffs(List<DiffEntry> diffEntries) {
        diffEntries.forEach(diffEntry -> {
            try {
                FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);
                diffFormatter.format(diffEntry);
                fileHeader.toEditList().forEach(edit -> {

                    switch (edit.getType()) {
                        case INSERT:
                            manageInsert(diffEntry, edit);
                            break;
                        case DELETE:
                            manageDelete(diffEntry, edit);
                            break;
                        case REPLACE:
                            manageReplace(diffEntry, edit);
                            break;
                        default:
                            //TODO
                            break;
                    }
                });
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Couldn't analyze the diffEntry", e);
            }
        });
    }


    /**
     * Compute the statements modified by the commit
     * @param diffEntry a {@link DiffEntry}
     * @param edit an {@link Edit}
     */
    private void manageReplace(DiffEntry diffEntry, Edit edit) {

        BlockStmt blockStmtBefore = getStatementsFromOldPath(diffEntry, edit);
        BlockStmt blockStmtAfter = getStatementsFromNewPath(diffEntry, edit);

        LOGGER.fine(blockStmtBefore.getStatements().size()+ " statements before and "+blockStmtAfter.getStatements().size()+" after. Now comparing.");

        blockStmtBefore.getStatements().forEach(statement -> {
            if (! blockStmtAfter.getStatements().contains(statement) && statement.getRange().isPresent()) {
                LOGGER.info(statement+
                        " file: "+
                        diffEntry.getOldPath()+
                        " line: "+statement.getRange().get().begin.line+
                        " from "+statement.getRange().get().begin.column+
                        " to "+statement.getRange().get().end.column+
                        " is changed");
            }
        });

        blockStmtAfter.getStatements().forEach(statement -> {
            if (! blockStmtBefore.getStatements().contains(statement) && statement.getRange().isPresent()) {
                LOGGER.info(statement+
                        " file: "+
                        diffEntry.getNewPath()+
                        " line: "+statement.getRange().get().begin.line+
                        " from "+statement.getRange().get().begin.column+
                        " to "+statement.getRange().get().end.column+
                        " is new");
            }
        });

    }


    /**
     * Iterate over the statements deleted in the old compilation unit and get the coordinates, in order to find them in the impact analysis model
     * @param diffEntry the {@link DiffEntry}
     * @param edit the {@link Edit}
     *             @TODO Add a return value
     */
    private void manageDelete(DiffEntry diffEntry, Edit edit) {
        try {
            //Using an AtomicInteger for the For loop
            for (AtomicInteger line = new AtomicInteger(edit.getBeginA()); line.get() <= edit.getEndA(); line.incrementAndGet()) {
                //Getting the deleted line in the old version of the file
                String sourceLine = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, oldTree, diffEntry.getOldPath()), line.get());

                //Computing the statements present in the file
                BlockStmt blockStmt = JavaParser.parseBlock("{" + sourceLine + "}");

                //Getting the coordinates of each statements
                blockStmt.getStatements().forEach(statement -> System.out.println("Statement line " + line.get() + " from " + statement.getBegin().get().column + " to " + statement.getEnd().get().column + " deleted"));

                //FIXME: Return the values
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't get the deleted statements in file "+diffEntry.getOldPath(), e);
        }
    }

    /**
     * Iterate over the new statements in the new compilation unit and find their coordinates.
     * The goal would be to add those in the model without having to re-instrument everything
     * @param diffEntry a {@link DiffEntry}
     * @param edit an {@link Edit}
     */
    private void manageInsert(DiffEntry diffEntry, Edit edit) {
        try {
            //Uses AtomicInteger for the for loop, because it's final (and not int)
            for (AtomicInteger line = new AtomicInteger(edit.getBeginA()); line.get() <= edit.getEndA(); line.incrementAndGet()) {
                //getting the line in the new compilation unit
                String sourceLine = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, newTree, diffEntry.getNewPath()), edit.getBeginB());

                //computing the statements present in that line
                BlockStmt blockStmt = JavaParser.parseBlock("{" + sourceLine + "}");

                //getting the coordinates of each new statement
                blockStmt.getStatements().forEach(statement -> System.out.println("Statement line " + line.get() + " from " + statement.getBegin().get().column + " to " + statement.getEnd().get().column + " added"));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't get the new statements in file "+diffEntry.getNewPath(), e);
        }
    }

    /**
     * Parse a file to gather all the statements from a specific {@link DiffEntry}, form the old state of the code
     * @param diffEntry a {@link DiffEntry}
     * @param edit an {@link Edit}
     * @return a {@link BlockStmt}
     */
    private BlockStmt getStatementsFromOldPath(DiffEntry diffEntry, Edit edit) {
        BlockStmt blockStmtBefore = new BlockStmt();
        for (AtomicInteger i = new AtomicInteger(edit.getBeginA()); i.get() < edit.getEndA(); i.incrementAndGet()) {

            String line = null;
            try {
                line = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, oldTree, diffEntry.getOldPath()), i.get());
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't get the statements of the following line: "+line, e);
            }

            try {
                BlockStmt blockStmt = JavaParser.parseBlock("{"+line+"}");

                blockStmt.getStatements().forEach(statement -> {
                    Position begin = new Position(i.get(), statement.getBegin().get().column);
                    Position end = new Position(i.get(), statement.getEnd().get().column);
                    statement.setRange(new Range(begin, end));
                });

                blockStmtBefore.getStatements().addAll(blockStmt.getStatements());
            } catch (ParseProblemException e) {
                LOGGER.log(Level.FINE, "Couldn't get the statements of the following line: "+line, e);
            }
        }

        return blockStmtBefore;
    }

    /**
     * Parse a file to gather all the statements from a specific {@link DiffEntry}, form the new state of the code
     * @param diffEntry a {@link DiffEntry}
     * @param edit an {@link Edit}
     * @return a {@link BlockStmt} containing the statements
     */
    private BlockStmt getStatementsFromNewPath(DiffEntry diffEntry, Edit edit) {
        BlockStmt blockStmtAfter = new BlockStmt();
        for (AtomicInteger i = new AtomicInteger(edit.getBeginB()); i.get() < edit.getEndB(); i.incrementAndGet()) {
            String line = null;

            try {
                line = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, newTree, diffEntry.getNewPath()), i.get());
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't get the following line: "+i, e);
            }

            try {
                BlockStmt blockStmt = JavaParser.parseBlock("{"+line+"}");

                blockStmt.getStatements().forEach(statement -> {
                    Position begin = new Position(i.get(), statement.getBegin().get().column);
                    Position end = new Position(i.get(), statement.getEnd().get().column);
                    statement.setRange(new Range(begin, end));
                });

                blockStmtAfter.getStatements().addAll(blockStmt.getStatements());
            } catch (ParseProblemException e) {
                LOGGER.log(Level.FINE, "Couldn't get the statements of the following line: "+line, e);
            }
        }

        return blockStmtAfter;
    }
}