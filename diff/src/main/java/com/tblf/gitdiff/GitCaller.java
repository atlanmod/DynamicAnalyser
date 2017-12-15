package com.tblf.gitdiff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.tblf.model.Analysis;
import com.tblf.utils.Configuration;
import com.tblf.utils.ModelUtils;
import com.tblf.utils.ParserUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class call basic git methods using the JGit library
 */
public class GitCaller {

    private static final Logger LOGGER = Logger.getLogger("GitCaller");

    private File pomFolder;

    private Repository repository;
    private DiffFormatter diffFormatter;

    private RevTree oldTree;
    private RevTree newTree;

    private ResourceSet resourceSet;
    private Java2File java2File;

    private Set<MethodDeclaration> testToRun;

    /**
     * Constructor initializing the {@link Git}
     *
     * @param pomFolder a {@link File} directory containing a pom.xml mvn file
     * @param resourceSet a {@link ResourceSet}
     */
    public GitCaller(File pomFolder, ResourceSet resourceSet) {
        try (Git git = Git.open(pomFolder)) {
            this.repository = git.getRepository();
            this.resourceSet = resourceSet;
            this.pomFolder = pomFolder;
        } catch (IOException e) {
            throw new RuntimeException("Cannot load the git repository", e);
        }
    }

    /**
     * Constructor to use with multi-module projects
     * @param pomFolder the folder containing the pom
     * @param gitFolder the folder containing the .git
     * @param resourceSet the {@link ResourceSet}
     */
    public GitCaller(File pomFolder, File gitFolder, ResourceSet resourceSet) {
        try (Git git = Git.open(gitFolder)) {
            this.repository = git.getRepository();
            this.resourceSet = resourceSet;
            this.pomFolder = pomFolder;
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
    public void compareCommits(String currentCommitID, String nextCommitID) {
        try {
            ObjectId current = repository.resolve(currentCommitID);
            ObjectId future = repository.resolve(nextCommitID);

            if (current == null || future == null) {
                throw new IOException("Cannot resolve the commits: "+current+" -> "+future);
            }

            oldTree = new RevWalk(repository).parseCommit(current).getTree();
            newTree = new RevWalk(repository).parseCommit(future).getTree();
            diffFormatter = new DiffFormatter(new LogOutputStream(LOGGER, Level.FINE));
            diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);
            testToRun = new HashSet<>();

            List<DiffEntry> diffEntryList = diffFormatter.scan(current, future);
            analyseDiffs(diffEntryList);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the revision tree", e);
        }
    }

    /**
     * Iterate over a {@link DiffEntry} list and, get the impacted test cases out of it
     *
     * @param diffEntries a {@link List} of {@link DiffEntry}
     */
    private Collection<Map.Entry<String, String>> analyseDiffs(List<DiffEntry> diffEntries) {

        diffEntries.forEach(diffEntry -> {
            try {
                FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);

                String pkg;
                String uri = diffEntry.getOldPath();

                if (! uri.endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: "+uri+" does not concern a Java file");

                LOGGER.fine("Analyzing impacts of "+uri+" modification");

                pkg = ParserUtils.getPackageQNFromFile(new File(uri));

                if (java2File == null) {
                    Resource sutPackage = ModelUtils.getPackageResource(pkg, resourceSet);
                    java2File = (Java2File) sutPackage.getContents()
                            .stream()
                            .filter(eObject -> eObject instanceof Java2File
                                    && ((Java2File) eObject).getJavaUnit().getOriginalFilePath().endsWith(fileHeader.getOldPath()))
                            .findFirst()
                            .orElseThrow(() -> new NonJavaFileException("The DiffEntry does not concern a Java file but: " + fileHeader.getOldPath() + " No impact computed from it"));
                }

                diffFormatter.format(diffEntry);
                fileHeader.toEditList().forEach(edit -> manageEdit(diffEntry, edit));

            } catch (NonJavaFileException e) {
                LOGGER.log(Level.FINE, e.toString());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Couldn't analyze the diffEntry", e);
            }
        });

        LOGGER.info("Impact analysis completed");
        testToRun.forEach(methodDeclaration -> LOGGER.info("The test method: " + methodDeclaration.getName() + " of the test class " + ((ClassDeclaration) methodDeclaration.eContainer()).getName() + " is impacted by this modification"));
        return testToRun.stream().map(methodDeclaration -> new AbstractMap.SimpleEntry<>(methodDeclaration.getName(), ((ClassDeclaration) methodDeclaration.eContainer()).getName())).collect(Collectors.toList());
    }

    /**
     * Compute the statements modified by the commit
     *
     * @param diffEntry a {@link DiffEntry}
     * @param edit      an {@link Edit}
     */
    private void manageEdit(DiffEntry diffEntry, Edit edit) {

        BlockStmt blockStmtBefore = getStatementsFromOldPath(diffEntry, edit);
        BlockStmt blockStmtAfter = getStatementsFromNewPath(diffEntry, edit);

        LOGGER.fine(blockStmtBefore.getStatements().size() + " statements before and " + blockStmtAfter.getStatements().size() + " after. Now comparing.");

        //Statement modified or removed
        blockStmtBefore.getStatements().forEach(statement -> {
            if (!blockStmtAfter.getStatements().contains(statement) && statement.getRange().isPresent()) {
                LOGGER.fine("In file " + diffEntry.getOldPath() + statementToString(statement) + " modified.");
                //Get the impacts at the statement level
                testToRun.addAll(getImpacts(java2File, statement));
            }
        });

        //New statements
        blockStmtAfter.getStatements().forEach(statement -> {
            if (!blockStmtBefore.getStatements().contains(statement) && statement.getRange().isPresent()) {
                LOGGER.fine("In file " + diffEntry.getNewPath() + statementToString(statement) + " added");
                //Get the impacts at the method level
                testToRun.addAll(getMethodImpacts(java2File, statement));
            }
        });
    }

    /**
     * Find all the impacts of the method containing the {@link com.github.javaparser.ast.stmt.Statement}
     *
     * @param java2File the file in which the {@link com.github.javaparser.ast.stmt.Statement} will be found
     * @param statement the {@link com.github.javaparser.ast.stmt.Statement} that will be found in the {@link Java2File}
     * @return all the test {@link MethodDeclaration} impacted by the {@link com.github.javaparser.ast.stmt.Statement}
     */
    private Collection<MethodDeclaration> getMethodImpacts(Java2File java2File, com.github.javaparser.ast.stmt.Statement statement) {
        Set<MethodDeclaration> methodDeclarationSet = new HashSet<>();
        java2File.getChildren()
                .stream()
                //Iterating over the nodes of the Java2File to find the method
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof MethodDeclaration
                        && statement.getRange().isPresent()
                        && astNodeSourceRegion.getStartLine() <= statement.getRange().get().begin.line
                        && astNodeSourceRegion.getEndLine() >= statement.getRange().get().end.line
                        && !astNodeSourceRegion.getAnalysis().isEmpty())
                //For each method modified, find the impacts
                .forEach(node -> node.getAnalysis().forEach(eObject -> {
                    Analysis analysis = (Analysis) eObject;
                    methodDeclarationSet.addAll(analysis.getTarget().stream().map(eObject1 -> (MethodDeclaration) eObject1).collect(Collectors.toSet()));
                }));
        return methodDeclarationSet;
    }

