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
package org.eclipse.e4.core.macros.internal;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;

/**
 * Provides a way to recreate commands when playing back a macro.
 */
public class MacroPlaybackContextImpl implements IMacroPlaybackContext {

	private final Map<Object, Object> ctx = new HashMap<>();

	@Override
	public Object get(String key) {
		return ctx.get(key);
	}

	@Override
	public void set(String key, Object value) {
		ctx.put(key, value);
	}
}
