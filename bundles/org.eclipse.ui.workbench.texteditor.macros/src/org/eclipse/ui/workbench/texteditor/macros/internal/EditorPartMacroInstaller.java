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

import org.eclipse.e4.core.macros.CancelMacroException;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;

/**
 * A listener to the macro context which will enable notifications to the user
 * regarding limitations of recording only in the current editor.
 */
public class EditorPartMacroInstaller implements IMacroStateListener {

	private static final String NOTIFY_MACRO_ONLY_IN_CURRENT_EDITOR = "NOTIFY_MACRO_ONLY_IN_CURRENT_EDITOR"; //$NON-NLS-1$

	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange)
			throws CancelMacroException {
		if (stateChange == StateChange.RECORD_STARTED) {
			IMacroRecordContext context = macroService.getMacroRecordContext();
			NotifyMacroOnlyInCurrentEditor notifyMacroOnlyInCurrentEditor = new NotifyMacroOnlyInCurrentEditor(
					macroService);
			notifyMacroOnlyInCurrentEditor.checkEditorActiveForMacroRecording();

			notifyMacroOnlyInCurrentEditor.install();
			context.set(NOTIFY_MACRO_ONLY_IN_CURRENT_EDITOR, notifyMacroOnlyInCurrentEditor);

		} else if (stateChange == StateChange.RECORD_FINISHED) {
			IMacroRecordContext context = macroService.getMacroRecordContext();
			Object object = context.get(NOTIFY_MACRO_ONLY_IN_CURRENT_EDITOR);
			if (object instanceof NotifyMacroOnlyInCurrentEditor) {
				NotifyMacroOnlyInCurrentEditor notifyMacroOnlyInCurrentEditor = (NotifyMacroOnlyInCurrentEditor) object;
				notifyMacroOnlyInCurrentEditor.uninstall();
			}
		} else if (stateChange == StateChange.PLAYBACK_STARTED) {
			new NotifyMacroOnlyInCurrentEditor(macroService).checkEditorActiveForMacroPlayback();
		}
	}
}
