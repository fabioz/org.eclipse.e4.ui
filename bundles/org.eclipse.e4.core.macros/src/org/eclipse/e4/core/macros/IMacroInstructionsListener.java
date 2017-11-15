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
 * An instance of this interface can be notified of changes during macro
 * recording.
 *
 * <ul>
 * <li>FIXME: this seems to be more of an adviser than listener?
 * <li>FIXME: rename preAddMacroInstruction -> verifyMacroInstruction()?
 * </ul>
 */
public interface IMacroInstructionsListener {

	/**
	 * Called before adding a macro instruction to the macro.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be added.
	 * @throws CancelMacroRecordingException
	 *             if the recording of the macro should stop before actually adding
	 *             the given macro instruction.
	 */
	void preAddMacroInstruction(IMacroInstruction macroInstruction) throws CancelMacroRecordingException;

	/**
	 * Called after a given macro instruction is added to the macro. Note that
	 * it is possible that {@link #preAddMacroInstruction(IMacroInstruction)} is
	 * called without a matching
	 * {@link #postAddMacroInstruction(IMacroInstruction)) should the macro
	 * instruction not be of high-enough priority.
	 *
	 * @param macroInstruction
	 *            the macro instruction added to the current macro.
	 */
	void postAddMacroInstruction(IMacroInstruction macroInstruction);
}
