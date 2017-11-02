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

import java.util.Map;

/**
 * The basic abstraction of a macro instruction (i.e.: a macro may be composed
 * of multiple macro instructions). The macro instruction also can be stored in
 * disk to be reconstructed later on.
 */
public interface IMacroInstruction {

	/**
	 * @return the id for the macro instruction.
	 * @note this id may be visible to the user, so, it should ideally be something
	 *       short and readable (such as KeyDown, or Command), not a dotted name as
	 *       is usual for Eclipse ids (note that it cannot be changed afterwards as
	 *       this id may be written to disk).
	 */
	String getId();

	/**
	 * Executes the macro instruction in the given context.
	 *
	 * @param macroPlaybackContext
	 *            the context used to playback the macro.
	 * @throws MacroPlaybackException
	 *             if something didn't work when executing the macro.
	 */
	void execute(IMacroPlaybackContext macroPlaybackContext) throws MacroPlaybackException;

	/**
	 * Convert the macro instruction into a map (which may be later dumped to the
	 * disk) and recreated with an
	 * {@link org.eclipse.e4.core.macros.IMacroInstructionFactory} registered
	 * through the org.eclipse.e4.core.macros.macroInstructionsFactory extension
	 * point.
	 *
	 * @return a map which may be dumped to the disk and can be used to recreate the
	 *         macro instruction later on.
	 */
	Map<String, String> toMap();

}
