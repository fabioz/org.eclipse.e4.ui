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
package org.eclipse.ui.workbench.texteditor.macros.internal;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.CancelMacroPlaybackException;
import org.eclipse.e4.core.macros.CancelMacroRecordingException;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.ui.macros.internal.EditorUtils;
import org.eclipse.e4.ui.macros.internal.UserNotifications;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Used to notify that macro is only available in the initial editor.
 */
public class NotifyMacroOnlyInCurrentEditor {

	/**
	 * When a new window is opened/activated, add the needed listeners.
	 */
	private class WindowsListener implements IWindowListener {
		@Override
		public void windowOpened(IWorkbenchWindow window) {
			addListeners(window);
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
		}

		@Override
		public void windowActivated(IWorkbenchWindow window) {
			addListeners(window);
		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
		}
	}

	/**
	 * When a new part is made visible or is opened, check if it's the one active
	 * when macro was activated.
	 */
	private class MacroPartListener implements IPartListener2 {

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			checkCurrentEditor();
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			checkCurrentEditor();
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			checkCurrentEditor();
		}
	}

	private MacroPartListener fPartListener = new MacroPartListener();

	private WindowsListener fWindowsListener = new WindowsListener();

	/**
	 * The macro service to be listened to.
	 */
	private EMacroService fMacroService;

	/**
	 * The last editor checked.
	 */
	private StyledText fLastEditor;

	private UserNotifications fUserNotifications;

	private IEclipseContext fEclipseContext;

	/**
	 * @param macroService
	 *            the macro service.
	 * @param eclipseContext
	 *            eclipse context for dependency injection.
	 */
	public NotifyMacroOnlyInCurrentEditor(EMacroService macroService, IEclipseContext eclipseContext) {
		fMacroService = macroService;
		fEclipseContext = eclipseContext;
	}

	/**
	 * Provides the class which should be used to give user notifications.
	 *
	 * @return the helper class for giving user notifications.
	 */
	public UserNotifications getUserNotifications() {
		if (fUserNotifications == null) {
			fUserNotifications = new UserNotifications();
			ContextInjectionFactory.inject(fUserNotifications, fEclipseContext);
		}
		return fUserNotifications;
	}

	/**
	 * Checks that the current editor didn't change (if it did, notify the user that
	 * recording doesn't work in other editors).
	 */
	private void checkCurrentEditor() {
		IMacroRecordContext macroRecordContext = this.fMacroService.getMacroRecordContext();
		if (macroRecordContext != null) {
			StyledText currentStyledText = EditorUtils.getActiveEditorStyledText();
			StyledText targetStyledText = EditorUtils.getTargetStyledText(macroRecordContext);
			if (targetStyledText != currentStyledText && currentStyledText != fLastEditor) {
				getUserNotifications().setMessage(Messages.NotifyMacroOnlyInCurrentEditor_NotRecording);
				getUserNotifications().notifyCurrentEditor();
			} else if (targetStyledText == currentStyledText && fLastEditor != null
					&& fLastEditor != currentStyledText) {
				getUserNotifications().setMessage(Messages.NotifyMacroOnlyInCurrentEditor_Recording);
			}
			fLastEditor = currentStyledText;
		}
	}

	/**
	 * Check if there's some active editor when the macro recording starts.
	 *
	 * @throws CancelMacroRecordingException
	 *             if there's no active editor available for the macro recording.
	 */
	public void checkEditorActiveForMacroRecording() throws CancelMacroRecordingException {
		StyledText currentStyledText = EditorUtils.getActiveEditorStyledText();
		if (currentStyledText == null) {
			getUserNotifications().setMessage(Messages.NotifyMacroOnlyInCurrentEditor_NotRecording);
			getUserNotifications().notifyNoEditorOnMacroRecordStartup();
			throw new CancelMacroRecordingException();
		}
	}

	/**
	 * Check if there's some active editor when the macro playback starts.
	 *
	 * @throws CancelMacroPlaybackException
	 *             if there's no active editor available for the macro playback.
	 */
	public void checkEditorActiveForMacroPlayback() throws CancelMacroPlaybackException {
		StyledText currentStyledText = EditorUtils.getActiveEditorStyledText();
		if (currentStyledText == null) {
			getUserNotifications().notifyNoEditorOnMacroPlaybackStartup();
			throw new CancelMacroPlaybackException();
		}
	}


	private void addListeners(IWorkbenchWindow window) {
		window.getPartService().addPartListener(fPartListener);
	}

	private void removeListeners(IWorkbenchWindow window) {
		window.getPartService().removePartListener(fPartListener);
	}

	/**
	 * Install listeners regarding macro only for current editor.
	 */
	public void install() {
		PlatformUI.getWorkbench().addWindowListener(fWindowsListener);
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			addListeners(window);
		}
	}

	/**
	 * Uninstall listeners regarding macro only for current editor.
	 */
	public void uninstall() {
		fLastEditor = null;
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			removeListeners(window);
		}
		PlatformUI.getWorkbench().removeWindowListener(fWindowsListener);
	}



}
