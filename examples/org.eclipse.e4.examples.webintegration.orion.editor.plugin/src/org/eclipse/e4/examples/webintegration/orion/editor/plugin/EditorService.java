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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

/**
 * This class implements the call in point for a web application.
 * Through this single BrowserFunction a web application can request
 * various actions.
 * 
 * An alternate implementation could have a a single BrowserFunction subclass
 * for each action a web application could call.  But it was unclear that
 * such an implementation adds any value. 
 */
public class EditorService extends BrowserFunction {
	// Action constants.  Same values used by JavaScript
	private static final int DIRTY_CHANGED = 1;
	private static final int GET_CONTENT_NAME = 2;
	private static final int GET_INITIAL_CONTENT = 3;
	private static final int SAVE = 4;
	private static final int STATUS_CHANGED = 5;

	// Name of the JavaScript variable containing the editor service
	public static final String EDITOR_SERVICE_MAP = "editorService";
	
	// Name of the JavaScript function for the editor service handler
	public static final String EDITOR_SERVICE_HANDLER = "editorServiceHandler";
	
	// Name of JavaScript save function
	public static final String JAVA_SCRIPT_SAVE_FUNCTION = "return " + EDITOR_SERVICE_MAP + ".save()";
	
	Editor editor;
	
	public EditorService(Browser browser, String name, Editor editor) {
		super(browser, name);
		this.editor = editor;
	}
	
	/**
	 * This is the single function that is invoked by the JavaScript program.
	 * By specification of our implementation there is always one or more arguments
	 * and the first argument is the action id.
	 * Subsequent arguments, if present, are arguments for the given action.
	 */
	public Object function(Object[] arguments) {
		super.function(arguments);
		
		if (arguments.length == 0 || !(arguments[0] instanceof Double)) {
			return null;
		}

		int action = ((Double) arguments[0]).intValue();
		switch (action) {
			case DIRTY_CHANGED:
				return doDirtyChanged(arguments);
				
			case GET_CONTENT_NAME:
				return doGetContentName(arguments);

			case GET_INITIAL_CONTENT:
				return doGetInitialContent(arguments);

			case SAVE:
				return doSave(arguments);
				
			case STATUS_CHANGED:
				return doStatusChanged(arguments);
				
			default:
				return null;
		}
	}

	// Actions

	/**
	 * Return the initial content for the editor.  Return an empty string on any error
	 */
	private Object doGetInitialContent(Object[] arguments) {
		if (editor.getEditorInput() instanceof IStorageEditorInput) {
			IStorageEditorInput input = (IStorageEditorInput) editor.getEditorInput();

			BufferedInputStream inputStream = null;
			
			
			try {
				IStorage storage = input.getStorage();
				inputStream = new BufferedInputStream(storage.getContents());
				String contents = readInputStream(inputStream);
				return contents;
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return "";
	}
	
	/**
	 * Return the name of the file being edited.  Return an empty string on any error
	 */
	private Object doGetContentName(Object[] arguments) {
		if (editor.getEditorInput() instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
			return input.getName();
		}
		return "";
	}

	/**
	 * This method is called by the JavaScript editor whenever its dirty state changes.
	 * Set the dirty status of the Eclipse editor so the framework displays the
	 * appropriate dirty marker and save actions are enabled appropriately.
	 * 
	 * Return the dirtyState after the event is processed
	 */

	private Object doDirtyChanged(Object[] arguments) {
		if (arguments.length == 2 && (arguments[1] instanceof Boolean)) {
			editor.setDirty((Boolean) arguments[1]);
		}
		return editor.isDirty();
	}
	
	/**
	 * JavaScript has requested a save.  Call the Eclipse editors save method.
	 * By contract, the JavaScript passes the new contents as an argument. 
	 * @param arguments
	 * @return
	 */
	private Object doSave(Object[] arguments) {
		boolean result = false;
		if (arguments.length == 2 && (arguments[1] instanceof String)) {
			String newContents = (String) arguments[1];
			result = editor.performSave(newContents);
		}
		return result;
	}
	
	/**
	 * Update Eclipse status line.
	 * Currently we dig the cursor position out of the string that the Orion editor sends us.
	 * Clearly we would prefer Orion API that could just send us the position info ... but this is, after all,
	 * a web integration example and not an Orion example :-)
	 */
	private boolean doStatusChanged(Object[] arguments) {
		if (arguments.length != 2 || !(arguments[1] instanceof String)) {
			return false;
		}
		
		String[] position = parsePosition((String) arguments[1]);
		editor.setPositionStatus(position[0] + " : " + position[1]);
		return true;
	}
	
	// Boring utility methods

	// Read all the bytes from an InputStream and return them as a String
	private String readInputStream(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = null;
		buffer = new ByteArrayOutputStream();
		byte[] bytes = new byte[1024];
		int bytesRead = 0;
		while ((bytesRead = inputStream.read(bytes)) != -1) {
			buffer.write(bytes, 0, bytesRead);
		}
		return buffer.toString();
	}
	
	// Really sleazy "parsing" code for "Line x : Col y".  Lots of error cases ignored
	private String[] parsePosition(String message) {
		int start = message.indexOf("Line ") + "Line ".length();
		int end = message.indexOf(' ', start);
		String line = message.substring(start, end);
		
		start = message.indexOf("Col ") + "Col ".length();
		end = message.indexOf(' ', start);
		if (end == -1) {
			end = message.length();
		}
		
		String col = message.substring(start, end);
		return new String[] {line, col};
	}
}
