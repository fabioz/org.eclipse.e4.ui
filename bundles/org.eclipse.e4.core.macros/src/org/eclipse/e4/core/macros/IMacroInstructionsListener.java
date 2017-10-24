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
 * A listener for the macro instructions being added.
 */
public interface IMacroInstructionsListener {

	/**
	 * The macro instruction to be added to the macro.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be added.
	 * @throws CancelMacroRecordingException
	 *             if the recording of the macro should stop before actually adding
	 *             the given macro instruction.
	 */
	void beforeMacroInstructionAdded(IMacroInstruction macroInstruction) throws CancelMacroRecordingException;

	/**
	 * Called after a given macro instruction is added to the macro. Note that it's
	 * possible that beforeMacroInstructionAdded is called and
	 * afterMacroInstructionAdded isn't if the macro instruction doesn't have enough
	 * priority.
	 *
	 * @param macroInstruction
	 *            the macro instruction just added to the current macro.
	 */
	void afterMacroInstructionAdded(IMacroInstruction macroInstruction);
}
