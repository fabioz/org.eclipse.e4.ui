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
package org.eclipse.e4.ui.macros;

import org.eclipse.swt.widgets.Event;

public interface IMacroAction {
	/** The state of this action after processing an event */
	public enum ReplayState {
		/** This event has not changed the state of this action */
		WAITING,

		/**
		 * This event is not compatible with this action; the macro should be
		 * aborted
		 */
		ABORT,

		/**
		 * This event has been processed and the macro should move to the next
		 * action
		 */
		NEXT
	};

	public ReplayState process(Event e);
}
