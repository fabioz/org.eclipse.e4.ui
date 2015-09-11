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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class EasymportJob extends Job {

	/*
	 * Input parameters
	 */
	private File rootDirectory;
	private Set<File> directoriesToImport;
	private Set<File> excludedDirectories;
	private boolean discardRootProject;
	private boolean deepChildrenDetection;
	private boolean configureProjects;
	private boolean reconfigureEclipseProjects;
	private IWorkingSet[] workingSets;

	/*
	 * working fields
	 */
	private IProject rootProject;
	private IWorkspaceRoot workspaceRoot;
	private ProjectConfiguratorExtensionManager configurationManager;
	private RecursiveImportListener listener;

	protected Map<File, List<ProjectConfigurator>> importProposals;
	private Map<IProject, List<ProjectConfigurator>> report;
	private Map<IPath, Exception> errors;

	private JobGroup crawlerJobGroup;

	public EasymportJob(File rootDirectory, Set<IWorkingSet> workingSets, boolean configureProjects, boolean recuriveChildrenDetection) {
		super(rootDirectory.getAbsolutePath());
		this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		this.rootDirectory = rootDirectory;
		if (workingSets != null) {
			this.workingSets = workingSets.toArray(new IWorkingSet[workingSets.size()]);
		} else {
			this.workingSets = new IWorkingSet[0];
		}
		this.configureProjects = configureProjects;
		this.deepChildrenDetection = recuriveChildrenDetection;
		this.report = Collections.synchronizedMap(new HashMap<IProject, List<ProjectConfigurator>>());
		this.errors = Collections.synchronizedMap(new HashMap<IPath, Exception>());
		this.crawlerJobGroup = new JobGroup("Detecting and configurating nested projects", 0, 1);
	}

	@Deprecated
	public EasymportJob(File rootDirectory, Set<IWorkingSet> workingSets, boolean configureAndDetectNestedProject) {
		this(rootDirectory, workingSets, configureAndDetectNestedProject, configureAndDetectNestedProject);
	}

	public File getRoot() {
		return this.rootDirectory;
	}
	
	/**
	 * Sets the directories that have been detected by preliminary detection and that
	 * user has selected to import. Those will be imported and configured in any case.
	 * This does not impact output of {@link #getImportProposals(IProgressMonitor)}
	 * @param directories
	 */
	public void setDirectoriesToImport(Set<File> directories) {
		this.directoriesToImport = directories;
	}

	/**
	 * Set directories that users specifically configured as to NOT import.
	 * Projects UNDER those directories may be imported, but never project directly
	 * in one of those directories.
	 * This does not impact output of {@link #getImportProposals(IProgressMonitor)}
	 * @param directories
	 */
	public void setExcludedDirectories(Set<File> directories) {
		this.excludedDirectories = directories;
	}

	public void setListener(RecursiveImportListener listener) {
		this.listener = listener;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
	        IWorkspace workspace = ResourcesPlugin.getWorkspace();
	        IWorkspaceDescription description = workspace.getDescription();
	        boolean isAutoBuilding = workspace.isAutoBuilding();
	        if (isAutoBuilding) {
	        	description.setAutoBuilding(false);
	        	workspace.setDescription(description);
	        }

			if (directoriesToImport != null) {
				Comparator<File> rootToLeafComparator = new Comparator<File>() {
					public int compare(File arg0, File arg1) {
						int lengthDiff = arg0.getAbsolutePath().length() - arg1.getAbsolutePath().length();
						if (lengthDiff != 0) {
							return lengthDiff;
						} else {
							return arg0.compareTo(arg1);
						}
					}
				};
				SortedSet<File> directories = new TreeSet<>(rootToLeafComparator);
				directories.addAll(this.directoriesToImport);
				SortedMap<File, IProject> leafToRootProjects = new TreeMap<>(Collections.reverseOrder(rootToLeafComparator));
				final Set<IProject> alreadyConfiguredProjects = new HashSet<>();
				for (final File directoryToImport : directories) {
					final boolean alreadyAnEclipseProject = new File(directoryToImport, IProjectDescription.DESCRIPTION_FILE_NAME).isFile();
					IProject newProject = toExistingOrNewProject(directoryToImport, monitor, IResource.BACKGROUND_REFRESH);
					if (alreadyAnEclipseProject) {
						alreadyConfiguredProjects.add(newProject);
					}
					leafToRootProjects.put(directoryToImport, newProject);
				}
				if (configureProjects) {
					JobGroup multiDirectoriesJobGroup = new JobGroup("Configuring selected directories", 20, 1);
					for (final IProject newProject : leafToRootProjects.values()) {
						Job directoryJob = new Job("Configuring " + newProject.getName()) {
							protected IStatus run(IProgressMonitor monitor) {
								try {
									importProjectAndChildrenRecursively(newProject, EasymportJob.this.deepChildrenDetection, !alreadyConfiguredProjects.contains(newProject), monitor);
									return Status.OK_STATUS;
								} catch (Exception ex) {
									return new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), ex.getMessage(), ex);
								}
							}
						};
						// Job1 on path1 and Job2 on path2 can be run in parallel IFF path1 isn't a prefix of path2 and vice-versa
						directoryJob.setRule(new SubdirectoryOrSameNameSchedulingRule(newProject));
						directoryJob.setUser(true);
						directoryJob.setJobGroup(multiDirectoriesJobGroup);
						directoryJob.schedule();
					}
					multiDirectoriesJobGroup.join(0, monitor);
				}
				

			} else { // no specific projects included, consider only root
				File rootProjectFile = new File(this.rootDirectory, IProjectDescription.DESCRIPTION_FILE_NAME);
				boolean isRootANewProject = !rootProjectFile.isFile();
				this.rootProject = toExistingOrNewProject(
						this.rootDirectory,
						monitor,
						IResource.NONE); // complete load of the root project
	
	
				if (this.configureProjects) {
					importProjectAndChildrenRecursively(this.rootProject, this.deepChildrenDetection, isRootANewProject, monitor);
	
					if (isRootANewProject && rootProjectWorthBeingRemoved()) {
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								discardRootProject = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
									Messages.discardRootProject_title,
									Messages.discardRootProject_description);
							}
						});
						if (this.discardRootProject) {
							this.rootProject.delete(false, true, monitor);
							if (isRootANewProject) {
								rootProjectFile.delete();
							}
							this.report.remove(this.rootProject);
						}
					}
				}
			}
				
			if (isAutoBuilding) {
				description.setAutoBuilding(true);
	        	workspace.setDescription(description);
			}
		} catch (Exception ex) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex);
		}
		return Status.OK_STATUS;
	}

	protected boolean rootProjectWorthBeingRemoved() {
		if (this.report.size() == 1) {
			return false;
		}
		List<ProjectConfigurator> rootProjectConfigurators = this.report.get(this.rootProject);
		if (rootProjectConfigurators.isEmpty()) {
			return true;
		}
		boolean areOnlyDummyConfigurators = true;
		for (ProjectConfigurator configurator : rootProjectConfigurators) {
			// TODO: semantics whether configurator is "strong enough" for a root project should be put inside configurator
			areOnlyDummyConfigurators &= (configurator instanceof EclipseProjectConfigurator || configurator instanceof EclipseWorkspaceConfigurator);
		}
		return areOnlyDummyConfigurators;
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
				Set<IProject> projectFromCurrentContainer = importProjectAndChildrenRecursively(childFolder, deepChildrenDetection, false, progressMonitor);
				res.addAll(projectFromCurrentContainer);
				return Status.OK_STATUS;
			} catch (Exception ex) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex);
			}
		}
	}

	private Set<IProject> searchAndImportChildrenProjectsRecursively(IContainer parentContainer, Set<IPath> directoriesToExclude, final IProgressMonitor progressMonitor) throws Exception {
		for (IProject processedProjects : Collections.synchronizedSet(this.report.keySet())) {
			if (processedProjects.getLocation().equals(parentContainer.getLocation())) {
				return Collections.EMPTY_SET;
			}
		}
		parentContainer.refreshLocal(IResource.DEPTH_ONE, progressMonitor); // make sure we know all children
		Set<IFolder> childrenToProcess = new HashSet<IFolder>();
		final Set<IProject> res = Collections.synchronizedSet(new HashSet<IProject>());
		for (IResource childResource : parentContainer.members()) {
			if (progressMonitor.isCanceled()) {
				throw new InterruptedException("Interrupted by user");
			}
			if (childResource.getType() == IResource.FOLDER && !childResource.isDerived()) {
				boolean excluded = false;
				if (directoriesToExclude != null) {
					for (IPath excludedPath : directoriesToExclude) {
						if (!excludedPath.isPrefixOf(parentContainer.getLocation()) && excludedPath.isPrefixOf(childResource.getLocation())) {
							excluded = true;
						}
					}
				}
				if (!excluded) {
					childrenToProcess.add((IFolder)childResource);
				}
			}
		}

		Set<CrawlFolderJob> jobs = new HashSet<>();
		for (final IFolder childFolder : childrenToProcess) {
			if (progressMonitor.isCanceled()) {
				throw new InterruptedException("Interrupted by user");
			}
			CrawlFolderJob crawlerJob = new CrawlFolderJob("Crawling " + childFolder.getLocation().toString(), childFolder, res);
			if (crawlerJobGroup.getMaxThreads() == 0 || crawlerJobGroup.getActiveJobs().size() < crawlerJobGroup.getMaxThreads()) {
				crawlerJob.setJobGroup(crawlerJobGroup);
				jobs.add(crawlerJob);
				crawlerJob.schedule();
			} else {
				crawlerJob.run(progressMonitor);
			}
		}
		for (CrawlFolderJob job : jobs) {
			if (progressMonitor.isCanceled()) {
				throw new InterruptedException("Interrupted by user");
			}
			job.join();
		}
		return res;
	}

	private Set<IProject> importProjectAndChildrenRecursively(IContainer container, boolean deepDetectChildren, boolean forceFullProjectCheck, IProgressMonitor progressMonitor) throws Exception {
		if (progressMonitor.isCanceled()) {
			return null;
		}
		progressMonitor.setTaskName("Inspecting " + container.getLocation().toFile().getAbsolutePath());
		Set<IProject> projectFromCurrentContainer = new HashSet<IProject>();
		boolean isAlreadyAnEclipseProject = false;
		Set<ProjectConfigurator> mainProjectConfigurators = new HashSet<ProjectConfigurator>();
		Set<IPath> excludedPaths = new HashSet<IPath>();
		if (this.excludedDirectories != null) {
			for (File excludedDirectory : this.excludedDirectories) {
				excludedPaths.add(new Path(excludedDirectory.getAbsolutePath()));
			}
		}
		container.refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
		if (!forceFullProjectCheck) {
			EclipseProjectConfigurator eclipseProjectConfigurator = new EclipseProjectConfigurator();
			if (eclipseProjectConfigurator.shouldBeAnEclipseProject(container, progressMonitor)) {
				isAlreadyAnEclipseProject = true;
			}
		}

		if (this.configurationManager == null) {
			this.configurationManager = new ProjectConfiguratorExtensionManager();
		}
		Collection<ProjectConfigurator> activeConfigurators = this.configurationManager.getAllActiveProjectConfigurators(container);
		Set<ProjectConfigurator> potentialSecondaryConfigurators = new HashSet<ProjectConfigurator>();
		IProject project = null;
		for (ProjectConfigurator configurator : activeConfigurators) {
			if (progressMonitor.isCanceled()) {
				return null;
			}
			// exclude Eclipse project configurator for root project if is new
			if (configurator instanceof EclipseProjectConfigurator && forceFullProjectCheck) {
				continue;
			}
			if (configurator.shouldBeAnEclipseProject(container, progressMonitor)) {
				mainProjectConfigurators.add(configurator);
				if (project == null) {
					// Create project
					try {
						project = toExistingOrNewProject(container.getLocation().toFile(), progressMonitor, IResource.BACKGROUND_REFRESH);
					} catch (CouldNotImportProjectException ex) {
						this.errors.put(container.getLocation(), ex);
						if (this.listener != null) {
							this.listener.errorHappened(container.getLocation(), ex);
						}
						return projectFromCurrentContainer;
					}
					projectFromCurrentContainer.add(project);
				}
			} else {
				potentialSecondaryConfigurators.add(configurator);
			}
			progressMonitor.worked(1);
		}

		if (!mainProjectConfigurators.isEmpty()) {
			project.refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
		}
		for (ProjectConfigurator configurator : mainProjectConfigurators) {
			if (progressMonitor.isCanceled()) {
				throw new InterruptedException("Interrupted by user");
			}
			if (configurator instanceof EclipseProjectConfigurator || !isAlreadyAnEclipseProject || this.reconfigureEclipseProjects) {
				configurator.configure(project, excludedPaths, progressMonitor);
				this.report.get(project).add(configurator);
				if (this.listener != null) {
					listener.projectConfigured(project, configurator);
				}
			}
			excludedPaths.addAll(toPathSet(configurator.getDirectoriesToIgnore(project, progressMonitor)));
		}

		Set<IProject> allNestedProjects = new HashSet<>();
		if (deepChildrenDetection) {
			allNestedProjects.addAll( searchAndImportChildrenProjectsRecursively(container, excludedPaths, progressMonitor) );
			excludedPaths.addAll(toPathSet(allNestedProjects));
			projectFromCurrentContainer.addAll(allNestedProjects);
		}

		if (mainProjectConfigurators.isEmpty() && (!isAlreadyAnEclipseProject || forceFullProjectCheck)) {
			// Apply secondary configurators
			if (project == null) {
				// Create project
				try {
					project = toExistingOrNewProject(container.getLocation().toFile(), progressMonitor, IResource.BACKGROUND_REFRESH);
				} catch (CouldNotImportProjectException ex) {
					this.errors.put(container.getLocation(), ex);
					if (this.listener != null) {
						this.listener.errorHappened(container.getLocation(), ex);
					}
					return projectFromCurrentContainer;
				}
				projectFromCurrentContainer.add(project);
			}
			project.refreshLocal(IResource.DEPTH_ONE, progressMonitor); // At least one, maybe INFINITE is necessary
			progressMonitor.beginTask("Continue configuration of project at " + container.getLocation().toFile().getAbsolutePath(), potentialSecondaryConfigurators.size());
			for (ProjectConfigurator additionalConfigurator : potentialSecondaryConfigurators) {
				if (progressMonitor.isCanceled()) {
					throw new InterruptedException("Interrupted by user");
				}
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
		}
		return projectFromCurrentContainer;
	}

	private Set<IPath> toPathSet(Set<? extends IContainer> resources) {
		if (resources == null || resources.isEmpty()) {
			return Collections.EMPTY_SET;
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
	 * @param refreshMode One {@link IResource#BACKGROUND_REFRESH} for background refresh, or {@link IResource#NONE} for immediate refresh
	 * @return
	 * @throws Exception
	 */
	private IProject toExistingOrNewProject(File directory, IProgressMonitor progressMonitor, int refreshMode) throws CouldNotImportProjectException {
		try {
			progressMonitor.setTaskName("Import project at " + directory.getAbsolutePath());
			IProject project = projectAlreadyExistsInWorkspace(directory);
			if (project == null) {
				project = createOrImportProject(directory, progressMonitor);
			}

			if (progressMonitor.isCanceled()) {
				return null;
			}
			project.open(refreshMode, progressMonitor);
			if (!this.report.containsKey(project)) {
				this.report.put(project, new ArrayList<ProjectConfigurator>());
			}
			if (this.listener != null) {
				this.listener.projectCreated(project);
			}
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
					// project seems already there
					return projectWithSameName;
				} else {
					throw new CouldNotImportProjectException(directory, NLS.bind(Messages.anotherProjectWithSameNameExists_description, expectedName));
				}
			}
		} else {
			String projectName = directory.getName();
			if (this.workspaceRoot.getProject(directory.getName()).exists()) {
				int i = 1;
				do {
					projectName = directory.getName() + "_(" + i + ")";
					i++;
				} while (this.workspaceRoot.getProject(projectName).exists());
			}
			
			desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
		}
		desc.setLocation(new Path(directory.getAbsolutePath()));
		IProject res = workspaceRoot.getProject(desc.getName());
		res.create(desc, progressMonitor);
		PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(res, this.workingSets);
		return res;
	}

	public IProject getRootProject() {
		return this.rootProject;
	}

	public Map<IProject, List<ProjectConfigurator>> getConfiguredProjects() {
		return this.report;
	}

	public Map<IPath, Exception> getErrors() {
		return this.errors;
	}

	public Map<File, List<ProjectConfigurator>> getImportProposals(IProgressMonitor monitor) {
		if (this.importProposals == null) {
			Map<File, List<ProjectConfigurator>> res = new HashMap<>();
			if (this.configurationManager == null) {
				this.configurationManager = new ProjectConfiguratorExtensionManager();
			}
			for (ProjectConfigurator configurator : configurationManager.getAllActiveProjectConfigurators(this.rootDirectory)) {
				Set<File> supportedFiles = configurator.findConfigurableLocations(EasymportJob.this.rootDirectory, monitor);
				if (supportedFiles != null) {
					for (File supportedFile : supportedFiles) {
						if (!res.containsKey(supportedFile)) {
							res.put(supportedFile,  new ArrayList<ProjectConfigurator>());
						}
						res.get(supportedFile).add(configurator);
					}
				}
			}
			this.importProposals = res;
		}
		return this.importProposals;
	}

	public boolean isDetectNestedProjects() {
		return this.deepChildrenDetection;
	}

	public void setDetectNestedProjects(boolean detectNestedProjects) {
		this.deepChildrenDetection = detectNestedProjects;
	}

	public void resetProposals() {
		this.importProposals = null;
	}

	public Set<File> getDirectoriesToImport() {
		return this.directoriesToImport;
	}
}
