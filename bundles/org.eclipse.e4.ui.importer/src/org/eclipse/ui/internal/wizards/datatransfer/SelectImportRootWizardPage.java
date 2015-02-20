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

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;

public class SelectImportRootWizardPage extends WizardPage {

	public static final String ROOT_DIRECTORY = "rootDirectory";
	
	private File selection;
	private Set<IWorkingSet> workingSets;
	private ControlDecoration rootDirectoryTextDecorator;
	private WorkingSetConfigurationBlock workingSetsBlock;

	private Text rootDirectoryText;

	public SelectImportRootWizardPage(IWizard wizard, File initialSelection, Set<IWorkingSet> initialWorkingSets) {
		super(EasymportWizard.class.getName());
		this.selection = initialSelection;
		this.workingSets = initialWorkingSets;
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
		this.rootDirectoryTextDecorator.setImage(getShell().getDisplay().getSystemImage(SWT.ERROR));
		this.rootDirectoryTextDecorator.setDescriptionText(Messages.EasymportWizardPage_incorrectRootDirectory);
		this.rootDirectoryTextDecorator.hide();
		Button browseButton = new Button(res, SWT.PUSH);
		browseButton.setText(Messages.EasymportWizardPage_browse);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText(rootDirectoryText.getText());
				String res = dialog.open();
				if (res != null) {
					rootDirectoryText.setText(res);
					SelectImportRootWizardPage.this.selection = new File(res);
					SelectImportRootWizardPage.this.validatePage();
				}
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
		} else {
			this.rootDirectoryTextDecorator.hide();
		}
		setPageComplete(isPageComplete());
	}
	
	@Override
	public boolean isPageComplete() {
		return this.selection != null && this.selection.isDirectory();
	}
	

	public File getSelectedRootDirectory() {
		return this.selection.getAbsoluteFile();
	}
	
	public void setInitialSelectedDirectory(File directory) {
		this.selection = directory;
		this.rootDirectoryText.setText(directory.getAbsolutePath());
	}

	public Set<IWorkingSet> getSelectedWorkingSets() {
		Set<IWorkingSet> res = new HashSet<IWorkingSet>(this.workingSetsBlock.getSelectedWorkingSets().length);
		for (IWorkingSet workingSet : this.workingSetsBlock.getSelectedWorkingSets()) {
			res.add(workingSet);
		}
		return res;
	}

	@Override
	public boolean canFlipToNextPage() {
		return this.selection != null && this.selection.isDirectory();
	}
	
}
