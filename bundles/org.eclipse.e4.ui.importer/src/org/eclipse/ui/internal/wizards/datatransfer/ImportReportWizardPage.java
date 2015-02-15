package org.eclipse.ui.internal.wizards.datatransfer;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkingSet;

public class ImportReportWizardPage extends WizardPage {
	
	private boolean nestedProjectsLoaded;
	
	protected ImportReportWizardPage(EasymportWizard wizard) {
		super(ImportReportWizardPage.class.getName());
		setWizard(wizard);
	}

	@Override
	public EasymportWizard getWizard() {
		return (EasymportWizard)super.getWizard();
	}
	
	@Override
	public void createControl(Composite parent) {
		setTitle("Nested Projects");
		setDescription("Look for nested projects");
		Composite res = new Composite(parent, SWT.NONE);
		res.setLayout(new GridLayout(1, false));
		Label introLabel = new Label(res, SWT.WRAP);
		introLabel.setText(NLS.bind(Messages.EasymportWizardPage_nestedProjects, this.getWizard().getProject().getLocation()));
		introLabel.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false));
		new Label(res, SWT.NONE).setText(Messages.EasymportWizardPage_availableDetectors);
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
		return !this.nestedProjectsLoaded;
	}
	
	@Override
	public ImportReportWizardPage getNextPage() {
		if (!this.nestedProjectsLoaded) {
			final Set<IWorkingSet> workingSets = getWizard().getSelectedWorkingSets();
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							new OpenFolderCommand().importProjectAndChildrenRecursively(getWizard().getProject(), true, workingSets, monitor);
						} catch (Exception ex) {
							throw new InvocationTargetException(ex);
						}
					}
				});
				this.nestedProjectsLoaded = true;
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