    /**
     * Find all the impacts of the specified {@link com.github.javaparser.ast.stmt.Statement} on the tests
     *
     * @param java2File the file in which the {@link com.github.javaparser.ast.stmt.Statement} will be found
     * @param statement the {@link com.github.javaparser.ast.stmt.Statement} that will be found in the {@link Java2File}
     * @return all the test {@link MethodDeclaration} impacted by the {@link com.github.javaparser.ast.stmt.Statement}
     */
    private Collection<MethodDeclaration> getImpacts(Java2File java2File, com.github.javaparser.ast.stmt.Statement statement) {
        Set<MethodDeclaration> methodDeclarationSet = new HashSet<>();

        java2File.getChildren()
                .stream()
                //Iterating over the Java2File children
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof Statement
                        && statement.getRange().isPresent()
                        && astNodeSourceRegion.getStartLine() == statement.getRange().get().begin.line
                        && astNodeSourceRegion.getEndLine() == statement.getRange().get().end.line
                        && !astNodeSourceRegion.getAnalysis().isEmpty()) //The node is a java statement with the same statement position
                //Iterating over the statement's analysis to get the impacts
                .forEach(node -> node.getAnalysis().forEach(eObject -> {
                    Analysis analysis = (Analysis) eObject;
                    methodDeclarationSet.addAll(analysis.getTarget().stream().map(eObject1 -> (MethodDeclaration) eObject1).collect(Collectors.toSet()));
                }));

