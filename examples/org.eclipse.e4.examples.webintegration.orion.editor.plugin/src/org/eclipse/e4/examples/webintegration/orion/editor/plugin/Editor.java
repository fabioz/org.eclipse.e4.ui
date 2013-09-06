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

package org.eclipse.e4.examples.webintegration.orion.editor.plugin;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;


/**
 * Example of hosting an editor in a BrowserWidget while
 * implementing interesting parts of the Eclipse Workbench 
 * editor life cycle and properties:
 *      1) Opening local content
 *      2) Dirty indicator
 *      3) Saving
 *      4) Eclipse Status Bar (cursor position)
 *      
 * Probably the most interesting part of this example is how
 * the web application is implemented to allow the application
 * to run in either a Browser or the Workbench with maximum integration
 * using minimal code changes.
 * 
 * The particular editor we embed is the Orion editor from http://eclipse.org/orion/
 * 
 * This example is discussed at http://deanoneclipse.wordpress.com
 */
public class Editor extends EditorPart {
	private Browser browser;
	private boolean isDirty = false;
	private EditorService editorService;
	private IStatusLineManager statusLineManager;
	private StatusLineContributionItem position;
	private StatusLineContributionItem keyMode;
	private StatusLineContributionItem writeMode;

	// Create the editor widgets
	public void createPartControl(Composite parent) {
		// Use the system's default browser
		browser = new Browser(parent, SWT.NONE);
		
		URL resource = Editor.class.getClassLoader().getResource("orion/examples/editor/embeddededitor.html");
		try {
			URL resolved = FileLocator.resolve(resource);
			String qualifiedPath = resolved.toExternalForm();
			browser.setUrl(qualifiedPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		// Add a listener to register and unregister browser functions when pages load
		browser.addLocationListener(getLocationListener());
		
		// Create Eclipse status line contributions
		createStatusLine();
	}
	
	/**
	 * Register our browser functions when the editor web page is loaded, unregister the browser
	 * functions when any other page is loaded.
	 */
	private LocationListener getLocationListener() {
		return new LocationListener() {
			public void changing(LocationEvent event) {
				// Do nothing before the page loads
			}
			
			public void changed(LocationEvent event) {
				if (!event.top) return;

				if (event.location.contains("embeddededitor.html")) {
					// Register browser functions that allow JavaScript to call into the Workbench
					registerBrowserFunctions();
				} else {
					unregisterBrowserFunctions();
				}
			}
		};
	}

	// Register browser functions that allow JavaScript to call into the Workbench
	private void registerBrowserFunctions() {
		editorService = new EditorService(browser, EditorService.EDITOR_SERVICE_HANDLER , this);
	}

	private void unregisterBrowserFunctions() {
		if (editorService != null && editorService instanceof BrowserFunction) {
			editorService.dispose();
		}
	}
	
	/**
	 * This method is part of the Eclipse editor save framework and will be called
	 * by Eclipse when a Save action is invoked (tool bar, menu, Eclipse key binding etc.)
	 * 
	 * This implementation will call into the web application requesting a save, since
	 * only the web application knows what it means to save itself
	 *  
	 * IMPORTANT:
	 * 
	 * This mechanism is synchronous, while saving in a web world is typically asynchronous.  
	 * Eclipse has an asynchronous saving mechanism through ISavelable.  This example will
	 * be updated shortly to use that mechanism.
	 */
	public void doSave(IProgressMonitor monitor) {
			try {
				Object resultObj = browser.evaluate(EditorService.JAVA_SCRIPT_SAVE_FUNCTION);

				// If the call to the web application returns false, indicating the save failed, cancel the operation
				if (!(resultObj instanceof Boolean && (Boolean) resultObj)) {
					monitor.setCanceled(true);
				}
			} catch (SWTException e) {
				// Either the script caused a javascript error or returned an unsupported type
				e.printStackTrace();
				monitor.setCanceled(true);
			}
	}
	
	
	/**
	 * This method is called by the web application's editor service to perform the local save.
	 * @param newContents The new contents to save as a string 
	 * @return true if the save was successful, false otherwise
	 */
	protected boolean performSave(String newContents) {
		if (getEditorInput() instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput) getEditorInput();
			ByteArrayInputStream inputStream = new ByteArrayInputStream(newContents.getBytes());
			try {
				input.getFile().setContents(inputStream, IFile.KEEP_HISTORY, null);
				return true;
			} catch (CoreException e) {
				// Save failed
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean isDirty() {
		return isDirty;
	}
	
	// Set by the web application's editor service when the dirty state changes
	protected void setDirty(boolean newValue) {
		if (isDirty != newValue) {
			isDirty = newValue;
			firePropertyChange(PROP_DIRTY);
		}
	}
	
	// Set by the web application's editor service when the cursor position changes
	protected void setPositionStatus(String text) {
		position.setText(text);
	}
	
	/**
	 * Initialize the editor.
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public void doSaveAs() {
	}
	
	private void createStatusLine() {
		statusLineManager = getEditorSite().getActionBars().getStatusLineManager();
		position = new StatusLineContributionItem("position", 15);
		keyMode = new StatusLineContributionItem("keyMode", 15);
		writeMode = new StatusLineContributionItem("writeMode", 15);
		statusLineManager.add(writeMode);
		statusLineManager.add(new Separator());
		statusLineManager.add(keyMode);
		statusLineManager.add(new Separator());
		statusLineManager.add(position);
		
		// Set the initial cursor position
		position.setText("0 : 0");

		// writeMode and keyMode values are faked at the moment since the Orion editor
		// does not return actual values for these properties.
		writeMode.setText("Writable");
		keyMode.setText("Smart Insert");
	}
	
	public void setFocus() {
		// Nothing to do but contractually obligated to override this abstract method.
	}
}
