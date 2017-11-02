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
package org.eclipse.e4.ui.macros.internal.keybindings;

import javax.inject.Inject;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.macros.internal.EditorUtils;

/**
 * A macro state listener that will install the execution listener when in a
 * record context.
 */
public class CommandManagerExecutionListenerInstaller implements IMacroStateListener {

	@Inject
	private CommandManager fCommandManager;

	@Inject
	private EHandlerService fHandlerService;

	private CommandManagerExecutionListener fCommandManagerExecutionListener;

	public CommandManagerExecutionListener getCommandManagerExecutionListener() {
		return fCommandManagerExecutionListener;
	}

	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange) {
		if (macroService.isRecording()) {
			if (fCommandManagerExecutionListener == null) {
				fCommandManagerExecutionListener = new CommandManagerExecutionListener(macroService, fHandlerService);
				fCommandManager.addExecutionListener(fCommandManagerExecutionListener);
			}
		} else {
			if (fCommandManagerExecutionListener != null) {
				fCommandManager.removeExecutionListener(fCommandManagerExecutionListener);
				fCommandManagerExecutionListener = null;
			}
		}
	}

	@Override
	public void macroPlaybackContextCreated(IMacroPlaybackContext macroContext) {
		EditorUtils.cacheTargetStyledText(macroContext);
	}

	@Override
	public void macroRecordContextCreated(IMacroRecordContext macroContext) {
		EditorUtils.cacheTargetStyledText(macroContext);
	}
}
