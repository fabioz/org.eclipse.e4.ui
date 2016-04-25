/*******************************************************************************
 * Copyright (c) 2007, 2016 Gunnar Wagenknecht and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mickael Istria (Red Hat Inc) - [102527] Reshaped UI, based on builders
 ******************************************************************************/
package org.eclipse.ui.resources.natures;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.log.Logger;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Project property page for viewing and modifying the project natures.
 *
 * @since 3.3
 */
public class ProjectNaturesPage extends PropertyPage {

	private static final String PLUGIN_ID = "org.eclipse.e4.ui.naturist"; //$NON-NLS-1$
	public static final String HELP_CONTEXT_ID = PLUGIN_ID + ".project_natures_property_page_context"; //$NON-NLS-1$
	public static final String ID = "org.eclipse.ui.propertypages.project.natures"; //$NON-NLS-1$

	private IProject project;
	private List<String> naturesIdsWorkingCopy;

	// widgets
	private TableViewer activeNaturesList;

	private boolean warningAlreadyShown = false;
	private Logger logger;

	/**
	 * @see PreferencePage#createContents
	 */
	@Override
	protected Control createContents(final Composite parent) {
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		BundleContext bundleContext = bundle.getBundleContext();
		logger = bundleContext.getService(bundleContext.getServiceReference(Logger.class));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), HELP_CONTEXT_ID);
		Font font = parent.getFont();

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setFont(font);

		initialize();

		Composite header = new Composite(composite, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		Label description = createDescriptionLabel(header);
		description.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		header.setLayout(new GridLayout(2, false));
		Label warningImageLabel = new Label(header, SWT.NONE);
		warningImageLabel.setImage(header.getDisplay().getSystemImage(SWT.ICON_WARNING));
		Label warningLabel = new Label(header, SWT.WRAP);
		warningLabel.setText(Messages.ProjectNaturesPage_warningMessage);
		GridData warningMessageLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		warningMessageLayoutData.widthHint = 400;
		warningMessageLayoutData.verticalIndent = 10;
		warningLabel.setLayoutData(warningMessageLayoutData);

		Composite naturesComposite = new Composite(composite, SWT.NONE);
		naturesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		naturesComposite.setLayout(new GridLayout(2, false));
		this.activeNaturesList = new TableViewer(naturesComposite);
		this.activeNaturesList.getTable().setFont(font);
		this.activeNaturesList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		this.activeNaturesList.setLabelProvider(new NatureLabelProvider(this.project.getWorkspace()));
		this.activeNaturesList.setContentProvider(new ArrayContentProvider());
		try {
			this.naturesIdsWorkingCopy = new ArrayList<>();
			this.naturesIdsWorkingCopy.addAll(Arrays.asList(project.getDescription().getNatureIds()));
		} catch (CoreException ex) {
			logger.log(IStatus.WARNING, "Error while loading project description for " + this.project.getName(), //$NON-NLS-1$
					ex);
		}
		this.activeNaturesList.setInput(this.naturesIdsWorkingCopy);

		Composite buttonComposite = new Composite(naturesComposite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(1, false));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addButton.setText(Messages.ProjectNaturesPage_addNature);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!ProjectNaturesPage.this.warningAlreadyShown) {
					if (!MessageDialog.openConfirm(getShell(), Messages.ProjectNaturesPage_changeWarningTitle,
							Messages.ProjectNaturesPage_warningMessage + "\n\n" //$NON-NLS-1$
									+ Messages.ProjectNaturesPage_changeWarningQuestion)) {
						return;
					}
					ProjectNaturesPage.this.warningAlreadyShown = true;
				}
				ElementListSelectionDialog naturesSelectionDialog = new ElementListSelectionDialog(parent.getShell(),
						new NatureLabelProvider(project.getWorkspace()));
				naturesSelectionDialog.setMessage(Messages.ProjectNaturesPage_selectNatureToAddMessage);
				naturesSelectionDialog.setTitle(Messages.ProjectNaturesPage_selectNatureToAddTitle);
				List<IProjectNatureDescriptor> natures = new ArrayList<>();
				for (IProjectNatureDescriptor nature : project.getWorkspace().getNatureDescriptors()) {
					if (!naturesIdsWorkingCopy.contains(nature.getNatureId())) {
						natures.add(nature);
					}
				}
				naturesSelectionDialog.setElements(natures.toArray(new IProjectNatureDescriptor[natures.size()]));
				if (naturesSelectionDialog.open() == Window.OK) {
					for (Object item : naturesSelectionDialog.getResult()) {
						IProjectNatureDescriptor nature = (IProjectNatureDescriptor) item;
						naturesIdsWorkingCopy.add(nature.getNatureId());
					}
					ProjectNaturesPage.this.activeNaturesList.refresh();
				}
			}
		});
		final Button removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeButton.setText(Messages.ProjectNaturesPage_removeNature);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!ProjectNaturesPage.this.warningAlreadyShown) {
					if (!MessageDialog.openConfirm(getShell(), Messages.ProjectNaturesPage_changeWarningTitle,
							Messages.ProjectNaturesPage_warningMessage + "\n\n" //$NON-NLS-1$
									+ Messages.ProjectNaturesPage_changeWarningQuestion)) {
						return;
					}
					ProjectNaturesPage.this.warningAlreadyShown = true;
				}
				IStructuredSelection selection = (IStructuredSelection) ProjectNaturesPage.this.activeNaturesList
						.getSelection();
				for (Object item : selection.toList()) {
					String natureId = (String) item;
					naturesIdsWorkingCopy.remove(natureId);
				}
				ProjectNaturesPage.this.activeNaturesList.refresh();
			}
		});
		this.activeNaturesList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				removeButton.setEnabled(!ProjectNaturesPage.this.activeNaturesList.getSelection().isEmpty());
			}
		});
		this.activeNaturesList.setSelection(new StructuredSelection()); // Empty
																		// selection

		return composite;
	}

	private static class NatureLabelProvider extends LabelProvider {
		private IWorkspace workspace;
		private Map<String, Image> natureImages;

		public NatureLabelProvider(IWorkspace workspace) {
			this.workspace = workspace;
			this.natureImages = new HashMap<>(workspace.getNatureDescriptors().length);
		}

		@Override
		public String getText(Object element) {
			IProjectNatureDescriptor nature = null;
			if (element instanceof IProjectNatureDescriptor) {
				nature = (IProjectNatureDescriptor) element;
			} else if (element instanceof String) {
				String natureId = (String) element;
				nature = this.workspace.getNatureDescriptor(natureId);
				if (nature == null) {
					return getMissingNatureLabel(natureId);
				}
			} else {
				return "Not a valid nature input " + element.toString(); //$NON-NLS-1$
			}
			return getNatureDescriptorLabel(nature);
		}

		@Override
		public Image getImage(Object element) {
			String natureId = null;
			if (element instanceof IProjectNatureDescriptor) {
				natureId = ((IProjectNatureDescriptor) element).getNatureId();
			} else if (element instanceof String) {
				natureId = (String) element;
			} else {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
			if (this.workspace.getNatureDescriptor(natureId) != null) {
				if (!natureImages.containsKey(natureId)) {
					ImageDescriptor image = IDEWorkbenchPlugin.getDefault().getProjectImageRegistry()
							.getNatureImage(natureId);
					if (image != null) {
						this.natureImages.put(natureId, image.createImage());
					} else {
						// TODO a generic image?
					}
				}
				return natureImages.get(natureId);
			} else /* Unknown nature */ {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			}

		}

		protected String getMissingNatureLabel(String natureId) {
			return NLS.bind(Messages.ProjectNaturesPage_missingNatureText, natureId);
		}

		protected String getNatureDescriptorLabel(IProjectNatureDescriptor natureDescriptor) {
			String label = natureDescriptor.getLabel();
			if (label.trim().length() == 0)
				return natureDescriptor.getNatureId();
			return label;
		}

		@Override
		public void dispose() {
			for (Image image : natureImages.values()) {
				image.dispose();
			}
			super.dispose();
		}

	}

	/**
	 * Handle the exception thrown when saving.
	 *
	 * @param e
	 *            the exception
	 */
	protected void handle(InvocationTargetException e) {
		IStatus error;
		Throwable target = e.getTargetException();
		if (target instanceof CoreException) {
			error = ((CoreException) target).getStatus();
		} else {
			String msg = target.getMessage();
			if (msg == null) {
				msg = IDEWorkbenchMessages.Internal_error;
			}
			error = new Status(IStatus.ERROR, IDEWorkbenchPlugin.IDE_WORKBENCH, 1, msg, target);
		}
		ErrorDialog.openError(getControl().getShell(), null, null, error);
	}

	/**
	 * Initializes a ProjectReferencePage.
	 */
	private void initialize() {
		project = (IProject) getElement().getAdapter(IResource.class);
		noDefaultAndApplyButton();
		setDescription(Messages.ProjectNaturesPage_label);
	}

	/**
	 * @see PreferencePage#performOk
	 */
	@Override
	public boolean performOk() {
		List<String> originalNatureIds = null;
		try {
			originalNatureIds = Arrays.asList(this.project.getDescription().getNatureIds());
		} catch (CoreException ex) {
			logger.log(IStatus.WARNING, "Error while loading project description for " + this.project.getName(), //$NON-NLS-1$
					ex);
			originalNatureIds = new ArrayList<>();
		}
		if (this.naturesIdsWorkingCopy.size() == originalNatureIds.size()
				&& this.naturesIdsWorkingCopy.containsAll(originalNatureIds)) {
			return true;
		}

		// set nature ids
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {

				try {
					IProjectDescription description = project.getDescription();
					description.setNatureIds(ProjectNaturesPage.this.naturesIdsWorkingCopy
							.toArray(new String[ProjectNaturesPage.this.naturesIdsWorkingCopy.size()]));
					project.setDescription(description, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			new ProgressMonitorJobsDialog(getControl().getShell()).run(true, true, runnable);
		} catch (InterruptedException e) {
			// Ignore interrupted exceptions
		} catch (InvocationTargetException e) {
			handle(e);
			return false;
		}
		return true;
	}
}
