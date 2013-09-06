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

package org.eclipse.e4.examples.webintegration.links;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * A simple view to be opened by BrowserView when certain links are intercepted
 */
public class LinkView extends ViewPart {

	private Browser browser;

	public void createPartControl(Composite parent) {
		browser = new Browser(parent, SWT.NONE);
	}

	public void setFocus() {
	}

	public void setURL(String location) {
		browser.setUrl(location);
	}

}
