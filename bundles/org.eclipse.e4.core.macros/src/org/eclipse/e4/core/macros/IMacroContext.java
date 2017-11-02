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
 * The context bound to the macro record or playback. Macro state listeners
 * (org.eclipse.e4.core.macros.IMacroStateListener) registered in
 * {@link EMacroService} may use it as a simple key-value store to keep
 * macro-specific state during a record or playback.
 */
public interface IMacroContext {

	/**
	 * @param key
	 *            the key of the variable to be retrieved.
	 * @return the object related to that variable.
	 */
	public Object get(String key);

	/**
	 * @param key
	 *            the key of the variable to store.
	 * @param value
	 *            the value to be stored for that key.
	 */
	public void set(String key, Object value);

}
