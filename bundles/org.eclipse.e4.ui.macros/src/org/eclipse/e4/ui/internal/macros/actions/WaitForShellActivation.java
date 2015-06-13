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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;

public class WaitForShellActivation implements IMacroAction {
	public WaitForShellActivation(Widget widget) {
		// FIXME: store identifying information for the widget shell details?
	}

	@Override
	public ReplayState process(Event e) {
		// if (e.type == SWT.Activate && e.widget.getClass() == Shell.class) {
		// return ReplayState.NEXT;
		// }
		// return ReplayState.WAITING;

		return ReplayState.NEXT;
	}

	public String toString() {
		return "Wait for shell activation";
	}
}
