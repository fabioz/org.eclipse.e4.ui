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
package org.eclipse.e4.ui.macros.internal.actions;

import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionsListener;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.macros.internal.UserNotifications;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Make sure that the toolbar elements are kept properly updated even if the
 * macro is programmatically stopped.
 */
public class KeepMacroUIUpdated implements IMacroStateListener {

	/**
	 * A listener which will show messages to the user while he types macro
	 * instructions.
	 */
	private static final class MacroInstructionsListener implements IMacroInstructionsListener {
		@Override
		public void beforeMacroInstructionAdded(IMacroInstruction macroInstruction) {

		}

		@Override
		public void afterMacroInstructionAdded(IMacroInstruction macroInstruction) {
			UserNotifications.setMessage(Messages.KeepMacroUIUpdated_RecordedInMacro + macroInstruction);
		}
	}

	boolean wasRecording = false;

	boolean wasPlayingBack = false;

	IMacroInstructionsListener fMacroInstructionsListener;

	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange) {
		// Update the toggle action state.
		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		commandService.refreshElements(ToggleMacroRecordAction.COMMAND_ID, null);

		// Show a message to the user saying about the macro state.
		if (macroService.isRecording() != wasRecording) {
			if (!wasRecording) {
				UserNotifications.setMessage(Messages.KeepMacroUIUpdated_StartMacroRecord);
			} else {
				// When we stop the record, clear the message.
				UserNotifications.setMessage(null);
			}
			wasRecording = macroService.isRecording();
		}
		if (macroService.isPlayingBack() != wasPlayingBack) {
			if (!wasPlayingBack) {
				UserNotifications.setMessage(Messages.KeepMacroUIUpdated_StartMacroPlayback);
			} else {
				// When we stop the playback, clear the message.
				UserNotifications.setMessage(null);
			}
			wasPlayingBack = macroService.isPlayingBack();
		}

		if (macroService.isRecording()) {
			if (fMacroInstructionsListener == null) {
				fMacroInstructionsListener = new MacroInstructionsListener();
				macroService.addMacroInstructionsListener(fMacroInstructionsListener);
			}
		} else {
			if (fMacroInstructionsListener != null) {
				macroService.removeMacroInstructionsListener(fMacroInstructionsListener);
				fMacroInstructionsListener = null;
			}
		}
	}
}
