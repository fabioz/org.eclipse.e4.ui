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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This Perspective is part of an example project that contains a set of simple examples
 * for accomplishing better integration between WebUIs and the Eclipse Workbench.
 * 
 * Each example topic will have its own instance of this Perspective.  The implementation will
 * be in its one example.* package.  Look there for the interesting implementation details.
 * 
 * The code contained in this class is generic and is not particularly illustrative of the
 * examples themselves.
 */
public class Perspective implements IPerspectiveFactory {

	// Some public URLs that may be interesting for the various examples
	public static final String gmailURL = "https://mail.google.com/mail/?hl=en&shva=1#inbox";
	public static final String pcFinancialURL = "https://www.txn.banking.pcfinancial.ca/a/banking/accounts/accountSummary.ams";
	public static final String pcFinancialURL2 = "https://www.pcfinancial.ca/";
	public static final String yahooURL = "https://mail.yahoo.com";
	private static final Map<String, String> instructionURLMap = new HashMap<String, String>();
	
	// Initialize the instructions for each example
	{
		instructionURLMap.put("default", "static/default.html");
		instructionURLMap.put("Link Intercept", "static/link.intercept.example.instructions.html");
	}
	
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
	}

	// Return the appropriate instructions for each example
	public static String getInstructionLocation(String label) {
		
		String result = instructionURLMap.get(label);
		if (result == null) {
			result = instructionURLMap.get("default");
		}
		return result;
	}
}
