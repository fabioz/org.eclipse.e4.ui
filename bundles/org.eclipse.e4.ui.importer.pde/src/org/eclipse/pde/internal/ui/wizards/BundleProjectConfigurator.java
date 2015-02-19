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
package org.eclipse.pde.internal.ui.wizards;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.JDTProjectNature;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.osgi.framework.Constants;

public class BundleProjectConfigurator implements ProjectConfigurator {

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		try {
			IFile manifestResource = project.getFolder("META-INF").getFile("MANIFEST.MF");
			if (manifestResource.exists()) {
				Manifest manifest = new Manifest();
				InputStream stream = manifestResource.getContents();
				manifest.read(stream);
				stream.close();
				return manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null;
			}
		} catch (Exception ex) {
			Activator.getDefault().getLog().log(new Status(
					IStatus.ERROR,
					Activator.PLUGIN_ID,
					ex.getMessage(),
					ex));
		}
		return false;
	}

	@Override
	public IWizard getConfigurationWizard() {
		return null;
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		if (PDE.hasPluginNature(project)) {
			// already configured, nothing else to do
			return;
		}
		try {
			IJavaProject javaProject = null;
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
				javaProject = JavaCore.create(project);
			} else {
				javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
			}
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
			IFile buildProperties = project.getFile("build.properties");
			if (buildProperties.exists()) {
				WorkspaceBuildModel build = new WorkspaceBuildModel(buildProperties);
				Set<IContainer> sources = new HashSet<IContainer>();
				for (IBuildEntry entry : build.getBuild().getBuildEntries()) {
					if (entry.getName().startsWith("src.")) {
						for (String token : entry.getTokens()) {
							IFolder folder = project.getFolder(token);
							if (folder.exists()) {
								sources.add(folder);
							}
						}
					}
				}
				Set<IClasspathEntry> cpEntries = new HashSet<IClasspathEntry>(Arrays.asList(javaProject.getRawClasspath()));
				if (!sources.isEmpty()) {
					for (IClasspathEntry entry : javaProject.getRawClasspath()) {
						if (entry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
							cpEntries.remove(entry);
						}
					}
					for (IContainer sourceFolder : sources) {
						cpEntries.add(JavaCore.newSourceEntry(sourceFolder.getFullPath()));
					}
				}
				cpEntries.add(ClasspathComputer.createContainerEntry());
				javaProject.setRawClasspath(cpEntries.toArray(new IClasspathEntry[cpEntries.size()]), monitor);
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
		return container.getFile(new Path("META-INF/MANIFEST.MF")).exists();
	}

	@Override
	public Set<IFolder> getDirectoriesToIgnore(IProject project, IProgressMonitor monitor) {
		return new JDTProjectNature().getDirectoriesToIgnore(project, monitor);
		// TODO add directories declared for src.* and bin.* in build.properties
	}

}
