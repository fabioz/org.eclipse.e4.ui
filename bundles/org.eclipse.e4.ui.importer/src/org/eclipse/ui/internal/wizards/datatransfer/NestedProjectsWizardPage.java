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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class NestedProjectsWizardPage extends WizardPage {
	
	private boolean detectNestedProjects = true;
	private RecursiveImportListener tableReportFiller;
	private IProject rootProject;
	private Map<IProject, List<ProjectConfigurator>> processedProjects = new HashMap<IProject, List<ProjectConfigurator>>();
	private SelectImportRootWizardPage projectRootPage;
	private TableViewer nestedProjectsTable;
	
	public NestedProjectsWizardPage(IWizard wizard, SelectImportRootWizardPage projectRootPage) {
		super(NestedProjectsWizardPage.class.getName());
		setWizard(wizard);
		this.projectRootPage = projectRootPage;
	}
	
	@Override
	public void createControl(Composite parent) {
		setTitle(Messages.EasymportWizardPage_nestedProjects);
		setDescription(Messages.EasymportWizardPage_detectNestedProjects);
		setImageDescriptor(Activator.imageDescriptorFromPlugin(Activator.getDefault().getBundle().getSymbolicName(), "pics/wizban/nestedProjects.png")); //$NON-NLS-1$
		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(1, false));
		final Button detectNestedProjectCheckbox = new Button(res, SWT.CHECK);
		detectNestedProjectCheckbox.setText(Messages.EasymportWizardPage_detectNestedProjects);
		detectNestedProjectCheckbox.setSelection(this.detectNestedProjects);
		detectNestedProjectCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selection = detectNestedProjectCheckbox.getSelection();
				NestedProjectsWizardPage.this.detectNestedProjects = selection;
				setPageComplete(isPageComplete());
			}
		});
		
		new Label(res, SWT.NONE).setText(Messages.EasymportWizardPage_importedProjects);
		nestedProjectsTable = new TableViewer(res);
		nestedProjectsTable.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public Object[] getElements(Object root) {
				return ((Map<IProject, List<IContentProvider>>)root).entrySet().toArray();
			}
		});
		nestedProjectsTable.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object o1, Object o2) {
				IProject project1 = ((Entry<IProject, List<ProjectConfigurator>>) o1).getKey();
				IProject project2 = ((Entry<IProject, List<ProjectConfigurator>>) o2).getKey();
				return project1.getLocation().toString().compareTo(project2.getLocation().toString());
			}
		});
		nestedProjectsTable.setFilters(new ViewerFilter[] { new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				Entry<IProject, List<ProjectConfigurator>> entry = (Entry<IProject, List<ProjectConfigurator>>) element;
				return NestedProjectsWizardPage.this.rootProject.getLocation().isPrefixOf(entry.getKey().getLocation());
			}
		} });
		nestedProjectsTable.getTable().setHeaderVisible(true);
		nestedProjectsTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewerColumn projectColumn = new TableViewerColumn(nestedProjectsTable, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		projectColumn.getColumn().setWidth(200);
		projectColumn.getColumn().setText(Messages.EasymportWizardPage_project);
		projectColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Entry<IProject, List<ProjectConfigurator>>)element).getKey().getName();
			}
		});
		
		TableViewerColumn configuratorsColumn = new TableViewerColumn(nestedProjectsTable, SWT.NONE);
		configuratorsColumn.getColumn().setWidth(300);
		configuratorsColumn.getColumn().setText(Messages.EasymportWizardPage_natures);
		configuratorsColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				StringBuilder builder = new StringBuilder();
				for (ProjectConfigurator configurator : ((Entry<IProject, List<ProjectConfigurator>>)element).getValue()) {
					builder.append(ProjectConfiguratorExtensionManager.getLabel(configurator));
					builder.append(", ");
				};
				if (builder.length() > 0) {
					builder.delete(builder.length() - 2, builder.length());
				}
				return builder.toString();
			}
		});
		
		TableViewerColumn relativePathColumn = new TableViewerColumn(nestedProjectsTable, SWT.LEFT);
		relativePathColumn.getColumn().setText(Messages.EasymportWizardPage_relativePath);
		relativePathColumn.getColumn().setWidth(300);
		relativePathColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IProject project = ((Entry<IProject, List<ProjectConfigurator>>)element).getKey();
				return project.getLocation().removeFirstSegments(NestedProjectsWizardPage.this.rootProject.getLocation().segmentCount()).toString();
			}
		});

		nestedProjectsTable.setInput(this.processedProjects);
		this.tableReportFiller = new RecursiveImportListener() {
			@Override
			public void projectCreated(IProject project) {
				if (!NestedProjectsWizardPage.this.processedProjects.containsKey(project)) {
					NestedProjectsWizardPage.this.processedProjects.put(project, new ArrayList<ProjectConfigurator>());
					nestedProjectsTable.getControl().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							getShell().layout(true);
						}
					});
				}
			}
			
			@Override
			public void projectConfigured(IProject project, ProjectConfigurator configurator) {
				NestedProjectsWizardPage.this.processedProjects.get(project).add(configurator);
				nestedProjectsTable.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						nestedProjectsTable.refresh();
						nestedProjectsTable.getTable().update();
						nestedProjectsTable.getTable().redraw();
					}
				});
			}
		};
		
		Link showDetectorsLink = new Link(res, SWT.NONE);
		showDetectorsLink.setText("<A>Show available detectors that will be used to detect and configure nested projects.</A>");
		showDetectorsLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				StringBuilder message = new StringBuilder();
				for (String extensionLabel : ProjectConfiguratorExtensionManager.getAllExtensionLabels()) {
					message.append(extensionLabel);
					message.append('\n');
				}
				MessageDialog.openInformation(getShell(), "Available detectors and configurators", message.toString());
			}
		});
		
		
		setControl(res);
	}
	
	@Override
	public boolean canFlipToNextPage() {
		return mustProcessCurrentProject();
	}
	

	/**
	 * @return
	 */
	public boolean mustProcessCurrentProject() {
		return this.detectNestedProjects && !this.processedProjects.containsKey(this.rootProject);
	}
	
	@Override
	public NestedProjectsWizardPage getNextPage() {
		if (mustProcessCurrentProject()) {
			performNestedImport();
			return this;
		} else {
			return null;
		}
	}

	/**
	 * 
	 */
	public void performNestedImport() {
		if (rootProjectChanged() || mustProcessCurrentProject()) {
			final Set<IWorkingSet> workingSets = this.projectRootPage.getSelectedWorkingSets();
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							if (rootProjectChanged()) {
								NestedProjectsWizardPage.this.rootProject = new OpenFolderCommand().toExistingOrNewProject(
										NestedProjectsWizardPage.this.projectRootPage.getSelectedRootDirectory(),
										NestedProjectsWizardPage.this.projectRootPage.getSelectedWorkingSets(),
										monitor);
							}
							if (mustProcessCurrentProject()) {
						        IWorkspace workspace = ResourcesPlugin.getWorkspace();
						        IWorkspaceDescription description = workspace.getDescription();
						        boolean isAutoBuilding = workspace.isAutoBuilding();
						        if (isAutoBuilding) {
						        	description.setAutoBuilding(false);
						        	workspace.setDescription(description);
						        }
						        
								new OpenFolderCommand().importProjectAndChildrenRecursively(NestedProjectsWizardPage.this.rootProject, true, workingSets, monitor, tableReportFiller);
								
								if (isAutoBuilding) {
									description.setAutoBuilding(true);
						        	workspace.setDescription(description);
								}
							}
						} catch (Exception ex) {
							throw new InvocationTargetException(ex);
						}
					}
				});
			} catch (Exception ex) {
				Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
			}
			getWizard().getContainer().updateButtons();
		}
	}

	public RecursiveImportListener getImportListener() {
		return this.tableReportFiller;
	}
	
	@Override
	public void setVisible(boolean visible) {
		if (visible && rootProjectChanged()) {
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							NestedProjectsWizardPage.this.rootProject = new OpenFolderCommand().toExistingOrNewProject(
									NestedProjectsWizardPage.this.projectRootPage.getSelectedRootDirectory(),
									NestedProjectsWizardPage.this.projectRootPage.getSelectedWorkingSets(),
									monitor);
						} catch (CouldNotImportProjectException ex) {
							throw new InvocationTargetException(ex);
						}
					}
				});
				if (nestedProjectsTable != null && !nestedProjectsTable.getTable().isDisposed()) {
					nestedProjectsTable.refresh();
					nestedProjectsTable.getTable().update();
					nestedProjectsTable.getTable().redraw();
				}
			} catch (InterruptedException ex) {
				// Ignore
			} catch (InvocationTargetException ex) {
				Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), ex.getMessage(), ex));
			}
			// TODO Update UI
		}
		super.setVisible(visible);
	}

	/**
	 * @return
	 */
	public boolean rootProjectChanged() {
		return this.rootProject == null ||
				!this.rootProject.getLocation().toFile().getAbsoluteFile().equals(this.projectRootPage.getSelectedRootDirectory());
	}
}
