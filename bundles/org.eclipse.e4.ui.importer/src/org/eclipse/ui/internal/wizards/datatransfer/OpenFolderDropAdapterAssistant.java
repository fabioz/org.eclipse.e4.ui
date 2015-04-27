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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

public class OpenFolderDropAdapterAssistant extends CommonDropAdapterAssistant {

	public OpenFolderDropAdapterAssistant() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return FileTransfer.getInstance().isSupportedType(aTransferType);
	}

	@Override
	public IStatus validateDrop(Object target, int operation, TransferData transferType) {
		if (target instanceof IWorkingSet || target instanceof IWorkspaceRoot) {
			return Status.OK_STATUS;
		} else if (target instanceof IAdaptable) {
			IAdaptable targetAdaptable = (IAdaptable)target;
			if (targetAdaptable.getAdapter(IWorkspaceRoot.class) != null || targetAdaptable.getAdapter(IWorkingSet.class) != null) {
				return Status.OK_STATUS;
			}
		}
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter aDropAdapter, DropTargetEvent aDropTargetEvent, Object aTarget) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		String[] files = (String[]) aDropTargetEvent.data;
		// Currently only support single directory
		if (files.length != 1) {
			return Status.CANCEL_STATUS;
		}
		File directory = new File(files[0]);
		if (!directory.isDirectory()) {
			return Status.CANCEL_STATUS;
		}
		IWorkingSet targetWorkingSet = null;
		if (aTarget != null) {
			if (aTarget instanceof IWorkingSet) {
				targetWorkingSet = (IWorkingSet)aTarget;
			} else if (aTarget instanceof IAdaptable) {
				targetWorkingSet = (IWorkingSet) ((IAdaptable)aTarget).getAdapter(IWorkingSet.class);
			}
		}
		Set<IWorkingSet> workingSets = new HashSet<IWorkingSet>();
		workingSets.add(targetWorkingSet);
		EasymportWizard wizard = new EasymportWizard();
		wizard.setInitialDirectory(directory);
		Set<IWorkingSet> initialWorkingSets = new HashSet<IWorkingSet>();
		if (targetWorkingSet != null) {
			initialWorkingSets.add(targetWorkingSet);
		} else {
			// inherit workingSets
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			final Path asPath = new Path(directory.getAbsolutePath());
			IProject parentProject = null;
			for (IProject project : workspaceRoot.getProjects()) {
				if (project.getLocation().isPrefixOf(asPath) && (parentProject == null || parentProject.getLocation().isPrefixOf(project.getLocation())) ) {
					parentProject = project;
				}
			}
			if (parentProject != null) {
				for (IWorkingSet workingSet : workbench.getWorkingSetManager().getAllWorkingSets()) {
					for (IAdaptable element : workingSet.getElements()) {
						if (element.equals(parentProject)) {
							initialWorkingSets.add(workingSet);
						}
					}
				}
			}
		}
		wizard.setInitialWorkingSets(initialWorkingSets);
		WizardDialog wizardDialog = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), wizard);
		wizardDialog.setBlockOnOpen(false);
		wizardDialog.open();
		return Status.OK_STATUS;
	}

}
