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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class EasymportJobReportDialog extends Dialog {
	
	private EasymportJob job;
	private IJobChangeListener jobChangeListener;
	
	public EasymportJobReportDialog(Shell shell, EasymportJob job) {
		super(shell);
		setShellStyle(SWT.RESIZE);
		this.job = job;
		jobChangeListener = new IJobChangeListener() {
			@Override
			public void sleeping(IJobChangeEvent arg0) {
			}
			
			@Override
			public void scheduled(IJobChangeEvent arg0) {
			}
			
			@Override
			public void running(IJobChangeEvent arg0) {
			}
			
			@Override
			public void done(IJobChangeEvent job) {
				if (job.getJob() == EasymportJobReportDialog.this.job && getShell() != null) {
					getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							updateButtons();		
						}
					});
				}
			}
			
			@Override
			public void awake(IJobChangeEvent arg0) {
			}
			
			@Override
			public void aboutToRun(IJobChangeEvent arg0) {
			}
		};
		Job.getJobManager().addJobChangeListener(jobChangeListener);
	}
	
	@Override
	public Composite createDialogArea(Composite parent) {
		getShell().setText(Messages.EasymportWizardPage_nestedProjects);
//		setDescription(Messages.EasymportWizardPage_detectNestedProjects);
//		setImageDescriptor(Activator.imageDescriptorFromPlugin(Activator.getDefault().getBundle().getSymbolicName(), "pics/wizban/nestedProjects.png")); //$NON-NLS-1$
		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(1, false));
		res.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		new Label(res, SWT.NONE).setText(Messages.EasymportWizardPage_importedProjects);
		final TableViewer nestedProjectsTable = new TableViewer(res);
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
				return job.getRootProject().getLocation().isPrefixOf(entry.getKey().getLocation());
			}
		} });
		nestedProjectsTable.getTable().setHeaderVisible(true);
		GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableLayoutData.heightHint = 400;
		nestedProjectsTable.getControl().setLayoutData(tableLayoutData);
		
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
				return project.getLocation().removeFirstSegments(job.getRootProject().getLocation().segmentCount()).toString();
			}
		});

		nestedProjectsTable.setInput(this.job.getConfiguredProjects());
		RecursiveImportListener tableReportFiller = new RecursiveImportListener() {
			@Override
			public void projectCreated(IProject project) {
				nestedProjectsTable.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						getShell().layout(true);
					}
				});
			}
			
			@Override
			public void projectConfigured(IProject project, ProjectConfigurator configurator) {
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
		this.job.setListener(tableReportFiller);
		
		ProgressMonitorPart progressMonitorPart = new ProgressMonitorPart(res, null);
		
		return res;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, CANCEL, "Abort", false).addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				job.cancel();
				close();
			}
		});
		createButton(parent, OK, "OK", true).addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				close();
			}
		});
		updateButtons();
	}
	
	private void updateButtons() {
		getButton(OK).setEnabled(this.job.getResult() != null);
		getButton(CANCEL).setEnabled(this.job.getResult() == null);
	}

	@Override
	public boolean close() {
		Job.getJobManager().removeJobChangeListener(this.jobChangeListener);
		return super.close();
	}
}
