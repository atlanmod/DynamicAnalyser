package com.tblf.gitdiff;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.collect.Range;
import com.tblf.utils.Configuration;
import com.tblf.utils.ModelUtils;
import com.tblf.utils.ParserUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GitDiffManager {

    private ResourceSet resourceSet;
    private Collection<DiffEntry> diffEntries;
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private DiffFormatter diffFormatter = new DiffFormatter(new LogOutputStream(LOGGER, Level.FINE));

    public GitDiffManager(ResourceSet resourceSet, Collection<DiffEntry> diffEntries) {
        this.resourceSet = resourceSet;
        this.diffEntries = diffEntries;
    }

    public void analyse() {
        Collection<String> testsToRun = new ArrayList<>();

        diffEntries.forEach(diffEntry -> {
            try {

                // The file is not a Java File.
                if (!diffEntry.getNewPath().endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: " + diffEntry.getNewPath() + " does not concern a Java file");

                if (diffEntry.getOldPath().equals("/dev/null")) {
                    testsToRun.addAll(
                            manageNewFile(diffEntry) // The file is new
                    );
                } else {
                    testsToRun.addAll(
                            manageUpdatedFile(diffEntry) // The file has been updated
                    );
                }

            } catch (IOException e) {
                LOGGER.log(Level.INFO, "Could not analyze the diffEntry", e);
            }
        });
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
        String packageQNFromFile = ParserUtils.getPackageQNFromFile(new File(diffEntry.getOldPath()));
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
        Collection<String> methodDeclarationQualifiedNames = new ArrayList<>();

        if (diffEntry.getNewPath().contains(Configuration.getProperty("test"))) {
            //get all the methods edited, and map their name to Strings qualified name, ready to run.
            methodDeclarationQualifiedNames.addAll(
                    getMethodDeclarationsEdited(diffEntry.getNewPath(), Range.open(edit.getBeginB(), edit.getEndB()))
                            .stream()
                            .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                            .collect(Collectors.toList())
            );
        } else switch (edit.getType()) {
            case INSERT:
                //Collection of the changed methodDeclarations
                Collection<MethodDeclaration> methodDeclarations = getMethodDeclarationsEdited(diffEntry.getNewPath(), Range.open(edit.getBeginB(), edit.getEndB()));

                //if new method => get the impacts at the inheritance level
                //else just rerun the method


                break;
            case DELETE:
                //Code has been deleted. The impacted methods are gathered, their impacts are computed, and returned as test methods Qualified names
                methodDeclarations = getMethodDeclarationsEdited(diffEntry.getOldPath(), Range.open(edit.getBeginA(), edit.getEndA()));
                methodDeclarationQualifiedNames.addAll(
                        methodDeclarations.stream()
                        .map(methodDeclaration -> getImpactsAtMethodLevel(java2File, methodDeclaration))
                        .flatMap(Collection::stream)
                        .map(GitDiffManager::methodDeclarationToStringQualifiedName)
                        .collect(Collectors.toList())
                );

                break;
            case REPLACE:

                break;
            default:

        }

        return methodDeclarationQualifiedNames;
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
            CompilationUnit compilationUnit = JavaParser.parse(new File(fileName));

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
     * Get all the test methods impacted by changes on a MethodDeclaration
     *
     * @param java2File
     * @param methodDeclaration
     * @return
     */
    private Collection<org.eclipse.gmt.modisco.java.MethodDeclaration> getImpactsAtMethodLevel(Java2File java2File, MethodDeclaration methodDeclaration) {
        return java2File.getChildren()
                .stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof org.eclipse.gmt.modisco.java.MethodDeclaration)
                .filter(astNodeSourceRegion -> ((org.eclipse.gmt.modisco.java.MethodDeclaration) astNodeSourceRegion.getNode()).getName().equals(methodDeclaration.getName().toString()))
                .map(ASTNodeSourceRegion::getAnalysis)
                .flatMap(Collection::stream)
                .map(eObject -> ((org.eclipse.gmt.modisco.java.MethodDeclaration) eObject))
                .collect(Collectors.toList());
    }

    private Collection<org.eclipse.gmt.modisco.java.MethodDeclaration> getImpactsAtMethodLevel(Java2File java2File, Statement statement) {

        return null;
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

            CompilationUnit compilationUnit = JavaParser.parse(new File(uri));
            Collection<MethodDeclaration> methodDeclarations = compilationUnit.getChildNodesByType(MethodDeclaration.class);
            return methodDeclarationsToStringCollection(methodDeclarations);
        } else {
            return Collections.emptyList();
        }
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
