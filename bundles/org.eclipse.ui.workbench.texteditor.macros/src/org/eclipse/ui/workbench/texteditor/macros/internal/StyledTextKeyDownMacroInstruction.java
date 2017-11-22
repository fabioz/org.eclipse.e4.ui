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

import java.util.Map;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.internal.JSONHelper;
import org.eclipse.e4.ui.macros.internal.EditorUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;

/**
 * A macro instruction to replay a key down (always followed by a key up).
 */
/* default */ class StyledTextKeyDownMacroInstruction extends AbstractSWTEventMacroInstruction {

	private static final String ID = "KeyEvent"; //$NON-NLS-1$

	public StyledTextKeyDownMacroInstruction(Event event) {
		super(event);
	}

	@Override
	public void execute(IMacroPlaybackContext macroPlaybackContext) {
		StyledText styledText = EditorUtils.getTargetStyledText(macroPlaybackContext);
		if (styledText != null) {
			if (styledText.isDisposed()) {
				return;
			}
			Event keyDownEvent = copyEvent(fEvent);
			styledText.notifyListeners(SWT.KeyDown, keyDownEvent);

			if (styledText.isDisposed()) {
				return;
			}

			// Key up is also needed to update the clipboard.
			Event keyUpEvent = copyEvent(fEvent);
			keyUpEvent.type = SWT.KeyUp;
			styledText.notifyListeners(SWT.KeyUp, keyUpEvent);
		}
	}

	@Override
	protected int getEventType() {
		return SWT.KeyDown;
	}

	@Override
	public String getId() {
		return ID;
	}

	/* default */ static StyledTextKeyDownMacroInstruction fromMap(Map<String, String> map) {
		Event event = createEventFromMap(map, SWT.KeyDown);
		return new StyledTextKeyDownMacroInstruction(event);
	}

	@Override
	public String toString() {
		return Messages.StyledTextKeyDownMacroInstruction_KeyDown
				+ JSONHelper.quote(Character.toString(this.fEvent.character));
	}
}