        return methodDeclarationSet;
    }

    /**
     * Parse a file to gather all the statements from a specific {@link DiffEntry}, form the old state of the code
     *
     * @param diffEntry a {@link DiffEntry}
     * @param edit      an {@link Edit}
     * @return a {@link BlockStmt}
     */
    private BlockStmt getStatementsFromOldPath(DiffEntry diffEntry, Edit edit) {
        BlockStmt blockStmtBefore = new BlockStmt();
        for (AtomicInteger i = new AtomicInteger(edit.getBeginA()); i.get() <= edit.getEndA(); i.incrementAndGet()) {
            String line = null;
            try {
                //Getting the line of the original File
                line = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, oldTree, diffEntry.getOldPath()), i.get());

                //Computing the statements inside this line
                BlockStmt blockStmt = JavaParser.parseBlock("{" + line + "}");
                setBlockLines(i.get(), blockStmt);
                blockStmtBefore.getStatements().addAll(blockStmt.getStatements());
            } catch (ParseProblemException e) {
                LOGGER.log(Level.FINE, "Couldn't get the statements of the following line: " + line, e);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't get the statements of the following line: " + i.get(), e);
            }
        }

        return blockStmtBefore;
    }

    /**
     * Parse a file to gather all the statements from a specific {@link DiffEntry}, form the new state of the code
     *
     * @param diffEntry a {@link DiffEntry}
     * @param edit      an {@link Edit}
     * @return a {@link BlockStmt} containing the statements
     */
    private BlockStmt getStatementsFromNewPath(DiffEntry diffEntry, Edit edit) {
        BlockStmt blockStmtAfter = new BlockStmt();
        for (AtomicInteger i = new AtomicInteger(edit.getBeginB()); i.get() <= edit.getEndB(); i.incrementAndGet()) {
            String line = null;
            try {
                //Getting the line of the original File
                line = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, newTree, diffEntry.getNewPath()), i.get());

                //Computing the statements inside this line
                BlockStmt blockStmt = JavaParser.parseBlock("{" + line + "}");
                setBlockLines(i.get(), blockStmt);
                blockStmtAfter.getStatements().addAll(blockStmt.getStatements());
            } catch (ParseProblemException e) {
                LOGGER.log(Level.FINE, "Couldn't get the statements of the following line: " + line, e);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't get the following line: " + i, e);
            }
        }

        return blockStmtAfter;
    }

    /**
     * Set the line number of every statement in a block.
     * Since we're parsing only a line of code, the line number will be 1 for all the statements in this line.
     * Instead, we set the correct line number according to the compilation unit, instead of the block itself.
     *
     * @param line      the line number
     * @param blockStmt a {@link BlockStmt} containing statements located on a single line of code
     */
    private void setBlockLines(int line, BlockStmt blockStmt) {
        blockStmt.getStatements().forEach(statement -> {
            if (statement.getRange().isPresent()) {
                Position begin = new Position(line, statement.getRange().get().begin.column);
                Position end = new Position(line, statement.getRange().get().end.column);
                statement.setRange(new Range(begin, end));
            }
        });
    }

    /**
     * Gets testToRun
     *
     * @return value of testToRun
     */
    public Set<MethodDeclaration> getTestToRun() {
        return testToRun;
    }

    /**
     * Parse a statement to create a string to display out of it
     *
     * @param statement the {@link com.github.javaparser.ast.stmt.Statement}
     * @return a {@link String}
     */
    private static String statementToString(com.github.javaparser.ast.stmt.Statement statement) {
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
     * Sets testToRun
     *
     * @param testToRun a {@link Set} of {@link MethodDeclaration}
     */
    public void setTestToRun(Set<MethodDeclaration> testToRun) {
        this.testToRun = testToRun;
    }
}