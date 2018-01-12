/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - http://eclip.se/8519
 *******************************************************************************/
package org.eclipse.e4.ui.macros.internal;

import org.eclipse.e4.core.macros.IMacroContext;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Utilities related to getting/storing the current editor from/to the macro
 * context.
 */
public class EditorUtils {

	/**
	 * A variable which holds the current editor when macro record or playback
	 * started.
	 */
	private final static String TARGET_STYLED_TEXT = "TARGET_STYLED_TEXT"; //$NON-NLS-1$

	/**
	 * A variable which holds the current editor part when macro record or playback
	 * started.
	 */
	private final static String TARGET_EDITOR_PART = "TARGET_EDITOR_PART"; //$NON-NLS-1$

	/**
	 * Provides the styled text which is active from the current editor or null if
	 * it is not available.
	 *
	 * @return the StyledText related to the current editor or null if there is no
	 *         such widget available (i.e.: if the current editor is not a text
	 *         editor or if there is no open editor).
	 */
	public static StyledText getActiveEditorStyledText() {
		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor == null) {
			return null;
		}
		Control control = activeEditor.getAdapter(Control.class);
		StyledText styledText = null;
		if (control instanceof StyledText) {
			styledText = (StyledText) control;
		}
		return styledText;
	}

	/**
	 * Provides a way to get the editor part which is currently active or null if
	 * there's no current editor part.
	 *
	 * @return the active editor part.
	 */
	public static IEditorPart getActiveEditor() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
		if (activePage == null) {
			return null;
		}
		return activePage.getActiveEditor();
	}

	/**
	 * Provides the current active workbench window or null if it is not available.
	 *
	 * @return the current active workbench window or null if it is not available.
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench workbench;
		try {
			workbench = PlatformUI.getWorkbench();
		} catch (IllegalStateException e) { // java.lang.IllegalStateException: Workbench has not been created yet.
			return null;
		}
		return workbench.getActiveWorkbenchWindow();
	}

	/**
	 * Caches the current styled text as being the one active in the passed macro
	 * context.
	 *
	 * @param macroContext
	 *            the macro context where it should be set.
	 */
	public static void cacheTargetStyledText(IMacroContext macroContext) {
		if (macroContext != null) {
			Object object = macroContext.get(TARGET_STYLED_TEXT);
			if (object == null) {
				macroContext.set(TARGET_STYLED_TEXT, getActiveEditorStyledText());
			}
		}
	}

	/**
	 * Caches the current editor part as being the one active in the passed macro
	 * context.
	 *
	 * @param macroContext
	 *            the macro context where it should be set.
	 */
	public static void cacheTargetEditorPart(IMacroContext macroContext) {
		if (macroContext != null) {
			Object object = macroContext.get(TARGET_EDITOR_PART);
			if (object == null) {
				macroContext.set(TARGET_EDITOR_PART, getActiveEditor());
			}
		}
	}


	/**
	 * Gets the styled text which was set as the current when the macro context was
	 * created.
	 *
	 * @param macroContext
	 *            the macro context.
	 * @return the StyledText which was current when the recording started or null
	 *         if there was no StyledText active when recording started.
	 */
	public static StyledText getTargetStyledText(IMacroContext macroContext) {
		if (macroContext != null) {
			Object object = macroContext.get(TARGET_STYLED_TEXT);
			if (object instanceof StyledText) {
				return (StyledText) object;
			}
		}
		return null;
	}

	/**
	 * Gets the editor part which was set as the current when the macro context was
	 * created.
	 *
	 * @param macroContext
	 *            the macro context.
	 * @return the editor part which was current when the recording started or null
	 *         if there was no editor part active when the context was created.
	 */
	public static IEditorPart getTargetEditorPart(IMacroContext macroContext) {
		if (macroContext != null) {
			Object object = macroContext.get(TARGET_EDITOR_PART);
			if (object instanceof IEditorPart) {
				return (IEditorPart) object;
			}
		}
		return null;
	}

}
