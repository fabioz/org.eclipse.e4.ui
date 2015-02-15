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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;

public class EasymportWizard extends Wizard implements IImportWizard {

	private File initialSelection;
	private Set<IWorkingSet> initialWorkingSets = new HashSet<IWorkingSet>();
	private EasymportWizardPage page;

	public EasymportWizard() {
		super();
		setNeedsProgressMonitor(true);
		setForcePreviousAndNextButtons(true);
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
			IResource resource = (IResource) ((IAdaptable)o).getAdapter(IResource.class);
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
			return (IWorkingSet) ((IAdaptable)o).getAdapter(IWorkingSet.class);
		}
		return null;
	}

	@Override
	public void addPages() {
		this.page = new EasymportWizardPage(this, this.initialSelection, this.initialWorkingSets);
		addPage(this.page);
	}
	
	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof ImportReportWizardPage) {
			// not sure how/whether to support "Back" at that time
			return null;
		}
		return super.getPreviousPage(page);
	}
	
	@Override
	public boolean canFinish() {
		return !getContainer().getCurrentPage().canFlipToNextPage();
	}
	
	@Override
	public boolean performFinish() {
		getDialogSettings().put(EasymportWizardPage.ROOT_DIRECTORY, page.getSelectedRootDirectory().getAbsolutePath());
		return true;
	}
	
	@Override
	public IDialogSettings getDialogSettings() {
		IDialogSettings dialogSettings = super.getDialogSettings();
		if (dialogSettings == null) {
			dialogSettings = Activator.getDefault().getDialogSettings();
			setDialogSettings(dialogSettings);
		}
		return dialogSettings;
	}

	public IProject getProject() {
		return this.page.getProject();
	}

	public Set<IWorkingSet> getSelectedWorkingSets() {
		return this.page.getSelectedWorkingSets();
	}
}
