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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;

public class WaitForWidgetFocus implements IMacroAction {
	private Class<? extends Widget> widgetClass;

	public WaitForWidgetFocus(Widget widget) {
		this.widgetClass = widget.getClass();
	}

	@Override
	public ReplayState process(Event e) {
		Control focus = e.display.getFocusControl();
		if (focus != null && focus.getClass() == widgetClass) {
			return ReplayState.NEXT;
		} else if (e.type == SWT.FocusIn && e.widget.getClass() == widgetClass) {
			return ReplayState.NEXT;
		}
		return ReplayState.WAITING;
	}

	public String toString() {
		return "Wait for focus onto " + widgetClass.getName();
	}
}
