/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat Inc., and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *     Aurelien Pupier (Bonitasoft S.A.) - bug fix 470024
 ******************************************************************************/
package org.eclipse.ui.internal.wizards.datatransfer;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class EasymportJobReportDialog extends Dialog {

	private EasymportJob job;
	private IJobChangeListener jobChangeListener;

	private StackLayout progressLayout;
	private Composite progressComposite;
	private ProgressBar progressBar;
	private Label completedStatusLabel;
	private Label abortedStatusLabel;
	private Button stopButton;
	private boolean cancel;

	public EasymportJobReportDialog(Shell shell, EasymportJob job) {
		super(shell);
		setShellStyle(SWT.RESIZE | SWT.MIN);
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
			public void done(final IJobChangeEvent jobEvent) {
				if (jobEvent.getJob() == EasymportJobReportDialog.this.job && getShell() != null) {
					getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							EasymportJobReportDialog.this.progressLayout.topControl.setVisible(false);
							if (!cancel) {
								EasymportJobReportDialog.this.progressLayout.topControl = completedStatusLabel;
							} else {
								EasymportJobReportDialog.this.progressLayout.topControl = abortedStatusLabel;
							}
							progressComposite.layout();
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
		final Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(2, false));
		res.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		//// Nested projects
		final Label nestedProjectsLabel = new Label(res, SWT.NONE);
		nestedProjectsLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
		nestedProjectsLabel.setText(NLS.bind(Messages.EasymportWizardPage_importedProjects, 0));

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
		GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
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
				IPath rootLocation = job.getRootProject().getLocation();
				IPath projectLocation = project.getLocation();
				return projectLocation.makeRelativeTo(rootLocation).toString();
			}
		});
		nestedProjectsTable.setInput(this.job.getConfiguredProjects());


		//// Errors
		final Label errorsLabel = new Label(res, SWT.NONE);
		GridData errorLabelLayoutData = new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1);
		errorLabelLayoutData.exclude = true;
		errorsLabel.setLayoutData(errorLabelLayoutData);
		errorsLabel.setText(NLS.bind(Messages.EasymportWizardPage_importErrors, 0));

		final TableViewer errorsTable = new TableViewer(res);
		errorsTable.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object root) {
				return ((Map<IPath, Exception>)root).entrySet().toArray();
			}
		});
		errorsTable.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object o1, Object o2) {
				IPath location1 = ((Entry<IPath, Exception>) o1).getKey();
				IPath location2 = ((Entry<IPath, Exception>) o2).getKey();
				return location1.toString().compareTo(location2.toString());
			}
		});
		errorsTable.setFilters(new ViewerFilter[] { new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				Entry<IPath, Exception> entry = (Entry<IPath, Exception>) element;
				return job.getRootProject().getLocation().isPrefixOf(entry.getKey());
			}
		} });
		errorsTable.getTable().setHeaderVisible(true);
		GridData errorTableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		errorTableLayoutData.heightHint = 100;
		errorTableLayoutData.exclude = true;
		errorsTable.getControl().setLayoutData(errorTableLayoutData);

		TableViewerColumn errorRelativePathColumn = new TableViewerColumn(errorsTable, SWT.LEFT);
		errorRelativePathColumn.getColumn().setText(Messages.EasymportWizardPage_relativePath);
		errorRelativePathColumn.getColumn().setWidth(300);
		errorRelativePathColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IPath rootLocation = job.getRootProject().getLocation();
				IPath projectLocation = ((Entry<IPath, Exception>)element).getKey();
				return projectLocation.makeRelativeTo(rootLocation).toString();
			}
		});
		TableViewerColumn errorColumn = new TableViewerColumn(errorsTable, SWT.LEFT);
		errorColumn.getColumn().setText(Messages.EasymportWizardPage_error);
		errorColumn.getColumn().setWidth(500);
		errorColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Entry<IPath, Exception>)element).getValue().getMessage();
			}
		});
		errorsTable.setInput(this.job.getErrors());



		RecursiveImportListener tableReportFiller = new RecursiveImportListener() {
			@Override
			public void projectCreated(IProject project) {
				nestedProjectsTable.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						nestedProjectsTable.refresh();
						nestedProjectsTable.getTable().update();
						nestedProjectsTable.getTable().redraw();
						nestedProjectsLabel.setText(NLS.bind(Messages.EasymportWizardPage_importedProjects, job.getConfiguredProjects().size()));
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

			@Override
			public void errorHappened(IPath location, Exception error) {
				errorsTable.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						GridData gridData = (GridData)errorsTable.getControl().getLayoutData();
						if (gridData.exclude) {
							gridData.exclude = false;
							((GridData)errorsLabel.getLayoutData()).exclude = false;
						}
						errorsTable.refresh();
						errorsTable.getTable().update();
						errorsLabel.setText(NLS.bind(Messages.EasymportWizardPage_importErrors, job.getErrors().size()));
						res.layout(true);
					}
				});
			}
		};
		this.job.setListener(tableReportFiller);

		this.progressComposite = new Composite(res, SWT.NONE);
		progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		this.progressLayout = new StackLayout();
		progressComposite.setLayout(progressLayout);
		this.progressBar = new ProgressBar(progressComposite, SWT.SMOOTH | SWT.INDETERMINATE);
		this.progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		this.progressBar.setToolTipText(Messages.EasymportWizardPage_progressBarTooltip);
		this.completedStatusLabel = new Label(progressComposite, SWT.NONE);
		completedStatusLabel.setText("Completed");
		this.abortedStatusLabel = new Label(progressComposite, SWT.NONE);
		abortedStatusLabel.setText("Aborted");
		progressLayout.topControl = this.progressBar;
		this.stopButton = new Button(res, SWT.PUSH);
		stopButton.setToolTipText("Abort");
		stopButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EasymportJobReportDialog.this.cancel = true;
				job.cancel();
			}
		});
		stopButton.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, false));
		stopButton.setImage(WorkbenchImages.getImage(ISharedImages.IMG_ELCL_STOP));
		return res;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, OK, "OK", true).addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				close();
			}
		});
		updateButtons();
	}

	private void updateButtons() {
		this.stopButton.setEnabled(this.job.getResult() == null);
		getButton(OK).setEnabled(this.job.getResult() != null);
	}

	@Override
	public boolean close() {
		Job.getJobManager().removeJobChangeListener(this.jobChangeListener);
		return super.close();
	}
}
