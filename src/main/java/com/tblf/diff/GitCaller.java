package com.tblf.diff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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

    /**
     * Constructor initializing the {@link Git}
     *
     * @param pomFolder
     */
    public GitCaller(File pomFolder) {
        try {
            Git git = Git.open(pomFolder);
            repository = git.getRepository();
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

    private void manageReplace(DiffEntry diffEntry, Edit edit) {
        int before=  edit.getEndA() - edit.getBeginA();
        int after =  edit.getEndB() - edit.getBeginB();

        if (before > after) {
            //lines updated AND deleted
            LOGGER.fine("lines updated and deleted. Computing the statement diff");

        }

        if (after > before) {
            //lines updated AND added
            LOGGER.fine("lines updated and added. Computing the statement diff");
        }

        if (after == before) {
            //lines updated
            LOGGER.fine("lines updated. Computing the statement diff");

            compareEditsSameSize(diffEntry, edit);
        }
    }

    private void compareEditsSameSize(DiffEntry diffEntry, Edit edit) {
        for (AtomicInteger line = new AtomicInteger(edit.getBeginA()); line.get() <= edit.getEndA(); line.incrementAndGet()) {

            String sourceLine = null;
            String targetLine = null;
            try {
                sourceLine = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, oldTree, diffEntry.getOldPath()), line.get());
                targetLine = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, newTree, diffEntry.getNewPath()), edit.getBeginB());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Couldn't get the line from the old file", e);
            }

            //computing the statements present in that line
            BlockStmt blockStmtSrc = JavaParser.parseBlock("{" + sourceLine + "}");
            BlockStmt blockStmtTrgt = JavaParser.parseBlock("{" + targetLine + "}");

        }
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

}
