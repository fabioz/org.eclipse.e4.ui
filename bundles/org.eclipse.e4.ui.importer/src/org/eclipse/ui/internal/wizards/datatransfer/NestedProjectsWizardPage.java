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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkingSet;

public class NestedProjectsWizardPage extends WizardPage {
	
	private boolean detectNestedProjects = true;
	private Set<IProject> processedProjects = new HashSet<IProject>();
	
	protected NestedProjectsWizardPage(EasymportWizard wizard) {
		super(NestedProjectsWizardPage.class.getName());
		setWizard(wizard);
	}

	@Override
	public EasymportWizard getWizard() {
		return (EasymportWizard)super.getWizard();
	}
	
	@Override
	public void createControl(Composite parent) {
		setTitle(Messages.EasymportWizardPage_nestedProjects);
		setDescription(Messages.EasymportWizardPage_detectNestedProjects);
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
		new Label(res, SWT.NONE).setText("The following detectors will be used to detect and configure nested projects.");
		ListViewer detectorsList = new ListViewer(res);
		detectorsList.setContentProvider(new ArrayContentProvider());
		detectorsList.setLabelProvider(new LabelProvider());
		detectorsList.setInput(ProjectConfiguratorExtensionManager.getAllExtensionLabels());
		detectorsList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false));
		new Label(res, SWT.NONE).setText(Messages.EasymportWizardPage_importedProjects);
		TableViewer nestedProject = new TableViewer(res);
		nestedProject.getControl().setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false));
		setControl(res);
	}
	
	@Override
	public boolean canFlipToNextPage() {
		return mustProcessProject();
	}

	/**
	 * @return
	 */
	public boolean mustProcessProject() {
		return this.detectNestedProjects && !this.processedProjects.contains(getWizard().getProject());
	}
	
	@Override
	public NestedProjectsWizardPage getNextPage() {
		if (mustProcessProject()) {
			final Set<IWorkingSet> workingSets = getWizard().getSelectedWorkingSets();
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							Set<IProject> newProjects = new OpenFolderCommand().importProjectAndChildrenRecursively(getWizard().getProject(), true, workingSets, monitor);
							NestedProjectsWizardPage.this.processedProjects.add(getWizard().getProject());
							NestedProjectsWizardPage.this.processedProjects.addAll(newProjects);
						} catch (Exception ex) {
							throw new InvocationTargetException(ex);
						}
					}
				});
			} catch (Exception ex) {
				Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
			}
			getWizard().getContainer().updateButtons();
			return this;
		} else {
			return null;
		}
	}
}
