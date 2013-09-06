/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dean Roberts, IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.e4.examples.webintegration.application;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.part.ViewPart;

/**
 * This view provides an area for displaying instructions germane 
 * to each example.  The view itself is not interesting in regards
 * to integrating WebUIs with an Eclipse Workbench 
 */
public class InstructionsView extends ViewPart {

	private Browser browser;
	
	public void createPartControl(Composite parent) {
		IPerspectiveDescriptor descriptor = getSite().getPage().getPerspective();
		String instructionLocation = Perspective.getInstructionLocation(descriptor.getLabel());
		browser = new Browser(parent, SWT.NONE);

		String qualifiedPath = "";
		try {
			qualifiedPath = new File(instructionLocation).toURL().toExternalForm();
			browser.setUrl(qualifiedPath);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void setFocus() {
		// Not used in this example
	}
}
