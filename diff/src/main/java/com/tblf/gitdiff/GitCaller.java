package com.tblf.gitdiff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.google.common.collect.Range;
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
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        try {
            ObjectId current = repository.resolve(currentCommitID);
            ObjectId future = repository.resolve(nextCommitID);

            if (current == null || future == null) {
                throw new IOException("Cannot resolve the commits: " + current + " -> " + future);
            }

            oldTree = new RevWalk(repository).parseCommit(current).getTree();
            newTree = new RevWalk(repository).parseCommit(future).getTree();
            diffFormatter = new DiffFormatter(new LogOutputStream(LOGGER, Level.FINE));
            diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);

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
    protected void analyseDiffs(List<DiffEntry> diffEntries) {

        diffEntries.forEach(diffEntry -> {
            try {
                if (!diffEntry.getNewPath().endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: " + diffEntry.getNewPath() + " does not concern a Java file");

                if (diffEntry.getOldPath().equals("/dev/null"))
                    manageNewFile(diffEntry);
                else
                    manageUpdatedFile(diffEntry);

            } catch (NonJavaFileException e) {
                LOGGER.log(Level.FINE, e.toString());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Couldn't analyze the diffEntry", e);
            }
        });

        LOGGER.info("Impact analysis completed");
    }

    private void manageUpdatedFile(DiffEntry diffEntry) throws IOException {
        String pkg;
        String uri = diffEntry.getNewPath();

        FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);

        LOGGER.fine("Analyzing impacts of " + uri + " modification");

        pkg = ParserUtils.getPackageQNFromFile(new File(uri));

        Resource sutPackage = ModelUtils.getPackageResource(pkg, resourceSet);

        //WONT GET NEW TEST FILES ???

        Java2File java2File = (Java2File) sutPackage.getContents()
                .stream()
                .filter(eObject -> eObject instanceof Java2File
                        && ((Java2File) eObject).getJavaUnit().getOriginalFilePath().endsWith(fileHeader.getOldPath()))
                .findFirst()
                .orElseThrow(() -> new NonJavaFileException("The DiffEntry does not concern a Java file but: " + fileHeader.getOldPath() + " No impact computed from it"));

        diffFormatter.format(diffEntry);
        fileHeader.toEditList().forEach(edit -> manageEdit(diffEntry, edit, java2File));

    }

    /**
     * A new file has been added in this {@link DiffEntry}. If it's a new test class, it needs to be added to the tests to run
     * @param diffEntry a {@link DiffEntry}
     */
    private void manageNewFile(DiffEntry diffEntry) {
        try {
            diffFormatter.toFileHeader(diffEntry).toEditList().forEach(edit -> {
                manageInsertion(diffEntry, edit); //FIXME
            });
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "couldn't analyze the new file "+diffEntry.getNewPath(), e);
        }
    }

    /**
     * Compute the statements modified by the commit
     *
     * @param diffEntry a {@link DiffEntry}
     * @param edit      an {@link Edit}
     */
    protected void manageEdit(DiffEntry diffEntry, Edit edit, Java2File java2File) {

        if (edit.getType().equals(Edit.Type.INSERT)) {
            manageInsertion(diffEntry, edit, java2File);
        } else {

            BlockStmt blockStmtBefore = getStatementsAsBlockFromOldPath(diffEntry, edit);
            BlockStmt blockStmtAfter = getStatementsAsBlockFromNewPath(diffEntry, edit);

            LOGGER.fine(blockStmtBefore.getStatements().size() + " statements before and " + blockStmtAfter.getStatements().size() + " after. Now comparing.");

            //Statement modified or removed
            blockStmtBefore.getStatements().forEach(statement -> {
                if (!blockStmtAfter.getStatements().contains(statement) && statement.getRange().isPresent()) {
                    LOGGER.fine("In file " + diffEntry.getOldPath() + DiffUtils.statementToString(statement) + " modified.");
                    //Get the impacts at the statement level
                    impactedTests.addAll(getMethodImpacts(java2File, statement)
                            .stream()
                            .map(methodDeclaration ->
                                    ModelUtils.getQualifiedName(methodDeclaration.eContainer()) + "#" + methodDeclaration.getName())
                            .collect(Collectors.toSet()));
                }
            });

            //New statements
            blockStmtAfter.getStatements().forEach(statement -> {
                if (!blockStmtBefore.getStatements().contains(statement) && statement.getRange().isPresent()) {
                    LOGGER.fine("In file " + diffEntry.getNewPath() + DiffUtils.statementToString(statement) + " added");
                    //Get the impacts at the method level

                    //FIXME
                    impactedTests.addAll(getMethodImpacts(java2File, statement)
                            .stream()
                            .map(methodDeclaration ->
                                    ModelUtils.getQualifiedName(methodDeclaration.eContainer()) + "#" + methodDeclaration.getName())
                            .collect(Collectors.toSet()));
                }
            });
        }
    }


    /**
     * Manage an insertion. Since those statements are new, they haven't been analysed yet. Thus, their impacts are a bit harder to compute
     * If it's in a test: Re run this test
     * If it's in a method from the System under test, get the impacts at the method level
     *
     * @param diffEntry a {@link DiffEntry}
     * @param edit      an {@link Edit}
     */
    private void manageInsertion(DiffEntry diffEntry, Edit edit, Java2File java2File) {
        try {
            String fileAsString = DiffUtils.getFileContentFromCommit(repository, newTree, diffEntry.getNewPath());
            CompilationUnit compilationUnit = JavaParser.parse(fileAsString);

            List<com.github.javaparser.ast.body.MethodDeclaration> methodDeclarations =
                    compilationUnit
                            .getChildNodesByType(com.github.javaparser.ast.body.MethodDeclaration.class)
                            .stream()
                            .filter(methodDeclaration -> methodDeclaration.getRange().isPresent())
                            .filter(methodDeclaration -> {

                                        com.google.common.collect.Range<Integer> methodRange = com.google.common.collect.Range.closed(
                                                methodDeclaration.getRange().get().begin.line,
                                                methodDeclaration.getRange().get().end.line);

                                        com.google.common.collect.Range<Integer> editRange = com.google.common.collect.Range.closed(
                                                edit.getBeginB() + 1,
                                                edit.getEndB());

                                        return editRange.isConnected(methodRange);
                                    }
                            ).collect(Collectors.toList());

            //Add all the new methods found edited to the tests to run
            //Map all the MethodDeclaration to a String as: Classname#MethodName

            if (diffEntry.getNewPath().contains(Configuration.getProperty("test"))) {
                //The code added is inside test classes. They need to be added in the tests to run.
                newTests.addAll(methodDeclarations
                        .stream()
                        .filter(methodDeclaration -> methodDeclaration.getAncestorOfType(ClassOrInterfaceDeclaration.class).isPresent())
                        .map(methodDeclaration -> String.format("%s.%s#%s",
                                compilationUnit.getPackageDeclaration().get().getName().asString(), //qualified package
                                methodDeclaration.getAncestorOfType(ClassOrInterfaceDeclaration.class).get().getName().asString(), //class name
                                methodDeclaration.getName().asString())) //methodName
                        .collect(Collectors.toList()));
            } else {
                //The code added is inside SUT. Its impact through inheritance must be computed
                methodDeclarations.forEach(methodDeclaration -> {
                    getMethodImpacts(java2File, methodDeclaration);
                });
            }



        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not parse the modified file: "+diffEntry.getNewPath(), e);
        }
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
                .forEach(node -> node.getAnalysis()
                        .forEach(eObject -> {
                    Analysis analysis = (Analysis) eObject;
                    methodDeclarationSet.addAll(analysis.getTarget()
                            .stream()
                            .map(eObject1 -> (MethodDeclaration) eObject1)
                            .collect(Collectors.toSet()));
                }));
        return methodDeclarationSet;
    }

    /**
     * Get the tests that are impacted by insertion inside the {@link com.github.javaparser.ast.body.MethodDeclaration}
     * @param java2File a {@link Java2File}
     * @param methodDeclaration a {@link MethodDeclaration}
     */
    private void getMethodImpacts(Java2File java2File, com.github.javaparser.ast.body.MethodDeclaration methodDeclaration) {
        Collection<ASTNodeSourceRegion> astNodeSourceRegions =
                java2File.getChildren()
                .stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof MethodDeclaration)
                .filter(astNodeSourceRegion -> methodDeclaration.getRange().isPresent())
                .filter(astNodeSourceRegion -> Range.open(astNodeSourceRegion.getStartLine(), astNodeSourceRegion.getEndLine())
                            .isConnected(Range.open(methodDeclaration.getRange().get().begin.line, methodDeclaration.getRange().get().end.line)))
                .collect(Collectors.toList());

        if (astNodeSourceRegions.isEmpty()) {
            //The impacted node cannot be found. We assume the method is new.
            //Impacts must be computed at the declaration level

            getDeclarationLevelImpacts(java2File, methodDeclaration);
        } else {
            astNodeSourceRegions
                    .stream()
                    .map(ASTNodeSourceRegion::getAnalysis) //Get all the impacts of the found methods using position comparison
                    .flatMap(List::stream)
                    .collect(Collectors.toList())
                    .stream() //Merge all the analysis into a single stream
                    .filter(eObject -> eObject instanceof MethodDeclaration)
                    .map(eObject -> ((MethodDeclaration) eObject))
                    .collect(Collectors.toList()); //Return all as a methodDeclaration list
        }



    }

    /**
     * Compute the impacts at the declaration level.
     * We consider the method as new, if it overrides an existing method in a superclass,
     * then the impacts of this supermethod must be considered.
     * Nonetheless, only the impacts of the tests that executed the supermethod, of the child-class.
     * @param java2File
     * @param methodDeclaration
     */
    private void getDeclarationLevelImpacts(Java2File java2File, com.github.javaparser.ast.body.MethodDeclaration methodDeclaration) {
        java2File.getChildren()
                .stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof ClassDeclaration)
                .filter(astNodeSourceRegion -> ((ClassDeclaration) astNodeSourceRegion.getNode()).getSuperClass() != null)
                .collect(Collectors.toList());
    }

    /**
     * Parse a file to gather all the statements from a specific {@link DiffEntry}, form the old state of the code
     *
     * @param diffEntry a {@link DiffEntry}
     * @param edit      an {@link Edit}
     * @return a {@link BlockStmt}
     */
    private BlockStmt getStatementsAsBlockFromOldPath(DiffEntry diffEntry, Edit edit) {
        BlockStmt blockStmtBefore = new BlockStmt();
        for (AtomicInteger i = new AtomicInteger(edit.getBeginA()); i.get() <= edit.getEndA(); i.incrementAndGet()) {
            String line = null;
            try {
                //Getting the line of the original File
                line = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, oldTree, diffEntry.getOldPath()), i.get());

                //Computing the statements inside this line
                BlockStmt blockStmt = JavaParser.parseBlock("{" + line + "}");
                DiffUtils.setLineNumberInBlockStatements(i.get(), blockStmt);
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
    private BlockStmt getStatementsAsBlockFromNewPath(DiffEntry diffEntry, Edit edit) {
        BlockStmt blockStmtAfter = new BlockStmt();
        for (AtomicInteger i = new AtomicInteger(edit.getBeginB()); i.get() <= edit.getEndB(); i.incrementAndGet()) {
            String line = null;
            try {
                //Getting the line of the original File
                line = DiffUtils.getLineFromFile(DiffUtils.getFileContentFromCommit(repository, newTree, diffEntry.getNewPath()), i.get());

                //Computing the statements inside this line
                BlockStmt blockStmt = JavaParser.parseBlock("{" + line + "}");
                DiffUtils.setLineNumberInBlockStatements(i.get(), blockStmt);
                blockStmtAfter.getStatements().addAll(blockStmt.getStatements());
            } catch (ParseProblemException e) {
                LOGGER.log(Level.FINE, "Couldn't get the statements of the following line: " + line, e);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Couldn't get the following line: " + i, e);
            }
        }

        return blockStmtAfter;
    }
}