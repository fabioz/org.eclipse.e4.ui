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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

public class SelectImportRootWizardPage extends WizardPage {

	public static final String ROOT_DIRECTORY = "rootDirectory";

	private File selection;
	private boolean detectNestedProjects = true;
	private boolean configureProjects = true;
	private Set<IWorkingSet> workingSets;
	private ControlDecoration rootDirectoryTextDecorator;
	private WorkingSetConfigurationBlock workingSetsBlock;

	private Text rootDirectoryText;

	public SelectImportRootWizardPage(IWizard wizard, File initialSelection, Set<IWorkingSet> initialWorkingSets) {
		super(EasymportWizard.class.getName());
		this.selection = initialSelection;
		this.workingSets = initialWorkingSets;
		if (this.workingSets == null) {
			this.workingSets = new HashSet<>();
		}
		setWizard(wizard);
	}

	@Override
	public void createControl(Composite parent) {
		setTitle(Messages.EasymportWizardPage_importProjectsInFolderTitle);
		setDescription(Messages.EasymportWizardPage_importProjectsInFolderDescription);
		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(3, false));
		Label rootDirectoryLabel = new Label(res, SWT.NONE);
		rootDirectoryLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		rootDirectoryLabel.setText(Messages.EasymportWizardPage_selectRootDirectory);
		rootDirectoryText = new Text(res, SWT.BORDER);
		rootDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		rootDirectoryText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				SelectImportRootWizardPage.this.selection = new File( ((Text)e.widget).getText() );
				SelectImportRootWizardPage.this.validatePage();
			}
		});
		this.rootDirectoryTextDecorator = new ControlDecoration(rootDirectoryText, SWT.TOP | SWT.LEFT);
		Image errorImage = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
		this.rootDirectoryTextDecorator.setImage(errorImage);
		this.rootDirectoryTextDecorator.setDescriptionText(Messages.EasymportWizardPage_incorrectRootDirectory);
		this.rootDirectoryTextDecorator.hide();
		Button browseButton = new Button(res, SWT.PUSH);
		browseButton.setText(Messages.EasymportWizardPage_browse);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText(Messages.selectFolderToImport);
				if (rootDirectoryText.getText() != null && new File(rootDirectoryText.getText()).isDirectory()) {
					dialog.setFilterPath(rootDirectoryText.getText());
				} else {
					dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toString());
				}
				String res = dialog.open();
				if (res != null) {
					rootDirectoryText.setText(res);
					SelectImportRootWizardPage.this.selection = new File(res);
					SelectImportRootWizardPage.this.validatePage();
				}
			}
		});

		final Button importRawProjectRadio = new Button(res, SWT.RADIO);
		importRawProjectRadio.setText(Messages.EasymportWizardPage_importRawProject);
		importRawProjectRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		importRawProjectRadio.setSelection(!this.detectNestedProjects);
		importRawProjectRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selection = importRawProjectRadio.getSelection();
				if (selection) {
					SelectImportRootWizardPage.this.detectNestedProjects = false;
					SelectImportRootWizardPage.this.configureProjects = false;
				}
				setPageComplete(isPageComplete());
			}
		});
		final Button importAndConfigureProjectRadio = new Button(res, SWT.RADIO);
		importAndConfigureProjectRadio.setText(Messages.EasymportWizardPage_importAndConfigureProject);
		importAndConfigureProjectRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		importAndConfigureProjectRadio.setSelection(!this.detectNestedProjects);
		importAndConfigureProjectRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selection = importAndConfigureProjectRadio.getSelection();
				if (selection) {
					SelectImportRootWizardPage.this.detectNestedProjects = false;
					SelectImportRootWizardPage.this.configureProjects = true;
				}
				setPageComplete(isPageComplete());
			}
		});
		final Button detectNestedProjectCheckbox = new Button(res, SWT.RADIO);
		detectNestedProjectCheckbox.setText(Messages.EasymportWizardPage_detectNestedProjects);
		detectNestedProjectCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		detectNestedProjectCheckbox.setSelection(this.detectNestedProjects);
		detectNestedProjectCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selection = detectNestedProjectCheckbox.getSelection();
				if (selection) {
					SelectImportRootWizardPage.this.detectNestedProjects = true;
					SelectImportRootWizardPage.this.configureProjects = true;
				}
				setPageComplete(isPageComplete());
			}
		});
		Link showDetectorsLink = new Link(res, SWT.NONE);
		showDetectorsLink.setText("<A>" + Messages.EasymportWizardPage_showAvailableDetectors + "</A>");
		showDetectorsLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		showDetectorsLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				StringBuilder message = new StringBuilder();
				message.append(Messages.EasymportWizardPage_availableDetectors_description);
				message.append('\n');
				message.append('\n');
				List<String> extensionsLabels = new ArrayList<String>(ProjectConfiguratorExtensionManager.getAllExtensionLabels());
				Collections.sort(extensionsLabels);
				for (String extensionLabel : extensionsLabels) {
					message.append("* ");
					message.append(extensionLabel);
					message.append('\n');
				}
				MessageDialog.openInformation(getShell(), Messages.EasymportWizardPage_availableDetectors_title, message.toString());
			}
		});

		Group workingSetsGroup = new Group(res, SWT.NONE);
		workingSetsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		workingSetsGroup.setLayout(new GridLayout(1, false));
		workingSetsGroup.setText(Messages.EasymportWizardPage_workingSets);
		workingSetsBlock = new WorkingSetConfigurationBlock(new String[] { "org.eclipse.ui.resourceWorkingSetPage" }, getDialogSettings()); //$NON-NLS-1$
		if (this.workingSets != null) {
			workingSetsBlock.setWorkingSets(this.workingSets.toArray(new IWorkingSet[this.workingSets.size()]));
		}
		workingSetsBlock.createContent(workingSetsGroup);

		if (this.selection == null) {
			String dialogSetting = getDialogSettings().get(ROOT_DIRECTORY);
			if (dialogSetting != null) {
				this.selection = new File(dialogSetting);
			}
		}
		if (this.selection != null) {
			rootDirectoryText.setText(this.selection.getAbsolutePath());
			validatePage();
		}

		setControl(res);
	}

	protected void validatePage() {
		if (this.selection == null || !this.selection.isDirectory()) {
			this.rootDirectoryTextDecorator.show();
			setErrorMessage(this.rootDirectoryTextDecorator.getDescriptionText());
		} else {
			this.rootDirectoryTextDecorator.hide();
			setErrorMessage(null);
		}
		setPageComplete(isPageComplete());
	}

	@Override
	public boolean isPageComplete() {
		return this.selection != null && this.selection.isDirectory();
	}


	public File getSelectedRootDirectory() {
		return this.selection;
	}

	public void setInitialSelectedDirectory(File directory) {
		this.selection = directory;
		this.rootDirectoryText.setText(directory.getAbsolutePath());
	}

	public Set<IWorkingSet> getSelectedWorkingSets() {
		this.workingSets.clear();
		// workingSetsBlock doesn't support listeners...
		Runnable workingSetsRetriever = new Runnable() {
			@Override
			public void run() {
				for (IWorkingSet workingSet : SelectImportRootWizardPage.this.workingSetsBlock.getSelectedWorkingSets()) {
					SelectImportRootWizardPage.this.workingSets.add(workingSet);
				}
			}
		};
		if (Display.getCurrent() == null) {
			getContainer().getShell().getDisplay().syncExec(workingSetsRetriever);
		} else {
			workingSetsRetriever.run();
		}
		return this.workingSets;
	}

	public boolean isDetectNestedProject() {
		return this.detectNestedProjects;
	}

	public boolean isConfigureProjects() {
		return this.configureProjects;
	}

	/**
	 *
	 * @return
	 * @deprecated Use {@link #isConfigureProjects()} and {@link #isDetectNestedProject()} instead
	 */
	@Deprecated
	public boolean isConfigureAndDetectNestedProject() {
		return isConfigureProjects();
	}

}

