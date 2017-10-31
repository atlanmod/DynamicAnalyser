package com.tblf.gitdiff;

import com.tblf.business.Manager;
import com.tblf.utils.Configuration;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GitCallerTest {

    @Before
    public void setUp() {

    }

    @Test
    public void checkCompareCommit() {
        File file = new File("src/test/resources/files");
        Assert.assertTrue(file.exists());
        GitCaller gitCaller = new GitCaller(file, new ResourceSetImpl());
        gitCaller.compareCommits("HEAD~1");
    }

    @Test
    public void checkCompareCommitStandardProject() throws IOException {
        File zip = new File("src/test/resources/fullprojects/SimpleProject.zip");
        ModelUtils.unzip(zip);

        Configuration.setProperty("mode", "SOURCECODE");
        Manager manager = new Manager();

        File file = new File("src/test/resources/fullprojects/SimpleProject");
        Assert.assertTrue(file.exists());

        try {
            long before = System.currentTimeMillis();
            File trace = manager.buildTraces(file);

            System.out.println("Elapsed time: " + String.valueOf(System.currentTimeMillis() - before)+" ms");

            Assert.assertNotNull(trace);

            Resource analysis = manager.parseTraces(trace);

            Assert.assertNotNull(analysis);
            analysis.getResourceSet().getResources().forEach(resource -> System.out.println(resource.getURI()));

            //Verify that among all the resources, there at least one ASTNode with an Analysis
            Assert.assertTrue(! analysis
                    .getResourceSet()
                    .getResources()
                    .stream() //resourceSet
                    .filter(resource -> ! resource
                            .getContents()
                            .stream() //resource
                            .filter(eObject -> eObject instanceof Java2File)
                            .filter(eObject -> ! ((Java2File) eObject)
                                    .getChildren()
                                    .stream() //EObject
                                    .filter(astNodeSourceRegion -> ! astNodeSourceRegion
                                            .getAnalysis()
                                            .isEmpty())
                                    .collect(Collectors.toList())
                                    .isEmpty())
                            .collect(Collectors.toList())
                            .isEmpty())
                    .collect(Collectors.toList())
                    .isEmpty());

            initRepo(file);
            GitCaller gitCaller = new GitCaller(file, analysis.getResourceSet());
            gitCaller.compareCommits("HEAD~1");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        FileUtils.deleteDirectory(file);
    }

    /**
     * Init a git repository in the file
     * @param repo
     */
    private void initRepo(File repo) {
        try {
            Git git = Git.init().setDirectory(repo).call();
            git.add().addFilepattern("*").call();
            git.commit().setAll(true).setMessage("first commit").call();

            File fileToModify = new File(repo, "src/main/java/com/tblf/SimpleProject/App.java");
            Assert.assertTrue(fileToModify.exists());

            List<String> newLines = new ArrayList<>();

            for (String line : Files.readAllLines(fileToModify.toPath(), StandardCharsets.UTF_8)) {
                if (line.contains("has been called")) {
                    newLines.add("System.out.println(\"has been modified\");");
                    System.out.println("Updated the line");
                } else {
                    newLines.add(line);
                }
            }
            Files.write(fileToModify.toPath(), newLines, StandardCharsets.UTF_8);

            git.commit().setAll(true).setMessage("Modification done").call();
        } catch (GitAPIException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
