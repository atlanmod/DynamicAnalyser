package com.tblf.main;

import com.tblf.business.Manager;
import com.tblf.gitdiff.GitCaller;
import com.tblf.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.NamedElement;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class TestMain {

    static final String URI = "/home/thibault/Documents/git/JsonPullParser/jsonpullparser-core";
    private Git git;

    @Before
    public void setUp() throws GitAPIException {
        git = Git.init().setDirectory(new File(URI)).call();
        DirCache dirCache = git.add().addFilepattern(".").call();
        Assert.assertTrue(dirCache.getEntryCount() > 0);

        git.commit().setAll(true).setMessage("hey, this is my first commit").call();

        //Be sure that the project is at the right state
    }

    @Test
    @Ignore
    public void test() throws IOException, GitAPIException {
        File file = new File(URI);

        //Creating the impact analysis model
        Assert.assertTrue(file.exists());

        Configuration.setProperty("mode", "BYTECODE");

        Manager manager = new Manager();
        File trace = manager.buildTraces(file);
        Resource resource = manager.parseTraces(trace);

        Assert.assertTrue(resource.getContents().size() > 0 );

        //Modifying a file and commiting the changes
        File fileToModify = new File(file, "src/main/java/net/vvakame/util/jsonpullparser/JsonPullParser.java");
        Assert.assertTrue(fileToModify.exists());

        List<String> lines = Files.readAllLines(fileToModify.toPath(), StandardCharsets.UTF_8);

        String extraLine = "System.out.println(\"hello world!\");";

        lines.add(227, extraLine);
        Files.write(fileToModify.toPath(), lines, StandardCharsets.UTF_8);

        git.commit().setAll(true).setMessage("Modification done").call();

        //Computing the impacts of this modification
        GitCaller gitCaller = new GitCaller(file, resource.getResourceSet());
        gitCaller.compareCommits("HEAD~1");


        //Displaying all the methods that would need a rerun
        System.out.println(gitCaller.getTestToRun().size()+" test method impacted:");
        gitCaller.getTestToRun().forEach(methodDeclaration -> System.out.println(methodDeclaration.getName() +" in class "+ ((NamedElement) methodDeclaration.eContainer()).getName()));
    }

    @After
    public void tearDown() throws IOException, GitAPIException {
        //delete the git project
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").call();
        FileUtils.deleteDirectory(new File(new File(URI), ".git"));

    }
}
