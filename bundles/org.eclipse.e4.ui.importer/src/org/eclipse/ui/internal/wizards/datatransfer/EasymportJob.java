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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class EasymportJob extends Job {

	private File rootDirectory;
	private IWorkingSet[] workingSets;
	private boolean recursiveConfigure;
	private IProject rootProject;
	private IWorkspaceRoot workspaceRoot;
	private ProjectConfiguratorExtensionManager configurationManager;
	private RecursiveImportListener listener;
	private Map<IProject, List<ProjectConfigurator>> report;
	
	public EasymportJob(File rootDirectory, Set<IWorkingSet> workingSets, boolean recuriveConfigure) {
		super(rootDirectory.getAbsolutePath());
		this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		this.rootDirectory = rootDirectory;
		if (workingSets != null) {
			this.workingSets = workingSets.toArray(new IWorkingSet[workingSets.size()]);
		} else {
			this.workingSets = new IWorkingSet[0];
		}
		this.recursiveConfigure = recuriveConfigure;
		this.report = new HashMap<>();
	}
	
	public void setListener(RecursiveImportListener listener) {
		this.listener = listener;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			this.rootProject = toExistingOrNewProject(
					this.rootDirectory,
					monitor);
			
			if (this.recursiveConfigure) {
		        IWorkspace workspace = ResourcesPlugin.getWorkspace();
		        IWorkspaceDescription description = workspace.getDescription();
		        boolean isAutoBuilding = workspace.isAutoBuilding();
		        if (isAutoBuilding) {
		        	description.setAutoBuilding(false);
		        	workspace.setDescription(description);
		        }
		        
				importProjectAndChildrenRecursively(this.rootProject, true, monitor);
				
				if (isAutoBuilding) {
					description.setAutoBuilding(true);
		        	workspace.setDescription(description);
				}
			}
		} catch (Exception ex) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex);
		}
		return Status.OK_STATUS;
	}

	
	private final class CrawlFolderJob extends Job {
		private final IFolder childFolder;
		private final Set<IProject> res;

		private CrawlFolderJob(String name, IFolder childFolder, Set<IProject> res) {
			super(name);
			this.childFolder = childFolder;
			this.res = res;
		}

		@Override
		public IStatus run(IProgressMonitor progressMonitor) {
			try {
				Set<IProject> projectFromCurrentContainer = importProjectAndChildrenRecursively(childFolder, false, progressMonitor);
				res.addAll(projectFromCurrentContainer);
				return Status.OK_STATUS;
			} catch (Exception ex) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex);
			}
		}
	}

	private Set<IProject> searchAndImportChildrenProjectsRecursively(IContainer parentContainer, Set<IPath> directoriesToExclude, final IProgressMonitor progressMonitor) throws Exception {
		Set<IFolder> childrenToProcess = new HashSet<IFolder>();
		final Set<IProject> res = Collections.synchronizedSet(new HashSet<IProject>());
		for (IResource childResource : parentContainer.members()) {
			if (childResource.getType() == IResource.FOLDER && !childResource.isDerived()) {
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
		// TODO parallelize here
		
		for (final IFolder childFolder : childrenToProcess) {
			CrawlFolderJob crawlerJob = new CrawlFolderJob("Crawling " + childFolder.getLocation().toString(), childFolder, res);
			crawlerJob.run(progressMonitor);
		}
		return res;
	}

	/**
	 * @param folder
	 * @param workingSets
	 * @param progressMonitor
	 * @param listener 
	 * @return
	 * @throws Exception
	 */
	private Set<IProject> importProjectAndChildrenRecursively(IContainer container, boolean isRootProject, IProgressMonitor progressMonitor) throws Exception {
		if (progressMonitor.isCanceled()) {
			return null;
		}
		if (this.configurationManager == null) {
			this.configurationManager = new ProjectConfiguratorExtensionManager();
		}
		progressMonitor.setTaskName("Inspecting " + container.getLocation().toFile().getAbsolutePath());
		Collection<ProjectConfigurator> activeConfigurators = this.configurationManager.getAllActiveProjectConfigurators(container);
		Set<IProject> projectFromCurrentContainer = new HashSet<IProject>();
		Set<ProjectConfigurator> mainProjectConfigurators = new HashSet<ProjectConfigurator>();
		Set<ProjectConfigurator> secondaryConfigurators = new HashSet<ProjectConfigurator>();
		Set<IPath> excludedPaths = new HashSet<IPath>();
		IProject project = null;
		for (ProjectConfigurator configurator : activeConfigurators) {
			if (progressMonitor.isCanceled()) {
				return null;
			}
			if (configurator.shouldBeAnEclipseProject(container, progressMonitor)) {
				mainProjectConfigurators.add(configurator);
				if (project == null) {
					// Create project
					project = toExistingOrNewProject(container.getLocation().toFile(), progressMonitor);
					if (this.listener != null) {
						this.listener.projectCreated(project);
					}
					// use directly project for next analysis
					container = project;
					projectFromCurrentContainer.add(project);					
				}
			} else {
				secondaryConfigurators.add(configurator);
			}
			progressMonitor.worked(1);
		}
		

		if (!mainProjectConfigurators.isEmpty()) {
			for (ProjectConfigurator configurator : mainProjectConfigurators) {
				configurator.configure(project, excludedPaths, progressMonitor);
				this.report.get(project).add(configurator);
				if (this.listener != null) { 
					listener.projectConfigured(project, configurator);
				}
				excludedPaths.addAll(toPathSet(configurator.getDirectoriesToIgnore(project, progressMonitor)));
			}
		}
		
		Set<IProject> allNestedProjects = searchAndImportChildrenProjectsRecursively(container, excludedPaths, progressMonitor);
		excludedPaths.addAll(toPathSet(allNestedProjects));
		
		if (allNestedProjects.isEmpty() && isRootProject) {
			// No sub-project found, so apply available configurators anyway
			progressMonitor.beginTask("Configuring 'leaf' of project at " + container.getLocation().toFile().getAbsolutePath(), activeConfigurators.size());
			project = toExistingOrNewProject(container.getLocation().toFile(), progressMonitor);
			if (this.listener != null) {
				listener.projectCreated(project);
			}
			projectFromCurrentContainer.add(project);
		}
		
		if (project != null) {
			// Apply secondary configurators
			progressMonitor.beginTask("Continue configuration of project at " + container.getLocation().toFile().getAbsolutePath(), secondaryConfigurators.size());
			for (ProjectConfigurator additionalConfigurator : secondaryConfigurators) {
				if (additionalConfigurator.canConfigure(project, excludedPaths, progressMonitor)) {
					additionalConfigurator.configure(project, excludedPaths, progressMonitor);
					this.report.get(project).add(additionalConfigurator);
					if (this.listener != null) {
						listener.projectConfigured(project, additionalConfigurator);
					}
					excludedPaths.addAll(toPathSet(additionalConfigurator.getDirectoriesToIgnore(project, progressMonitor)));
				}
				progressMonitor.worked(1);
			}
			projectFromCurrentContainer.addAll(allNestedProjects);
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
	private IProject toExistingOrNewProject(File directory, IProgressMonitor progressMonitor) throws CouldNotImportProjectException {
		try {
			progressMonitor.setTaskName("Import project at " + directory.getAbsolutePath());
			IProject project = projectAlreadyExistsInWorkspace(directory);
			if (project == null) {
				project = createOrImportProject(directory, progressMonitor);
			}

			if (progressMonitor.isCanceled()) {
				return null;
			}
			project.open(progressMonitor);
			this.report.put(project, new ArrayList<ProjectConfigurator>());
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

	private IProject createOrImportProject(File directory, IProgressMonitor progressMonitor) throws Exception {
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
			StringBuilder currentName = new StringBuilder(directory.getName());
			while (this.workspaceRoot.getProject(currentName.toString()).exists()) {
				currentName.append('_');
			}
			desc = ResourcesPlugin.getWorkspace().newProjectDescription(currentName.toString());
		}
		desc.setLocation(new Path(directory.getAbsolutePath()));
		IProject res = workspaceRoot.getProject(desc.getName());
		// TODO? open Configuration wizard
		res.create(desc, progressMonitor);
		PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(res, this.workingSets);
		return res;
	}
	
	public IProject getRootProject() {
		return this.rootProject;
	}
	
	public Map<IProject, List<ProjectConfigurator>> getConfiguredProjects() {
		return report;
	}
}
