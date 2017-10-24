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
 * Context passed when playing back a macro.
 */
public interface IMacroPlaybackContext extends IMacroContext {

	/**
	 * Creates a macro instruction (for execution) given its id and the
	 * {@link IMacroInstruction#toMap()} contents gotten from a previously recorded
	 * instruction.
	 *
	 * @param macroInstructionId
	 *            the id of the macro instruction to be created.
	 * @param stringMap
	 *            the contents previously gotten from
	 *            {@link IMacroInstruction#toMap()}.
	 * @return a macro instruction created from the given id and parameters.
	 * @throws Exception
	 *             if some error was thrown when creating the macro instruction.
	 */
	IMacroInstruction createMacroInstruction(String macroInstructionId, Map<String, String> stringMap) throws Exception;

}
