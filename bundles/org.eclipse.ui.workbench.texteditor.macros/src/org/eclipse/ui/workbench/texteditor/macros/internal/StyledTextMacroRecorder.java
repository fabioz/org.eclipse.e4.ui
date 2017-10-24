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

import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * A listener that will record actions done in a StyledText and add them to the
 * macro.
 */
public class StyledTextMacroRecorder implements Listener {

	private final EMacroService fMacroService;

	public StyledTextMacroRecorder(EMacroService macroService) {
		this.fMacroService = macroService;
	}

	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.KeyDown && fMacroService.isRecording()) {
			// Note: we only currently record key down actions and replay each
			// one with KeyDown and KeyUp. In practice, having a key pressed
			// down multiple times down and only once up gives the same result
			// as doing a down/up at each step.
			fMacroService.addMacroInstruction(new StyledTextKeyDownMacroInstruction(event), event,
					EMacroService.PRIORITY_LOW);
		}
	}

	public void uninstall(StyledText textWidget) {
		textWidget.removeListener(SWT.KeyDown, this);
	}

	public void install(StyledText textWidget) {
		textWidget.addListener(SWT.KeyDown, this);
	}
}
