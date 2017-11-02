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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.MacroPlaybackException;

/**
 * Actually loads a macro from a JS file to be played back. Works with the
 * contents saved from {@link ComposableMacro#toJSBytes()}.
 * <p>
 * Currently the saved macro is a JavaScript file to be played back again with a
 * "runMacro" function which may have multiple "runMacroInstruction" calls to
 * run a macro instruction which was previously persisted with
 * {@link IMacroInstruction#toMap()}.
 * </p>
 */
public class SavedJSMacro implements IMacro {

	/**
	 * The file which contains the contents of the macro.
	 */
	private final File fFile;

	/**
	 * Creates a macro which is backed up by the contents of a javascript file.
	 *
	 * @param file
	 *            the file with the contents of the macro.
	 */
	public SavedJSMacro(File file) {
		this.fFile = file;
	}

	/**
	 * Static method to be called when playing back a macro to run a macro
	 * instruction..
	 *
	 * @param macroPlaybackContext
	 *            the context for the macro playback.
	 * @param macroInstructionId
	 *            the id of the macro instruction to be executed.
	 * @param macroInstructionParameters
	 *            the parameters to create the macro instruction.
	 * @param macroInstructionIdToFactory
	 *            a map pointing from the macro instruction id to the factory used
	 *            to create the related macro instruction.
	 * @throws Exception
	 *             if something happened when creating the macro instruction or
	 *             actually executing it.
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void runMacroInstruction(IMacroPlaybackContext macroPlaybackContext, String macroInstructionId,
			Object macroInstructionParameters, Map<String, IMacroInstructionFactory> macroInstructionIdToFactory)
			throws Exception {
		Map<String, String> stringMap = new HashMap<>();
		Map m = (Map) macroInstructionParameters;
		Set<Map.Entry> entrySet = m.entrySet();
		for (Map.Entry entry : entrySet) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			stringMap.put(key.toString(), value.toString());
		}

		IMacroInstructionFactory macroFactory = macroInstructionIdToFactory.get(macroInstructionId);
		if (macroFactory == null) {
			throw new RuntimeException(
					"Unable to find IMacroInstructionFactory for macro instruction: " + macroInstructionId); //$NON-NLS-1$
		}

		IMacroInstruction macroInstruction = macroFactory.create(stringMap);
		macroInstruction.execute(macroPlaybackContext);
	}

	@Override
	public void playback(IMacroPlaybackContext macroPlaybackContext,
			Map<String, IMacroInstructionFactory> macroInstructionIdToFactory) throws MacroPlaybackException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("nashorn"); //$NON-NLS-1$
		SimpleScriptContext context = new SimpleScriptContext();
		context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
		Bindings engineScope = context.getBindings(ScriptContext.ENGINE_SCOPE);

		// Setup the default context.
		engineScope.put("__macroPlaybackContext", macroPlaybackContext); //$NON-NLS-1$
		engineScope.put("__macroInstructionIdToFactory", macroInstructionIdToFactory); //$NON-NLS-1$

		try {
			engine.eval("" + //$NON-NLS-1$
					"__macro = Java.type('org.eclipse.e4.core.macros.internal.SavedJSMacro');\n" //$NON-NLS-1$
					+ "function runMacroInstruction(macroMacroInstructionId, macroInstructionParameters){" //$NON-NLS-1$
					+ "    __macro.runMacroInstruction(__macroPlaybackContext, macroMacroInstructionId, macroInstructionParameters, __macroInstructionIdToFactory);" //$NON-NLS-1$
					+ "}" //$NON-NLS-1$
					+ "", context); //$NON-NLS-1$

			// The contents to execute are actually built at:
			// org.eclipse.e4.core.macros.internal.ComposableMacro.toJSBytes()
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(fFile), "UTF-8"))) { //$NON-NLS-1$
				// Let any exception running it go through.
				// It should define a runMacro() method which we can run later on.
				engine.eval(reader, context);

				// Actually run the macro now.
				engine.eval("runMacro();", context); //$NON-NLS-1$
			}
		} catch (Exception e) {
			throw new MacroPlaybackException(
					MessageFormat.format(Messages.SavedJSMacro_MacrosEvalError, e.getMessage()), e);
		}
	}
}
