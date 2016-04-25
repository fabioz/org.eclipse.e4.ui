/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.naturist;

import java.util.Collections;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.resources.natures.Messages;
import org.eclipse.ui.resources.natures.ProjectNaturesPage;

public class EditProjectNaturesQuickfix implements IMarkerResolution2 {

	private IResource resource;

	public EditProjectNaturesQuickfix(IResource resource) {
		this.resource = resource;
	}

	@Override
	public String getLabel() {
		return Messages.editProjectNatures;
	}

	@Override
	public void run(IMarker marker) {
		PreferencesUtil.createPropertyDialogOn(Display.getDefault().getActiveShell(), resource.getProject(),
				ProjectNaturesPage.ID, new String[] { ProjectNaturesPage.ID }, Collections.EMPTY_MAP);
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Image getImage() {
		return null;
	}

}
