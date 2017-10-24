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
package org.eclipse.e4.core.macros;

/**
 * Provides a way for clients to be notified of the creation of macro contexts
 * to fill it as needed.
 */
public interface IMacroStateListener1 extends IMacroStateListener {

	/**
	 * Called after the creation of the macro playback context (before notifying
	 * about changes to the macro state or actual playback).
	 *
	 * @param context
	 *            the context for the macro playback.
	 */
	public void onMacroPlaybackContextCreated(IMacroPlaybackContext context);

	/**
	 * Called after the creation of the macro record context (before notifying about
	 * changes to the macro state or actual record).
	 *
	 * @param context
	 *            the context for the macro record.
	 */
	public void onMacroRecordContextCreated(IMacroRecordContext context);
}
