package com.tblf.diff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.tblf.Model.Analysis;
import com.tblf.parsing.ParserUtils;
import com.tblf.util.ModelUtils;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private Java2File java2File;

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
                String pkg = ParserUtils.getPackageQNFromSUTFile(new File(diffEntry.getOldPath()));

                if (java2File == null) {
                    Resource sutPackage = ModelUtils.getPackageResource(pkg, resourceSet);
                    java2File = (Java2File) sutPackage.getContents()
                            .stream()
                            .filter(eObject -> eObject instanceof Java2File
                                    && ((Java2File) eObject).getJavaUnit().getOriginalFilePath().endsWith(fileHeader.getOldPath()))
                            .findFirst()
                            .orElseThrow(() -> new IOException("The DiffEntry does not concern a Java file"));


                    LOGGER.info("Java File currently analysed :"+java2File.getJavaUnit().getName());
                }

                diffFormatter.format(diffEntry);
                fileHeader.toEditList().forEach(edit -> {
                   manageEdit(diffEntry, edit);
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
    private void manageEdit(DiffEntry diffEntry, Edit edit) {

        BlockStmt blockStmtBefore = getStatementsFromOldPath(diffEntry, edit);
        BlockStmt blockStmtAfter = getStatementsFromNewPath(diffEntry, edit);

        LOGGER.fine(blockStmtBefore.getStatements().size()+ " statements before and "+blockStmtAfter.getStatements().size()+" after. Now comparing.");

        //Statement modified or removed
        blockStmtBefore.getStatements().forEach(statement -> {
            if (! blockStmtAfter.getStatements().contains(statement) && statement.getRange().isPresent()) {
                LOGGER.info("In file "+diffEntry.getOldPath()+ParserUtils.statementToString(statement)+" modified.");
                getImpacts(java2File, statement);

                //TODO: return
            }
        });

        //New statements
        blockStmtAfter.getStatements().forEach(statement -> {
            if (! blockStmtBefore.getStatements().contains(statement) && statement.getRange().isPresent()) {
                LOGGER.info("In file "+diffEntry.getNewPath()+ParserUtils.statementToString(statement)+" added");
                //Get the impacts at the method level

                //TODO
            }
        });

    }

    /**
     * Find all the impacts of the specified {@link com.github.javaparser.ast.stmt.Statement} on the tests
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
                    MethodDeclaration methodDeclaration = (MethodDeclaration) analysis.getTarget();
                    ClassDeclaration classDeclaration = (ClassDeclaration) methodDeclaration.eContainer();
                    LOGGER.fine("The test method: " + methodDeclaration.getName() + " of the test class " + classDeclaration.getName() + " is impacted by this modification");
                    methodDeclarationSet.add(methodDeclaration);
                }));

        return methodDeclarationSet;
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