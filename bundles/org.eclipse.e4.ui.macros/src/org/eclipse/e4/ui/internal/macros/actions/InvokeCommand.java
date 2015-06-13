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

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.ui.macros.IMacroAction;
import org.eclipse.swt.widgets.Event;

/**
 * Keyboard macro invoked an Eclipse-level command.
 */
public class InvokeCommand implements IMacroAction {
	private EHandlerService handlerService;
	private ParameterizedCommand command;

	public InvokeCommand(EHandlerService hs, ParameterizedCommand command) {
		this.handlerService = hs;
		this.command = command;
	}

	@Override
	public ReplayState process(Event e) {
		if (!handlerService.canExecute(command)) {
			return ReplayState.ABORT;
		}
		handlerService.executeHandler(command);
		return ReplayState.NEXT;
	}

	@Override
	public String toString() {
		return "Invoke " + command.getId();
	}
}
