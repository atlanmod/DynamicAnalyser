package com.tblf.discovery;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.modisco.infra.discovery.core.AbstractModelDiscoverer;
import org.eclipse.modisco.infra.discovery.core.exception.DiscoveryException;
import org.eclipse.modisco.java.composition.discoverer.DiscoverKDMSourceAndJavaModelFromJavaProject;

import java.io.File;
import java.io.IOException;

/**
 * Parse Java projects in order to generate the MoDisco model
 */
public class Discoverer {

    private static final IProgressMonitor PROGRESS_MONITOR = new NullProgressMonitor();

    public static void generateFullModel(File project) throws IOException {
        if (!project.exists() || !project.isDirectory()) {
            throw new IOException("The project at URI "+project.getAbsolutePath()+" is not correct");
        }

        try {
            IProjectDescription description;
            if (new File(project, ".project").exists()) {
                description = ResourcesPlugin.getWorkspace().loadProjectDescription(new Path(new File(project, ".project").getAbsolutePath()));
            } else {
                description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
                description.setLocation(new Path(project.getAbsolutePath()));
                description.setNatureIds(new String[]{JavaCore.NATURE_ID});
            }

            IProject iProject = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
            iProject.create(description, PROGRESS_MONITOR);
            iProject.open(PROGRESS_MONITOR);

            IJavaProject iJavaProject = JavaCore.create(iProject);
            iJavaProject.open(PROGRESS_MONITOR);

            AbstractModelDiscoverer<IJavaProject> iJavaProjectAbstractModelDiscoverer = new DiscoverKDMSourceAndJavaModelFromJavaProject();
            iJavaProjectAbstractModelDiscoverer.setSerializeTarget(true);
            iJavaProjectAbstractModelDiscoverer.discoverElement(iJavaProject, PROGRESS_MONITOR);

        } catch (CoreException | DiscoveryException e) {
            throw new IOException(e);
        }
    }

    /**
     * Generates the full Modisco model of a java project using MoDisco
     * @param absoluteUri the absolute uri to the project.
     * see the generateFullModel method for more data
     */
    public static void generateFullModel(String absoluteUri) throws IOException {
        generateFullModel(new File(absoluteUri));
    }
}
