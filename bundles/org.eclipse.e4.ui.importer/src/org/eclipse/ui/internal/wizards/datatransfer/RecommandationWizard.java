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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.internal.dialogs.ImportExportWizard;

public class RecommandationWizard extends ImportExportWizard {

	public RecommandationWizard(String pageId) {
		super(pageId);
	}

	@Override
	public void addPages() {
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
//		for (Map<Entry>)
		return null;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
