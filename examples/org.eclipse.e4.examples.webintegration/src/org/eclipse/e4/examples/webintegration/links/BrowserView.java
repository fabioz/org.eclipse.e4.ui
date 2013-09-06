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


import org.eclipse.e4.examples.webintegration.application.Perspective;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewWizardAction;
import org.eclipse.ui.part.ViewPart;


/**
 * Example of a BrowserWidget that can incercept links and perform Eclipse Workbench
 * actions as desired.
 * 
 * This example is discussed at http://deanoneclipse.wordpress.com
 */
public class BrowserView extends ViewPart {

	private Browser browser;

	public void createPartControl(Composite parent) {
		browser = new Browser(parent, SWT.NONE);
		browser.setUrl(Perspective.gmailURL);
		
		// Hooks the link intercept code
		browser.addLocationListener(new LinkInterceptListener());
	}

	/**
	 * Implement a LocationListener to intercept links and decide what to do.
	 */
	private class LinkInterceptListener implements LocationListener {
		// method called when the user clicks a link but before the link is opened.
		public void changing(LocationEvent event) {
			try {
				// Call user code to process link as desired and return
				// true if the link should be opened in place.
				boolean shouldOpenLinkInPlace = !openView(event.location);
				
				// Setting event.doit to false prevents the link from opening in place
				event.doit = shouldOpenLinkInPlace;
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
		
		// method called after the link has been opened in place.
		public void changed(LocationEvent event) {
			// Not used in this example
		}
	}

	/**
	 * User code:
	 * 
	 * Examine the link and determine if we wish to intercept it.  Perform appropriate actions for intercepted links, do
	 * nothing for links we want to be opened in place (default behaviour)
	 * 
	 * Return true if we intercepted the link.  Return false if we did not intercept the link and expect the browser to
	 * open the link in place.
	 */
	private boolean openView(String location) throws PartInitException {
		
		/**
		 * Certainly the if/else-if construct could be replaced with a more elegant lookup mechanism. 
		 */
		
		// Open a view
		if (location.equals("http://www.google.com/intl/en_CA/mobile/mail/#utm_source=en_CA-cpp-g4mc-gmhp&utm_medium=cpp&utm_campaign=en_CA")) {
			IViewPart newView = getViewSite().getPage().showView("url.link.1");
			((LinkView) newView).setURL(location);
			
			return true;
		// Open a wizard
		} else if (location.contains("/accounts/recovery")) {
			NewWizardAction action = new NewWizardAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			action.run();
			
			return true;
		}
		
		// Do not intercept link.  Allow browser widget to open link in place
		return false;
	}

	public void setFocus() {
		// Not important for our example.
	}
}