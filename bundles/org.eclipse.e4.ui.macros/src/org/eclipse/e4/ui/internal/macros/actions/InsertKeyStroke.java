/*******************************************************************************
 * Copyright (c) 2015 Manumitting Technologies Inc and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manumitting Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.internal.macros.actions;

import org.eclipse.e4.ui.macros.IMacroAction;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;

/**
 * Keyboard macro performed some key stroke
 */
public class InsertKeyStroke implements IMacroAction {
	private static int[] stateKeyCodes = { SWT.MOD1, SWT.MOD2, SWT.MOD3, SWT.MOD4 };
	private char character;
	private int stateMask;
	private int keyCode;

	public InsertKeyStroke(Event event, Widget widget) {
		this.character = event.character;
		this.stateMask = event.stateMask;
		this.keyCode = event.keyCode;
	}

	@Override
	public ReplayState process(Event e) {
		postKeyStroke(e.display);
		return ReplayState.NEXT;
	}

	private void postKeyStroke(final Display display) {
		Event event;
		for (int i = 0; i < stateKeyCodes.length; i++) {
			if (stateKeyCodes[i] > 0 && (stateMask & stateKeyCodes[i]) != 0) {
				event = new Event();
				event.type = SWT.KeyDown;
				event.keyCode = stateKeyCodes[i];
				display.post(event);
			}
		}
		event = new Event();
		event.type = SWT.KeyDown;
		event.character = character;
		event.keyCode = keyCode;
		display.post(event);

		event = new Event();
		event.type = SWT.KeyUp;
		event.character = character;
		event.keyCode = keyCode;
		display.post(event);

		for (int i = 0; i < stateKeyCodes.length; i++) {
			if (stateKeyCodes[i] > 0 && (stateMask & stateKeyCodes[i]) != 0) {
				event = new Event();
				event.type = SWT.KeyUp;
				event.keyCode = stateKeyCodes[i];
				display.post(event);
			}
		}
	}

	@Override
	public String toString() {
		return "Key " + SWTKeySupport.getKeyFormatterForPlatform().format(keyCode);
	}

}
