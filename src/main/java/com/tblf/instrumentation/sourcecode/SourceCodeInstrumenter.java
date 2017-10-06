package com.tblf.instrumentation.sourcecode;

import com.tblf.instrumentation.Instrumenter;
import com.tblf.instrumentation.sourcecode.processors.TargetProcessor;
import com.tblf.instrumentation.sourcecode.processors.TestProcessor;
import spoon.Launcher;

import java.io.File;
import java.util.Collection;

public class SourceCodeInstrumenter implements Instrumenter {

    private File directory;
    private File testDirectory;
    private File sutDirectory;

    private File binDirectory;
    private File srcDirectory;

    private Collection<File> dependencies;

    public SourceCodeInstrumenter(File directory, Collection<File> dependencies) {
        this.directory = directory;
        this.dependencies = dependencies;
    }

    public SourceCodeInstrumenter(File directory) {
        this.directory = directory;
    }

    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        //TODO use hashsets instead of standard lists to optimize the contains() method
        Launcher spoonLauncher = new Launcher();

        spoonLauncher.getEnvironment().setShouldCompile(true);
        spoonLauncher.getEnvironment().setAutoImports(true);
        spoonLauncher.getEnvironment().setNoClasspath(false);

        addLinkerToDependencies();

        spoonLauncher.getEnvironment().setSourceClasspath((String[]) dependencies.stream().map(File::getAbsolutePath).toArray());

        spoonLauncher.addInputResource(srcDirectory.toString());
        spoonLauncher.addInputResource(sutDirectory.toString());

        spoonLauncher.setBinaryOutputDirectory(binDirectory);
        spoonLauncher.setSourceOutputDirectory(srcDirectory);

        spoonLauncher.addProcessor(new TargetProcessor());
        spoonLauncher.addProcessor(new TestProcessor());
    }

    private void addLinkerToDependencies() {
        File file = new File("libs/Link-1.0.0.jar");
        if (file.exists()) {
            dependencies.add(file);
        }
    }
}
