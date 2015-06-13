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

import org.eclipse.core.runtime.IStatus;

/**
 * A hook is notified before and after a macro is recorded or re-played. They
 * are typically used to disable non-deterministic behaviours in the UI (i.e.,
 * where the results or ordering of results may change on subsequent
 * re-invocations). These hooks are configured with the
 * <code>org.eclipse.e4.ui.macros.hooks</code> extension point.
 * 
 * <p>
 * The macro system guarantees:
 * </p>
 * <ol>
 * <li>An instance that receives {{@link #start(Mode)} will receive a
 * corresponding {@link #stop(Mode)}.</li>
 * <li>The same instance that receives {{@link #start(Mode)} will receive
 * {@link #stop(Mode)}.</li>
 * </ol>
 */
public interface IMacroHook {
	enum Mode {
		/** A macro is about to be or finished being recorded */
		RECORDING,

		/** A macro is about to be or finished being replayed */
		PLAYBACK
	};

	/**
	 * Prepare for a macro to be recorded or played-back. Returns a status
	 * object; if not OK then the macro will be aborted.
	 * 
	 * @param mode
	 *            the macro mode
	 * @return the status
	 */
	IStatus start(Mode mode);

	/**
	 * The macro recording or playback has been completed (perhaps successfully
	 * but also may have been aborted).
	 * 
	 * @param mode
	 *            the macro mode
	 */
	void stop(Mode mode);
}
