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
	 * @return the StyledText related to the current editor or null if there's no
	 *         such widget available (i.e.: if the current editor is not a text
	 *         editor or if there's no open editor).
	 */
	public static StyledText getActiveStyledText() {
		IEditorPart activeEditor = getActiveEditorPart();
		if (activeEditor == null) {
			return null;
		}
		return getEditorPartStyledText(activeEditor);
	}

	public static IEditorPart getActiveEditorPart() {
		IWorkbenchWindow activeWorkbenchWindow = getActivateWorkbenchWindow();
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
	 * @return the currently active workbench window or null if not available.
	 */
	public static IWorkbenchWindow getActivateWorkbenchWindow() {
		IWorkbench workbench;
		try {
			workbench = PlatformUI.getWorkbench();
		} catch (IllegalStateException e) { // java.lang.IllegalStateException: Workbench has not been created yet.
			return null;
		}
		return workbench.getActiveWorkbenchWindow();
	}

	/**
	 * @param editor
	 *            the editor for which we want the StyledText.
	 * @return the StyledText related to the current editor or null if it doesn't
	 *         have a StyledText.
	 */
	public static StyledText getEditorPartStyledText(IEditorPart editor) {
		Control control = editor.getAdapter(Control.class);
		StyledText styledText = null;
		if (control instanceof StyledText) {
			styledText = (StyledText) control;
		}
		return styledText;
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
				macroContext.set(TARGET_STYLED_TEXT, getActiveStyledText());
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
				macroContext.set(TARGET_EDITOR_PART, getActiveEditorPart());
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
