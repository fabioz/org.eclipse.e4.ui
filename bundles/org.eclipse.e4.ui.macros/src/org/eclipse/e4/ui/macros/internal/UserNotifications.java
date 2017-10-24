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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.macros.Activator;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Helper class to show notifications to the user.
 */
public class UserNotifications {

	private static final String FIND_REPLACE_USER_NOTIFICATION_MSG = "FIND_REPLACE_USER_NOTIFICATION_MSG"; //$NON-NLS-1$

	private static final String CURRENT_EDITOR_NOTIFICATION_MSG = "CURRENT_EDITOR_NOTIFICATION_MSG"; //$NON-NLS-1$

	private static final String NO_EDITOR_ON_MACRO_RECORD_STARTUP_NOTIFICATION_MSG = "NO_EDITOR_ON_MACRO_RECORD_STARTUP_NOTIFICATION_MSG"; //$NON-NLS-1$

	/**
	 * Sets a given message to be shown to the user.
	 *
	 * @param message
	 *            the message to be shown or null to clear it.
	 */
	public static void setMessage(String message) {
		IStatusLineManager statusLineManager = UserNotifications.getStatusLineManager();
		if (statusLineManager != null) {
			statusLineManager.setMessage(message);
			if (message == null) {
				// Also clear any previous error message we might have set.
				statusLineManager.setErrorMessage(null);
			}
		}
	}

	/**
	 * Shows some error message related to the macro to the user.
	 *
	 * @param message
	 *            the error message to be shown (cannot be null).
	 */
	public static void showErrorMessage(String message) {
		Activator plugin = Activator.getDefault();
		if (plugin != null) {
			// Log it
			plugin.getLog().log(new Status(IStatus.INFO, plugin.getBundle().getSymbolicName(), message));
		}

		// Make it visible to the user.
		IStatusLineManager statusLineManager = UserNotifications.getStatusLineManager();
		if (statusLineManager == null) {
			Shell parent = UserNotifications.getParent();
			if (parent == null) {
				System.err.println(Messages.Activator_ErrorMacroRecording + ": " + message); //$NON-NLS-1$
			} else {
				MessageDialog.openWarning(parent, Messages.Activator_ErrorMacroRecording, message);
			}
		} else {
			statusLineManager.setErrorMessage(message);
			Display current = Display.getCurrent();
			if (current != null) {
				// Also beep to say something strange happened.
				current.beep();
			}
		}
	}

	/**
	 * @return a shell to be used as a dialog's parent.
	 */
	private static Shell getParent() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		return activeWorkbenchWindow.getShell();
	}

	/**
	 * @return the available status line manager for the current editor.
	 */
	private static IStatusLineManager getStatusLineManager() {
		IWorkbenchWindow activeWorkbenchWindow = EditorUtils.getActivateWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
		if (activePage == null) {
			return null;
		}
		IEditorPart activeEditor = activePage.getActiveEditor();
		if (activeEditor == null) {
			return null;
		}
		IEditorSite editorSite = activeEditor.getEditorSite();
		if (editorSite == null) {
			return null;
		}
		return editorSite.getActionBars().getStatusLineManager();
	}

	/**
	 * Show a notification regarding limitations on find/replace.
	 */
	public static void notifyFindReplace() {
		openWarningWithIgnoreToggle(Messages.UserNotifications_FindReplaceDialogTitle,
				Messages.UserNotifications_FindReplaceDialogMessage, FIND_REPLACE_USER_NOTIFICATION_MSG);
	}

	/**
	 * Show a notification regarding limitations on the editor changing.
	 */
	public static void notifyCurrentEditor() {
		openWarningWithIgnoreToggle(Messages.UserNotifications_EditorChangedTitle,
				Messages.UserNotifications_EditorChangedMessage, CURRENT_EDITOR_NOTIFICATION_MSG);
	}

	/**
	 * Show a notification regarding not having an editor opened when record
	 * started.
	 */
	public static void notifyNoEditorOnMacroRecordStartup() {
		openWarningWithIgnoreToggle(Messages.UserNotifications_NoEditorForRecordTitle,
				Messages.UserNotifications_NoEditorForRecordMsg, NO_EDITOR_ON_MACRO_RECORD_STARTUP_NOTIFICATION_MSG);
	}

	/**
	 * Show a notification regarding not having an editor opened when playback
	 * started.
	 */
	public static void notifyNoEditorOnMacroPlaybackStartup() {
		openWarningWithIgnoreToggle(Messages.UserNotifications_NoEditorForPlaybackTitle,
				Messages.UserNotifications_NoEditorForPlaybackMsg, NO_EDITOR_ON_MACRO_RECORD_STARTUP_NOTIFICATION_MSG);
	}

	private static void openWarningWithIgnoreToggle(String title, String message, String key) {
		IWorkbenchWindow activateWorkbenchWindow = EditorUtils.getActivateWorkbenchWindow();
		if (activateWorkbenchWindow == null) {
			return;
		}
		Shell shell = activateWorkbenchWindow.getShell();

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String val = store.getString(key);
		if (val.trim().length() == 0) {
			val = MessageDialogWithToggle.PROMPT; // Initial value if not specified
		}

		if (!val.equals(MessageDialogWithToggle.ALWAYS)) {
			MessageDialogWithToggle.openWarning(shell, title, message, Messages.UserNotifications_DontShowAgain, false,
					store, key);
		}
		return;
	}

}
