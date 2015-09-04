/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat Inc., and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *     Snjezana Peco (Red Hat Inc.)
 ******************************************************************************/
package org.eclipse.ui.internal.wizards.datatransfer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

public class EasymportWizard extends Wizard implements IImportWizard {

	private File initialSelection;
	private Set<IWorkingSet> initialWorkingSets = new HashSet<IWorkingSet>();
	private SelectImportRootWizardPage projectRootPage;
	private EasymportJob easymportJob;

	public EasymportWizard() {
		super();
		setNeedsProgressMonitor(true);
		setForcePreviousAndNextButtons(true);
		IDialogSettings dialogSettings = getDialogSettings();
		if (dialogSettings == null) {
			dialogSettings = Activator.getDefault().getDialogSettings();
			setDialogSettings(dialogSettings);
		}
		setDefaultPageImageDescriptor(IDEWorkbenchPlugin.getIDEImageDescriptor("wizban/newprj_wiz.png")); //$NON-NLS-1$
	}

	public void setInitialDirectory(File directory) {
		this.initialSelection = directory;
	}

	public void setInitialWorkingSets(Set<IWorkingSet> workingSets) {
		this.initialWorkingSets = workingSets;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if (selection != null) {
			for (Object item : selection.toList()) {
				File asFile = toFile(item);
				if (asFile != null && this.initialSelection == null) {
					this.initialSelection = asFile;
				} else {
					IWorkingSet asWorkingSet = toWorkingSet(item);
					if (asWorkingSet != null) {
						this.initialWorkingSets.add(asWorkingSet);
					}
				}
			}
		}
	}

	public static File toFile(Object o) {
		if (o instanceof File) {
			return (File)o;
		} else if (o instanceof IResource) {
			return ((IResource)o).getLocation().toFile();
		} else if (o instanceof IAdaptable) {
			IResource resource = ((IAdaptable)o).getAdapter(IResource.class);
			if (resource != null) {
				return resource.getLocation().toFile();
			}
		}
		return null;
	}

	private IWorkingSet toWorkingSet(Object o) {
		if (o instanceof IWorkingSet) {
			return (IWorkingSet)o;
		} else if (o instanceof IAdaptable) {
			return ((IAdaptable)o).getAdapter(IWorkingSet.class);
		}
		return null;
	}

	@Override
	public void addPages() {
		this.projectRootPage = new SelectImportRootWizardPage(this, this.initialSelection, this.initialWorkingSets);
		addPage(this.projectRootPage);
		addPage(new ImportProposalsWizardPage(this));
	}

	@Override
	public boolean performFinish() {
		getDialogSettings().put(SelectImportRootWizardPage.ROOT_DIRECTORY, projectRootPage.getSelectedRootDirectory().getAbsolutePath());
		EasymportJob job = getImportJob();
		EasymportJobReportDialog dialog = new EasymportJobReportDialog(getShell(), job);
		job.schedule();
		if (projectRootPage.isDetectNestedProject() || projectRootPage.isConfigureProjects()) {
			dialog.open();
		}
		return true;
	}
	
	public EasymportJob getImportJob() {
		if (this.projectRootPage.getSelectedRootDirectory() == null) {
			this.easymportJob = null;
		} else if (this.easymportJob == null || !this.easymportJob.getRoot().equals(this.projectRootPage.getSelectedRootDirectory())) {
			this.easymportJob = new EasymportJob(projectRootPage.getSelectedRootDirectory(), projectRootPage.getSelectedWorkingSets(), projectRootPage.isConfigureProjects(), projectRootPage.isDetectNestedProject());
		}
		return this.easymportJob;
	}

	@Override
	public boolean canFinish() {
		if (getContainer().getCurrentPage() == this.projectRootPage) {
			return this.projectRootPage.isPageComplete() && !this.projectRootPage.isDetectNestedProject();
		} else {
			return super.canFinish();
		}
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == this.projectRootPage && !this.projectRootPage.isDetectNestedProject()) {
			return null;
		}
		return super.getNextPage(page);
	}

}