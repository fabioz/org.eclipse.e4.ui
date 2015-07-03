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
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
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
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.wizards.JavaProjectNature;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.osgi.framework.Constants;

public class BundleProjectConfigurator implements ProjectConfigurator {

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		IFile manifestFile = PDEProject.getManifest(project);;
		if (manifestFile != null && manifestFile.exists()) {
			for (IPath ignoredDirectory : ignoredDirectories) {
				if (ignoredDirectory.isPrefixOf(manifestFile.getLocation())) {
					return false;
				}
			}
		}
		return hasOSGiManifest(project);
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
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
			if (project.hasNature(JavaCore.NATURE_ID)) {
				return;
			}
			// configure Java & Classpaht
			IFile buildPropertiesFile = PDEProject.getBuildProperties(project);
			Properties buildProperties = new Properties();
			if (buildPropertiesFile.exists()) {
				InputStream stream = buildPropertiesFile.getContents();
				buildProperties.load(stream);
				stream.close();
			}
			boolean hasSourceFolder = false;
			for (String entry : buildProperties.stringPropertyNames()) {
				hasSourceFolder |= (entry.startsWith("src.") || entry.startsWith("source."));
			}
			if (!hasSourceFolder) {
				// Nothing for Java
				return;
			}
			
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
			IJavaProject javaProject = JavaCore.create(project);
			Set<IClasspathEntry> classpath = new HashSet<>();
			for (Entry<?, ?> entry : buildProperties.entrySet()) {
				String entryKey = (String)entry.getKey();
				if (entryKey.startsWith("src.") || entryKey.startsWith("source.")) {
					for (String token : ((String)entry.getValue()).split(",")) {
						token = token.trim();
						if (token.endsWith("/")) {
							token = token.substring(0, token.length() - 1);
						}
						if (token != null && token.length() > 0 && !token.equals(".")) {
							IFolder folder = project.getFolder(token);
							if (folder.exists()) {
								classpath.add(JavaCore.newSourceEntry(folder.getFullPath()));
							}
						}
					}
				} else if (entryKey.equals("output..")) {
					javaProject.setOutputLocation(project.getFolder(((String)entry.getValue()).trim()).getFullPath(), monitor);
				}
			}
			// TODO select container according to BREE
			classpath.add(JavaRuntime.getDefaultJREContainerEntry());
			classpath.add(ClasspathComputer.createContainerEntry());
			javaProject.setRawClasspath(classpath.toArray(new IClasspathEntry[classpath.size()]), monitor);
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
		return hasOSGiManifest(container);
	}

	private boolean hasOSGiManifest(IContainer container) {
		try {
			IFile manifestResource = container.getFile(new Path("META-INF/MANIFEST.MF"));
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
	public Set<IFolder> getDirectoriesToIgnore(IProject project, IProgressMonitor monitor) {
		Set<IFolder> res = new HashSet<IFolder>();
		res.addAll(new JavaProjectNature().getDirectoriesToIgnore(project, monitor));
		try {
			IFile buildPropertiesFile = PDEProject.getBuildProperties(project);
			Properties buildProperties = new Properties();
			if (buildPropertiesFile.exists()) {
				InputStream stream = buildPropertiesFile.getContents();
				buildProperties.load(stream);
				stream.close();
			}
			for (Entry<?, ?> entry : buildProperties.entrySet()) {
				String entryKey = (String)entry.getKey();
				if (entryKey.startsWith("src.") || entryKey.startsWith("source.") ||
					entryKey.startsWith("bin.") || entryKey.startsWith("output.")) {
					for (String token : ((String)entry.getValue()).split(",")) {
						token = token.trim();
						if (token.endsWith("/")) {
							token = token.substring(0, token.length() - 1);
						}
						if (token != null && token.length() > 0 && !token.equals(".")) {
							IFolder folder = project.getFolder(token);
							if (folder.exists()) {
								res.add(folder);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			Activator.getDefault().getLog().log(new Status(
					IStatus.ERROR,
					Activator.PLUGIN_ID,
					ex.getMessage(),
					ex));
		}
		return res;
	}

}
