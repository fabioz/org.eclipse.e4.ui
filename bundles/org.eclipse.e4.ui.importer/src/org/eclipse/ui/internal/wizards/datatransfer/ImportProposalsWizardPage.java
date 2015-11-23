/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class ImportProposalsWizardPage extends WizardPage implements IPageChangedListener {

	private CheckboxTreeViewer tree;
	private Set<File> alreadyExistingProjects;
	private Set<File> notAlreadyExistingProjects;
	private Button recurseInSelectedProjectsCheckbox;
	private EasymportJob currentJob;
	private Label selectionSummary;
	protected Map<File, List<ProjectConfigurator>> potentialProjects;

	private class FolderForProjectsLabelProvider extends CellLabelProvider implements IColorProvider {
		public String getText(Object o) {
			File file = (File)o;
			String label = file.getAbsolutePath();
			if (label.startsWith(currentJob.getRoot().getAbsolutePath())) {
				label = label.substring(currentJob.getRoot().getParentFile().getAbsolutePath().length() + 1); 
			}
			return label;
		}

		@Override
		public Color getBackground(Object o) {
			return null;
		}

		@Override
		public Color getForeground(Object o) {
			if (alreadyExistingProjects.contains(o)) {
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			}
			return null;
		}

		@Override
		public void update(ViewerCell cell) {
			cell.setText(getText(cell.getElement()));
			Color color = getForeground(cell.getElement());
			if (color != null) {
				cell.setForeground(color);
			}
		}
	}
	
	private class ProjectConfiguratorLabelProvider extends CellLabelProvider implements IColorProvider {
		public String getText(Object o) {
			File file = (File)o;
			if (alreadyExistingProjects.contains(file)) {
				return Messages.alreadyImportedAsProject_title;
			}
			List<ProjectConfigurator> configurators = ImportProposalsWizardPage.this.potentialProjects.get(file);
			if (configurators.isEmpty()) {
				return ""; //$NON-NLS-1$
			}
			return ProjectConfiguratorExtensionManager.getLabel(configurators.get(0));
		}

		@Override
		public Color getBackground(Object o) {
			return null;
		}

		@Override
		public Color getForeground(Object o) {
			if (alreadyExistingProjects.contains(o)) {
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			}
			return null;
		}

		@Override
		public void update(ViewerCell cell) {
			cell.setText(getText(cell.getElement()));
			Color color = getForeground(cell.getElement());
			if (color != null) {
				cell.setForeground(color);
			}
		}
	}
	
	public ImportProposalsWizardPage(EasymportWizard wizard) {
		super(ImportProposalsWizardPage.class.getName());
		setWizard(wizard);
	}

	@Override
	public void createControl(Composite parent) {
		setTitle(Messages.preliminaryDetection_Title);
		setDescription(Messages.preliminaryDetection_Description);

		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider)getContainer()).addPageChangedListener(this);
		}

		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(2, false));
		selectionSummary = new Label(res, SWT.NONE);
		selectionSummary.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		tree = new CheckboxTreeViewer(res, SWT.BORDER);
		tree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setContentProvider(new ITreeContentProvider() {
			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				Map<File, ?> potentialProjects = (Map<File, ?>)inputElement;
				return potentialProjects.keySet().toArray(new File[potentialProjects.size()]);
			}
			

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
			
		});
		tree.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer v, Object o1, Object o2) {
				return ((File)o1).getAbsolutePath().compareTo(((File)o2).getAbsolutePath());
			}
		});
		tree.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (ImportProposalsWizardPage.this.alreadyExistingProjects.contains(event.getElement())) {
					tree.setChecked(event.getElement(), false);
				} else {
					selectionChanged();
				}
			}
		});
		
		tree.getTree().setHeaderVisible(true);
		ViewerColumn pathColumn = new TreeViewerColumn(tree, SWT.NONE);
		pathColumn.setLabelProvider(new FolderForProjectsLabelProvider());
		tree.getTree().getColumn(0).setText(Messages.folder);
		tree.getTree().getColumn(0).setWidth(400);
		ViewerColumn projectTypeColumn = new TreeViewerColumn(tree, SWT.NONE);
		projectTypeColumn.setLabelProvider(new ProjectConfiguratorLabelProvider());
		tree.getTree().getColumn(1).setText(Messages.importAs);
		tree.getTree().getColumn(1).setWidth(250);
		
		Composite selectionButtonsGroup = new Composite(res, SWT.NONE);
		selectionButtonsGroup.setLayout(new GridLayout(1, false));
		selectionButtonsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Button selectAllButton = new Button(selectionButtonsGroup, SWT.PUSH);
		selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false));
		selectAllButton.setText(DataTransferMessages.DataTransfer_selectAll);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tree.setCheckedElements(ImportProposalsWizardPage.this.notAlreadyExistingProjects.toArray());
				selectionChanged();
			}
		});
		Button deselectAllButton = new Button(selectionButtonsGroup, SWT.PUSH);
		deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false));
		deselectAllButton.setText(DataTransferMessages.DataTransfer_deselectAll);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tree.setCheckedElements(new Object[0]);
				selectionChanged();
			}
		});
		
		recurseInSelectedProjectsCheckbox = new Button(res, SWT.CHECK);
		recurseInSelectedProjectsCheckbox.setText("Use additional analysis after import to detect nested project under selected projects\n(BEWARE: this may create new projects in your workspace without ability to review it first!)");
		recurseInSelectedProjectsCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false, 2, 1));
		recurseInSelectedProjectsCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getWizard().getImportJob().setDetectNestedProjects(recurseInSelectedProjectsCheckbox.getSelection());
			}
		});
		setPageComplete(true);
		setControl(res);
	}

	@Override
	public boolean isPageComplete() {
		return getWizard().getImportJob() != null &&
			getWizard().getImportJob().getDirectoriesToImport() != null &&
			!getWizard().getImportJob().getDirectoriesToImport().isEmpty();
	}


	@Override
	public void setWizard(IWizard easymportWizard) {
		Assert.isTrue(easymportWizard instanceof EasymportWizard);
		super.setWizard(easymportWizard);
	}
	
	public EasymportWizard getWizard() {
		return (EasymportWizard)super.getWizard();
	}

	private void updateTreeContent() {
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					ImportProposalsWizardPage.this.potentialProjects = getWizard().getImportJob().getImportProposals(monitor);
					if (potentialProjects.size() == 0) {
						MessageDialog.openInformation(getShell(),
								Messages.didntFindImportProposals_title,
								NLS.bind(Messages.didntFindImportProposals_message, recurseInSelectedProjectsCheckbox.getText()));
					} else {
						// if some projects were detected thanks to metadata, assume projects has necessary metadata for good import
						// and skip additional nested project detection.
						getWizard().getImportJob().setDetectNestedProjects(false);
					}
					recurseInSelectedProjectsCheckbox.setSelection(getWizard().getImportJob().isDetectNestedProjects());
					if (!potentialProjects.containsKey(getWizard().getImportJob().getRoot())) {
						potentialProjects.put(getWizard().getImportJob().getRoot(), Collections.EMPTY_LIST);
						// force nested detection when the import didn't manage to do something 
						getWizard().getImportJob().setDetectNestedProjects(true);
					}
					
					ImportProposalsWizardPage.this.notAlreadyExistingProjects = new HashSet<File>(potentialProjects.keySet());
					ImportProposalsWizardPage.this.alreadyExistingProjects = new HashSet<File>();
					for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
						ImportProposalsWizardPage.this.notAlreadyExistingProjects.remove(project.getLocation().toFile());
						ImportProposalsWizardPage.this.alreadyExistingProjects.add(project.getLocation().toFile());
					}
					tree.setInput(potentialProjects);
					tree.setCheckedElements(ImportProposalsWizardPage.this.notAlreadyExistingProjects.toArray());
				}
			});
			selectionChanged();
		} catch (InterruptedException ex) {
			tree.setInput(Collections.EMPTY_MAP);
			getWizard().getImportJob().resetProposals();
		} catch (InvocationTargetException ex) {
			MessageDialog.openError(getShell(), "Sorry...", "An internal error occured. Please see log file for details.");
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR,
					Activator.getDefault().getBundle().getSymbolicName(),
					ex.getMessage(),
					ex));
		}
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		if (event.getSelectedPage() == this) {
			EasymportJob newJob = getWizard().getImportJob();
			if (newJob != this.currentJob) {
				this.currentJob = newJob;
				recurseInSelectedProjectsCheckbox.setSelection(getWizard().getImportJob().isDetectNestedProjects());
				updateTreeContent();
			}
		}
	}
	
	@Override
	public void dispose() {
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider)getContainer()).removePageChangedListener(this);
		}
		super.dispose();
	}

	private void selectionChanged() {
		Object[] selected = tree.getCheckedElements();
		EasymportJob importJob = getWizard().getImportJob();
		Set<File> excludedDirectories = new HashSet(((Map<File, ?>)this.tree.getInput()).keySet());
		Set<File> selectedProjects = new HashSet<File>();
		for (Object item : selected) {
			File directory = (File)item;
			excludedDirectories.remove(directory);
			selectedProjects.add(directory);
		}
		importJob.setDirectoriesToImport(selectedProjects);
		importJob.setExcludedDirectories(excludedDirectories);
		selectionSummary.setText(NLS.bind(Messages.selectionSummary, importJob.getImportProposals(null).size(), importJob.getDirectoriesToImport().size()));
		setPageComplete(isPageComplete());
	}
}
