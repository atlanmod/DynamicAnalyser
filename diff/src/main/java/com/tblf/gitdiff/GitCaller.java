package com.tblf.gitdiff;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class call basic git methods using the JGit library
 */
public class GitCaller extends VersionControlCaller {

    protected static final Logger LOGGER = Logger.getLogger("GitCaller");

    protected Repository repository;

    protected RevTree oldTree;
    protected RevTree newTree;

    protected DiffFormatter diffFormatter;

    /**
     * Constructor initializing the {@link Git}
     *
     * @param pomFolder   a {@link File} directory containing a pom.xml mvn file
     * @param resourceSet a {@link ResourceSet}
     */
    public GitCaller(File pomFolder, ResourceSet resourceSet) {
        super(pomFolder, resourceSet);
        try (Git git = Git.open(pomFolder)) {
            this.repository = git.getRepository();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load the git repository", e);
        }
    }

    /**
     * Constructor to use with multi-module projects
     *
     * @param pomFolder   the folder containing the pom
     * @param gitFolder   the folder containing the .git
     * @param resourceSet the {@link ResourceSet}
     */
    public GitCaller(File pomFolder, File gitFolder, ResourceSet resourceSet) {
        super(gitFolder, resourceSet);
        try (Git git = Git.open(gitFolder)) {
            this.repository = git.getRepository();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load the git repository", e);
        }
    }

    /**
     * Compare the statements between HEAD and a previous commit
     *
     * @param oldCommitID the ID of the previous commit, such as HEAD~4 for example
     */
    public void compareCommits(String oldCommitID) {
        compareCommits(oldCommitID, "HEAD");
    }

    /**
     * Compare two given commit ID
     *
     * @param currentCommitID the first commit ID
     * @param nextCommitID    the next commit ID
     */
    @Override
    public void compareCommits(String currentCommitID, String nextCommitID) {

        LOGGER.info("Comparing commits "+currentCommitID+" and "+nextCommitID);

        try {
            ObjectId current = repository.resolve(currentCommitID);
            ObjectId future = repository.resolve(nextCommitID);

            if (current == null || future == null) {
                throw new IOException("Cannot resolve the commits: " + current + " -> " + future);
            }

            oldTree = new RevWalk(repository).parseCommit(current).getTree();
            newTree = new RevWalk(repository).parseCommit(future).getTree();
            //diffFormatter = new DiffFormatter(new LogOutputStream(LOGGER, Level.FINE));
            diffFormatter = new DiffFormatter(System.out);
            diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);

            List<DiffEntry> diffEntryList = diffFormatter.scan(current, future);

            impactedTests = new GitDiffManager(this.repository.getWorkTree(), resourceSet, diffEntryList, diffFormatter).analyse();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the revision tree", e);
        }
    }
}