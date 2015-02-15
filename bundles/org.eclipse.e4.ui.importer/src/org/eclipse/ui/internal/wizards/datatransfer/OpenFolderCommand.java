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
package org.eclipse.ui.internal.wizards.datatransfer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class OpenFolderCommand extends AbstractHandler {

	private Shell shell;
	private IWorkspaceRoot workspaceRoot;
	private ProjectConfiguratorExtensionManager configurationManager;

	public OpenFolderCommand() {
		this(ResourcesPlugin.getWorkspace().getRoot());
	}
	
	public OpenFolderCommand(IWorkspaceRoot workspaceRoot) {
		super();
		this.workspaceRoot = workspaceRoot;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		this.shell = workbench.getActiveWorkbenchWindow().getShell();
		DirectoryDialog directoryDialog = new DirectoryDialog(shell);
		directoryDialog.setText(Messages.selectFolderToImport);
		IStructuredSelection sel = (IStructuredSelection)workbench.getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (!sel.isEmpty()) {
			File selectedFile = EasymportWizard.toFile(sel.getFirstElement());
			if (selectedFile != null) {
				directoryDialog.setFilterPath(selectedFile.getAbsolutePath());
			}
		}
		String res = directoryDialog.open();
		if (res == null) {
			return null;
		}
		EasymportWizard wizard = new EasymportWizard();
		final File directory = new File(res);
		wizard.setInitialDirectory(directory);
		// inherit workingSets
		final Path asPath = new Path(directory.getAbsolutePath());
		IProject parentProject = null;
		for (IProject project : this.workspaceRoot.getProjects()) {
			if (project.getLocation().isPrefixOf(asPath) && (parentProject == null || parentProject.getLocation().isPrefixOf(project.getLocation())) ) {
				parentProject = project;
			}
		}
		Set<IWorkingSet> initialWorkingSets = new HashSet<IWorkingSet>();
		if (parentProject != null) {
			for (IWorkingSet workingSet : workbench.getWorkingSetManager().getAllWorkingSets()) {
				for (IAdaptable element : workingSet.getElements()) {
					if (element.equals(parentProject)) {
						initialWorkingSets.add(workingSet);
					}
				}
			}
		}
		if (initialWorkingSets.isEmpty()) {
			wizard.init(workbench, sel);
		} else {
			wizard.setInitialWorkingSets(initialWorkingSets);
		}
		return new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), wizard).open();
	}

	/**
	 * @param directory
	 * @param workingSets
	 */
	public void importProjectsFromDirectory(final File directory, final Set<IWorkingSet> workingSets) {
		this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
					try {
						final IProject project = toExistingOrNewProject(directory, workingSets, progressMonitor);
						importProjectAndChildrenRecursively(project, true, workingSets, progressMonitor);
					} catch (final Exception ex) {
						final Status status = new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), ex.getMessage(), ex);
						Activator.getDefault().getLog().log(status);
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								ErrorDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
										"Could not fully import " + directory.getName(),
										"An error happened while try to import " + directory.getAbsolutePath() + ": " + ex.getMessage(),
										status);
							}
						});
					}
				}
			});
		} catch (Exception ex) {
			Status status = new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), ex.getMessage(), ex);
			Activator.getDefault().getLog().log(status);
			ErrorDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
					"Could not fully import " + directory.getName(),
					"An error happened while try to import " + directory.getAbsolutePath() + ": " + ex.getMessage(),
					status);
		}
	}

	private Set<IProject> searchAndImportChildrenProjectsRecursively(IContainer parentContainer, Set<IPath> directoriesToExclude, Set<IWorkingSet> workingSets, IProgressMonitor progressMonitor) throws Exception {
		Set<IFolder> childrenToProcess = new HashSet<IFolder>();
		Set<IProject> res = new HashSet<IProject>();
		for (IResource childResource : parentContainer.members()) {
			if (childResource.getType() == IResource.FOLDER) {
				boolean excluded = false;
				if (directoriesToExclude != null) {
					for (IPath excludedPath : directoriesToExclude) {
						if (excludedPath.isPrefixOf(childResource.getLocation())) {
							excluded = true;
						}
					}
				}
				if (!excluded) {
					childrenToProcess.add((IFolder)childResource);
				}
			}
		}
		for (IFolder childFolder : childrenToProcess) {
			try {
				Set<IProject> projectFromCurrentContainer = importProjectAndChildrenRecursively(childFolder, false, workingSets, progressMonitor);
				res.addAll(projectFromCurrentContainer);
			} catch (CouldNotImportProjectException ex) {
				// TODO accumulate the multiple issues and present it to users after import
				Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getLog().getBundle().getSymbolicName(), ex.getMessage(), ex));
			}
		}
		return res;
	}

	/**
	 * @param folder
	 * @param workingSets
	 * @param progressMonitor
	 * @return
	 * @throws Exception
	 */
	public Set<IProject> importProjectAndChildrenRecursively(IContainer container, boolean isRootProject, Set<IWorkingSet> workingSets, IProgressMonitor progressMonitor) throws Exception {
		if (progressMonitor.isCanceled()) {
			return null;
		}
		if (this.configurationManager == null) {
			this.configurationManager = new ProjectConfiguratorExtensionManager();
		}
		Collection<ProjectConfigurator> activeConfigurators = this.configurationManager.getAllActiveProjectConfigurators(container);
		progressMonitor.beginTask("Start configuration of project at " + container.getLocation().toFile().getAbsolutePath(), activeConfigurators.size());
		Set<IProject> projectFromCurrentContainer = new HashSet<IProject>();
		Set<ProjectConfigurator> mainProjectConfigurators = new HashSet<ProjectConfigurator>();
		Set<ProjectConfigurator> secondaryConfigurators = new HashSet<ProjectConfigurator>();
		Set<IPath> excludedPaths = new HashSet<IPath>();
		for (ProjectConfigurator configurator : activeConfigurators) {
			if (progressMonitor.isCanceled()) {
				return null;
			}
			if (configurator.shouldBeAnEclipseProject(container, progressMonitor)) {
				mainProjectConfigurators.add(configurator);
			} else {
				secondaryConfigurators.add(configurator);
			}
			progressMonitor.worked(1);
		}
		if (!mainProjectConfigurators.isEmpty()) {
			/*
			 * 1. Create project
			 * 2. Apply ensured project configurators + populate excludedPaths
			 * 3. Look recursively (ignored excluded paths)
			 * 4. Applied additional configurators
			 */
			IProject project = toExistingOrNewProject(container.getLocation().toFile(), workingSets, progressMonitor);
			projectFromCurrentContainer.add(project);
			for (ProjectConfigurator configurator : mainProjectConfigurators) {
				configurator.configure(project, excludedPaths, progressMonitor);
				excludedPaths.addAll(toPathSet(configurator.getDirectoriesToIgnore(project, progressMonitor)));
			}
			Set<IProject> allNestedProjects = searchAndImportChildrenProjectsRecursively(project, excludedPaths, workingSets, progressMonitor);
			excludedPaths.addAll(toPathSet(allNestedProjects));
			progressMonitor.beginTask("Continue configuration of project at " + container.getLocation().toFile().getAbsolutePath(), secondaryConfigurators.size());
			for (ProjectConfigurator additionalConfigurator : secondaryConfigurators) {
				if (additionalConfigurator.canConfigure(project, excludedPaths, progressMonitor)) {
					additionalConfigurator.configure(project, excludedPaths, progressMonitor);
					excludedPaths.addAll(toPathSet(additionalConfigurator.getDirectoriesToIgnore(project, progressMonitor)));
				}
				progressMonitor.worked(1);
			}
			projectFromCurrentContainer.addAll(allNestedProjects);
		} else {
			Set<IProject> nestedProjects = searchAndImportChildrenProjectsRecursively(container, null, workingSets, progressMonitor);
			projectFromCurrentContainer.addAll(nestedProjects);
			if (nestedProjects.isEmpty() && isRootProject) {
				// No sub-project found, so apply available configurators anyway
				progressMonitor.beginTask("Configuring 'leaf' of project at " + container.getLocation().toFile().getAbsolutePath(), activeConfigurators.size());
				IProject project = toExistingOrNewProject(container.getLocation().toFile(), workingSets, progressMonitor);
				projectFromCurrentContainer.add(project);
				for (ProjectConfigurator activeConfigurator : activeConfigurators) {
					if (activeConfigurator.canConfigure(project, excludedPaths, progressMonitor)) {
						activeConfigurator.configure(project, excludedPaths, progressMonitor);
						excludedPaths.addAll(toPathSet(activeConfigurator.getDirectoriesToIgnore(project, progressMonitor)));
					}
					progressMonitor.worked(1);
				}
			}
		}
		return projectFromCurrentContainer;
	}

	private Set<IPath> toPathSet(Set<? extends IContainer> resources) {
		if (resources == null || resources.isEmpty()) {
			return (Set<IPath>)Collections.EMPTY_SET;
		}
		Set<IPath> res = new HashSet<IPath>();
		for (IContainer container : resources) {
			res.add(container.getLocation());
		}
		return res;
	}

	/**
	 * @param directory
	 * @param workingSets
	 * @return
	 * @throws Exception
	 */
	public IProject toExistingOrNewProject(File directory, Set<IWorkingSet> workingSets, IProgressMonitor progressMonitor) throws CouldNotImportProjectException {
		try {
			progressMonitor.setTaskName("Import project at " + directory.getAbsolutePath());
			IProject project = projectAlreadyExistsInWorkspace(directory);
			if (project == null) {
				project = createOrImportProject(directory, workingSets, progressMonitor);
			}

			if (progressMonitor.isCanceled()) {
				return null;
			}
			project.open(progressMonitor);
			return project;
		} catch (Exception ex) {
			throw new CouldNotImportProjectException(directory, ex);
		}
	}


	private IProject projectAlreadyExistsInWorkspace(File directory) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (IProject project : workspaceRoot.getProjects()) {
			if (project.getLocation().toFile().getAbsoluteFile().equals(directory.getAbsoluteFile())) {
				return project;
			}
		}
		return null;
	}

	private IProject createOrImportProject(File directory, Set<IWorkingSet> workingSets, IProgressMonitor progressMonitor) throws Exception {
		IProjectDescription desc = null;
		File expectedProjectDescriptionFile = new File(directory, IProjectDescription.DESCRIPTION_FILE_NAME);
		if (expectedProjectDescriptionFile.exists()) {
			desc = ResourcesPlugin.getWorkspace().loadProjectDescription(new Path(expectedProjectDescriptionFile.getAbsolutePath()));
			String expectedName = desc.getName();
			IProject projectWithSameName = this.workspaceRoot.getProject(expectedName);
			if (projectWithSameName.exists()) {
				if (projectWithSameName.getLocation().toFile().equals(directory)) {
					throw new Exception(NLS.bind(Messages.anotherProjectWithSameNameExists_description, expectedName));
				}
			}
		} else {
			String currentName = directory.getName();
			while (this.workspaceRoot.getProject(currentName).exists()) {
				currentName += "_";
			}
			desc = ResourcesPlugin.getWorkspace().newProjectDescription(currentName);
		}
		desc.setLocation(new Path(directory.getAbsolutePath()));
		IProject res = workspaceRoot.getProject(desc.getName());
		// TODO? open Configuration wizard
		res.create(desc, progressMonitor);
		PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(res, workingSets.toArray(new IWorkingSet[workingSets.size()]));
		return res;
	}

}