package com.tblf.gitdiff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.collect.Range;
import com.tblf.utils.Configuration;
import com.tblf.utils.ModelUtils;
import com.tblf.utils.ParserUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.AbstractMethodDeclaration;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GitDiffManager {

    private ResourceSet resourceSet;
    private Collection<DiffEntry> diffEntries;
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private DiffFormatter diffFormatter;
    private File folder;

    public GitDiffManager(File gitFolder, ResourceSet resourceSet, Collection<DiffEntry> diffEntries, DiffFormatter diffFormatter) {
        this.resourceSet = resourceSet;
        this.diffEntries = diffEntries;
        this.diffFormatter = diffFormatter;
        this.folder = gitFolder;
    }

    public Collection<String> analyse() {
        Collection<String> testsToRun = new ArrayList<>();
        diffEntries.forEach(diffEntry -> {

            try {

                // The file is not a Java File.
                if (!diffEntry.getNewPath().endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: " + diffEntry.getNewPath() + " does not concern a Java file");

                if (diffEntry.getOldPath().equals("/dev/null")) {
                    LOGGER.fine("File added in the current revision: "+diffEntry.getNewPath());
                    testsToRun.addAll(
                            manageNewFile(diffEntry) // The file is new
                    );
                } else {
                    LOGGER.fine("File updated in the current revision: "+diffEntry.getNewPath());
                    testsToRun.addAll(
                            manageUpdatedFile(diffEntry) // The file has been updated
                    );
                }

            } catch (IOException e) {
                LOGGER.log(Level.INFO, "Could not analyze the diffEntry", e);
            }
        });

        return testsToRun;
    }

    /**
     * Get a diffEntry of a new file added in the previous commit and analyze it
     *
     * @param diffEntry a {@link DiffEntry}
     * @return a {@link Collection} of Test names as {@link String} to run
     * @throws IOException
     */
    private Collection<String> manageNewFile(DiffEntry diffEntry) throws IOException {
        String uri = diffEntry.getNewPath();

        if (uri.contains(Configuration.getProperty("test"))) {
            //The new file is a test class
            LOGGER.fine("File added is a test file: "+diffEntry.getNewPath());

            CompilationUnit compilationUnit = JavaParser.parse(new File(folder, uri));
            Collection<MethodDeclaration> methodDeclarations = compilationUnit.getChildNodesByType(MethodDeclaration.class);
            return methodDeclarationsToStringCollection(methodDeclarations);
        } else {
            LOGGER.fine("File added is a SUT file: "+diffEntry.getNewPath()+" no impacts can be computed yet");
            return Collections.emptyList();
        }
    }

    /**
     * Analyse the {@link DiffEntry} of an existing file, and return the merged {@link Collection}s of Methods Qualified names
     *
     * @param diffEntry a {@link DiffEntry}
     * @return a {@link Collection} of {@link String}
     * @throws IOException
     */
    private Collection<String> manageUpdatedFile(DiffEntry diffEntry) throws IOException {
        FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);

        //Get the package resource from the resourceSet
        String packageQNFromFile = ParserUtils.getPackageQNFromFile(new File(folder, diffEntry.getOldPath()));
        Resource sutPackage = ModelUtils.getPackageResource(packageQNFromFile, resourceSet);

        Java2File java2File = (Java2File) sutPackage.getContents()
                .stream()
                .filter(eObject -> eObject instanceof Java2File
                        && ((Java2File) eObject).getJavaUnit().getOriginalFilePath().endsWith(fileHeader.getOldPath()))
                .findFirst()
                .orElseThrow(() -> new NonJavaFileException("The DiffEntry does not concern a Java file but: " + fileHeader.getOldPath() + " No impact computed from it"));

        diffFormatter.format(diffEntry); //Will display in the logs the diff results

        return fileHeader.toEditList()
                .stream()
                .map(edit -> manageEdit(diffEntry, edit, java2File))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

    }

    /**
     * Manage a {@link DiffEntry} on an existing file. If the {@link Edit} concerns a test file,
     * then no impact is computed, and the updated test is immediately selected.
     * If the {@link Edit} concerns a SUT file, then its impact are computed, according to the edition
     * Insert: if a new method is inserted, impacts are computed at the inheritance level.
     * If no method is added, impacts are computed at the method level
     * Update: impacts are computed at the statement level
     * Delete: impacts are computed at the method level
     *
     * @param diffEntry a {@link DiffEntry}
     * @param edit      an {@link Edit}
     * @param java2File a {@link Java2File}
     * @return a {@link Collection} of test methods qualified name to run
     */
    private Collection<String> manageEdit(DiffEntry diffEntry, Edit edit, Java2File java2File) {
        Collection<String> testMethodQualifiedNamesToExecute = new ArrayList<>();

        if (diffEntry.getNewPath().contains(Configuration.getProperty("test"))) {
            //get all the methods edited, and map their name to Strings qualified name, ready to run.
            testMethodQualifiedNamesToExecute.addAll(
                    getMethodDeclarationsEdited(diffEntry.getNewPath(), Range.open(edit.getBeginB(), edit.getEndB()))
                            .stream()
                            .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                            .collect(Collectors.toList())
            );

        } else switch (edit.getType()) {
            case INSERT:
                testMethodQualifiedNamesToExecute.addAll(
                        manageInsertion(diffEntry, edit, java2File)
                );
                break;
            case DELETE:
                testMethodQualifiedNamesToExecute.addAll(
                        manageDeletion(diffEntry, edit, java2File)
                );
                break;
            case REPLACE:
                testMethodQualifiedNamesToExecute.addAll(
                        manageReplacement(diffEntry, edit, java2File)
                );
                break;
            default:

        }

        return testMethodQualifiedNamesToExecute;
    }

    /**
     * Manage an Insertion in the source code
     *
     * @param diffEntry the {@link DiffEntry} considering the diffs between the two revisions
     * @param edit      the {@link Edit} with {@link org.eclipse.jgit.diff.Edit.Type}.INSERT as type
     * @param java2File the {@link Java2File} containing the impact analysis results to used to get the impacted methods
     * @return the Qualified names of the methods impacted, ready to re-execute
     */
    private List<String> manageInsertion(DiffEntry diffEntry, Edit edit, Java2File java2File) {
        //Collection of the changed methodDeclarations
        Collection<MethodDeclaration> methodDeclarationsEdited = getMethodDeclarationsEdited(diffEntry.getNewPath(), Range.open(edit.getBeginB(), edit.getEndB()));
        Collection<org.eclipse.gmt.modisco.java.MethodDeclaration> methodDeclarationsImpacted =
                methodDeclarationsEdited.stream().map(methodDeclaration -> getImpactsAtMethodLevel(java2File, methodDeclaration))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

        if (methodDeclarationsImpacted.size() == 0) {
            //No impacts, is this method new ?
            Collection<MethodDeclaration> newMethods = getNewMethods(java2File, methodDeclarationsEdited);
            //Get the impacts of newly added method by checking its class inheritance
            return newMethods.stream().map(methodDeclaration -> getImpactsAtInheritanceLevel(java2File, methodDeclaration))
                    .flatMap(Collection::stream)
                    .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                    .collect(Collectors.toList());
        } else {
            //Add to the tests to execute the test method impacted
            return methodDeclarationsImpacted
                    .stream()
                    .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Manage a deletion in the source code
     *
     * @param diffEntry the {@link DiffEntry} considering the diffs between the two revisions
     * @param edit      the {@link Edit} with {@link org.eclipse.jgit.diff.Edit.Type}.DELETE as type
     * @param java2File the {@link Java2File} containing the impact analysis results to used to get the impacted methods
     * @return the Qualified names of the methods impacted, ready to re-execute
     */
    private List<String> manageDeletion(DiffEntry diffEntry, Edit edit, Java2File java2File) {
        //Code has been deleted. The impacted methods are gathered, their impacts are computed, and returned as test methods Qualified names
        Collection<MethodDeclaration> methodDeclarationsEdited = getMethodDeclarationsEdited(diffEntry.getOldPath(), Range.open(edit.getBeginA(), edit.getEndA()));
        return methodDeclarationsEdited.stream()
                .map(methodDeclaration -> getImpactsAtMethodLevel(java2File, methodDeclaration))
                .flatMap(Collection::stream)
                .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                .collect(Collectors.toList());
    }

    /**
     * Manage a replacement in the source code, considering the impacts at the statement level
     *
     * @param diffEntry the {@link DiffEntry} considering the diffs between the two revisions
     * @param edit      the {@link Edit} with {@link org.eclipse.jgit.diff.Edit.Type}.REPLACE as type
     * @param java2File the {@link Java2File} containing the impact analysis results to used to get the impacted methods
     * @return the Qualified names of the methods impacted, ready to re-execute
     */
    private Collection<? extends String> manageReplacement(DiffEntry diffEntry, Edit edit, Java2File java2File) {
        Collection<Statement> statementsReplaced = getStatementEdited(diffEntry.getOldPath(), Range.open(edit.getBeginA(), edit.getEndA()));
        return statementsReplaced.stream()
                .map(statement -> getImpactsAtMethodLevel(java2File, statement))
                .flatMap(Collection::stream)
                .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                .collect(Collectors.toList());
    }


    /**
     * Compare a set of modified methods to the existing model in order to check if the methods have been recently added in the model
     *
     * @param java2File                {@link Java2File}, the model before modifying the code
     * @param methodDeclarationsEdited a {@link Collection} of {@link MethodDeclaration} recently edited
     */
    private Collection<MethodDeclaration> getNewMethods(Java2File java2File, Collection<MethodDeclaration> methodDeclarationsEdited) {
        Collection<String> methodSignaturesAsString = ModelUtils.getMethodDeclarationFromJava2File(java2File)
                .stream()
                .map(ModelUtils::getMethodSignature)
                .collect(Collectors.toList());

        return methodDeclarationsEdited.stream().filter(methodDeclaration -> !methodSignaturesAsString.contains(methodDeclaration.getSignature().asString())).collect(Collectors.toList());

    }

    /**
     * Parse the file edited in the {@link DiffEntry} and returns all the method that have been edited
     *
     * @param fileName a {@link DiffEntry} {@link String} fileName
     * @param range    an {@link Edit} modified set of lines as a {@link Range}, considering a set of updated lines of code
     * @return a {@link Collection} of {@link MethodDeclaration}s
     */
    private Collection<MethodDeclaration> getMethodDeclarationsEdited(String fileName, Range<Integer> range) {
        Collection<MethodDeclaration> methodDeclarations = new LinkedList<>();

        try {
            CompilationUnit compilationUnit = JavaParser.parse(new File(folder, fileName));

            methodDeclarations.addAll(compilationUnit.getChildNodesByType(MethodDeclaration.class).stream()
                    .filter(methodDeclaration -> methodDeclaration.getRange().isPresent())
                    .filter(methodDeclaration -> Range.open(methodDeclaration.getRange().get().begin.line,
                            methodDeclaration.getRange().get().end.line)
                            .isConnected(range))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not parse the file " + fileName, e);
        }

        return methodDeclarations;
    }


    /**
     * Parse the file edited. First get the modified methods, and then get the impacts at the statement level
     *
     * @param path  the path to the file updated
     * @param range a continuous set of modified lines of code
     * @return a Collection of replaced statements
     */
    private Collection<Statement> getStatementEdited(String path, Range<Integer> range) {

        return getMethodDeclarationsEdited(path, range)
                .stream()
                .map(methodDeclaration -> methodDeclaration.getChildNodesByType(Statement.class)
                        .stream()
                        .filter(statement -> statement.getRange().isPresent()
                                && range.isConnected(Range.open(statement.getRange().get().begin.line, statement.getRange().get().end.line)))
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    /**
     * Get all the test methods impacted by changes on a MethodDeclaration
     *
     * @param java2File         the {@link Java2File} containing the impact analysis results to used to get the impacted methods
     * @param methodDeclaration a {@link org.eclipse.gmt.modisco.java.MethodDeclaration} to find in the model
     * @return a set of Impacted test {@link org.eclipse.gmt.modisco.java.MethodDeclaration}
     */
    private Collection<org.eclipse.gmt.modisco.java.MethodDeclaration> getImpactsAtMethodLevel(Java2File java2File, MethodDeclaration methodDeclaration) {
        return java2File.getChildren()
                .stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof org.eclipse.gmt.modisco.java.MethodDeclaration)
                .filter(astNodeSourceRegion -> GitDiffManager.methodDeclarationToStringQualifiedName((org.eclipse.gmt.modisco.java.MethodDeclaration) astNodeSourceRegion.getNode()).equals(methodDeclaration.getSignature().asString()))
                .map(ASTNodeSourceRegion::getAnalysis)
                .flatMap(Collection::stream)
                .map(eObject -> ((org.eclipse.gmt.modisco.java.MethodDeclaration) eObject))
                .collect(Collectors.toList());
    }

    /**
     * Get all the test methods impacted by changes of a specific {@link Statement}, using the position of the {@link Statement} to find the impacts in the model
     *
     * @param java2File a {@link Java2File}
     * @param statement a {@link Statement}
     * @return a set of Impacted test {@link org.eclipse.gmt.modisco.java.MethodDeclaration}
     */
    private Collection<org.eclipse.gmt.modisco.java.MethodDeclaration> getImpactsAtMethodLevel(Java2File java2File, Statement statement) {
        return java2File.getChildren()
                .stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof org.eclipse.gmt.modisco.java.Statement)
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getStartLine() == statement.getRange().get().begin.line && astNodeSourceRegion.getEndLine() == statement.getRange().get().end.line)
                //.filter(astNodeSourceRegion -> astNodeSourceRegion.getStartPosition() == statement.getRange().get().begin.column && astNodeSourceRegion.getEndPosition() == statement.getRange().get().end.column)
                .map(ASTNodeSourceRegion::getAnalysis)
                .map(eObject -> ((org.eclipse.gmt.modisco.java.MethodDeclaration) eObject))
                .collect(Collectors.toList());
    }


    /**
     * Get the impacts using inheritance.
     *
     * @param java2File         a {@link Java2File} from the MoDisco Model
     * @param methodDeclaration a {@link MethodDeclaration} newly added in the source code
     * @return a {@link Collection} of impacted test methods
     */
    private Collection<org.eclipse.gmt.modisco.java.MethodDeclaration> getImpactsAtInheritanceLevel(Java2File java2File, MethodDeclaration methodDeclaration) {
        AbstractMethodDeclaration overridenMethod = ModelUtils.getOverridenMethod(java2File, methodDeclaration.getSignature().asString());

        //The parent method has been found, its impacts must be gathered, in the java2kdm model

        if (overridenMethod != null && overridenMethod.eContainer() != null && overridenMethod.eContainer() instanceof ClassDeclaration) {
            ClassDeclaration classDeclaration = (ClassDeclaration) overridenMethod.eContainer(); //class containing the overriden method
            File file = ModelUtils.getSrcFromClass(classDeclaration);
            try {
                Java2File superClassJava2File = ModelUtils.getJava2FileInResourceSetFromPathAsString(resourceSet, file.getAbsolutePath());
                return ModelUtils.getASTNodeFromJavaElementInJava2File(superClassJava2File, overridenMethod)
                        .getAnalysis().stream().map(eObject -> ((org.eclipse.gmt.modisco.java.MethodDeclaration) eObject)).collect(Collectors.toList());

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not find the super class " + file.getAbsolutePath() + " in the model", e);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Transform a {@link Collection} of {@link MethodDeclaration} to as {@link Collection} of {@link String} such as:
     * ClassQualifiedName#MethodName
     *
     * @param methodDeclarations a {@link Collection} of {@link MethodDeclaration}
     * @return a {@link Collection} of {@link String}
     */
    private Collection<String> methodDeclarationsToStringCollection(Collection<MethodDeclaration> methodDeclarations) {
        return methodDeclarations
                .stream()
                .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                .collect(Collectors.toList());
    }

    /**
     * Return the qualified name of a {@link MethodDeclaration} as a {@link String}
     *
     * @param methodDeclaration a {@link MethodDeclaration}
     * @return a {@link String} such as "pkg.classname#methodname"
     */
    private static String methodDeclarationToStringQualifiedName(MethodDeclaration methodDeclaration) {
        String pkgName = "";
        String className = "";
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = null;

        if (methodDeclaration.getAncestorOfType(ClassOrInterfaceDeclaration.class).isPresent()) {
            classOrInterfaceDeclaration = methodDeclaration.getAncestorOfType(ClassOrInterfaceDeclaration.class).get();
            className = classOrInterfaceDeclaration.getName().asString();
        }

        if (classOrInterfaceDeclaration != null && classOrInterfaceDeclaration.findCompilationUnit().isPresent() && classOrInterfaceDeclaration.findCompilationUnit().get().getPackageDeclaration().isPresent()) {
            pkgName = classOrInterfaceDeclaration.findCompilationUnit().get().getPackageDeclaration().get().getName().asString();
        }

        return String.format("%s.%s#%s", pkgName, className, methodDeclaration.getName().asString());

    }

    /**
     * Return the qualified name of a {@link org.eclipse.gmt.modisco.java.MethodDeclaration} as a {@link String}
     *
     * @param methodDeclaration a {@link org.eclipse.gmt.modisco.java.MethodDeclaration}
     * @return a {@link String} such as "pkg.classname#methodname"
     */
    private static String methodDeclarationToStringQualifiedName(org.eclipse.gmt.modisco.java.MethodDeclaration methodDeclaration) {
        String qualifiedName = "";


        ClassDeclaration classDeclaration = ModelUtils.getContainerClassDeclaration(methodDeclaration);
        if (classDeclaration != null) {
            qualifiedName = ModelUtils.getQualifiedName(classDeclaration);
        }

        return String.format("%s#%s", qualifiedName, methodDeclaration.getName());
    }
}
