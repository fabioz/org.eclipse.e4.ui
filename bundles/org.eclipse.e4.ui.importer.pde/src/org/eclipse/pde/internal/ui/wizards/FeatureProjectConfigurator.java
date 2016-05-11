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

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class FeatureProjectConfigurator implements ProjectConfigurator {

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		IFile featureFile = project.getFile("feature.xml");
		if (featureFile.exists()) {
			WorkspaceFeatureModel workspaceFeatureModel = new WorkspaceFeatureModel(featureFile);
			workspaceFeatureModel.load();
			return workspaceFeatureModel.isLoaded();
		}
		return featureFile.exists();
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		if (!PDE.hasFeatureNature(project)) {
			try {
				CoreUtility.addNatureToProject(project, PDE.FEATURE_NATURE, monitor);
			} catch (Exception ex) {
				Activator.getDefault().getLog().log(new Status(
						IStatus.ERROR,
						Activator.PLUGIN_ID,
						ex.getMessage(),
						ex));
			}
		}
	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return container.getFile(new Path("feature.xml")).exists();
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		return null;
	}
	
	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		// Mot really easy to spot PDE projects from a given directory
		// Moreover PDE projects are often expected to have a .project, which is supported
		// by EclipseProjectConfigurator
		return null;
	}

}
