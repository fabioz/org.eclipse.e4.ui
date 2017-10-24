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
package org.eclipse.e4.ui.macros.internal.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.e4.core.macros.Activator;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.MacroPlaybackException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Activates the playback of the last macro.
 */
public class MacroPlaybackAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		try {
			PlatformUI.getWorkbench().getService(EMacroService.class).playbackLastMacro();
		} catch (MacroPlaybackException e) {
			Activator.log(e);
			IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
			if (activeWorkbenchWindow != null) {
				// When it comes from evaluating JS it adds
				// "org.eclipse.e4.core.macros.MacroPlaybackException: "
				// which isn't really interesting to the user (so, just remove that part).
				String msg = e.getMessage().replace("org.eclipse.e4.core.macros.MacroPlaybackException: ", ""); //$NON-NLS-1$//$NON-NLS-2$
				MessageDialog.openError(activeWorkbenchWindow.getShell(),
						Messages.MacroPlaybackAction_ErrorRunningMacro, msg);
			}

		}
		return null;
	}
}
