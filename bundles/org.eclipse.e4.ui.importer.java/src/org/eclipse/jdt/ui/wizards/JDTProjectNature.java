/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat Inc., and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.Activator;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class JDTProjectNature implements ProjectConfigurator {

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		return shouldBeAnEclipseProject(project, monitor);
	}

	@Override
	public IWizard getConfigurationWizard() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		try {
			IProjectDescription description = project.getDescription();
			List<String> natures = Arrays.asList(description.getNatureIds());
			if (!natures.contains(JavaCore.NATURE_ID)) {
				List<String> newNatures = new ArrayList<String>(natures);
				newNatures.add(JavaCore.NATURE_ID);
				description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
				project.setDescription(description, monitor);
			}
		} catch (Exception ex) {
			Activator.getDefault().getLog().log(new Status(
					IStatus.ERROR,
					Activator.PLUGIN_ID,
					ex.getMessage(),
					ex));
		}
	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return container.getFile(new Path(".classpath")).exists();
	}

	@Override
	public Set<IFolder> getDirectoriesToIgnore(IProject project, IProgressMonitor monitor) {
		Set<IFolder> res = new HashSet<IFolder>();
		try {
			IJavaProject javaProject = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
			IResource resource = project.getWorkspace().getRoot().findMember(javaProject.getOutputLocation());
			if (resource != null && resource.exists() && resource.getType() == IResource.FOLDER) {
				res.add((IFolder)resource);
			}
			for (IClasspathEntry entry : javaProject.getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IFolder sourceFolder = project.getFolder(entry.getPath());
					res.add(sourceFolder);
				}
			}
		} catch (CoreException ex) {
			Activator.log(IStatus.ERROR, ex.getMessage());
		}
		return res;
	}

	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		// TODO Might be relevant to search .classpath files
		return null;
	}

}
