/*******************************************************************************
 * Copyright (c) 2015 Manumitting Technologies Inc and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manumitting Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.internal.macros;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.macros.IMacroHook;
import org.eclipse.e4.ui.macros.IMacroHook.Mode;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.osgi.framework.FrameworkUtil;

public class ReplayMacroHandler {
	private static String pluginId = FrameworkUtil.getBundle(ToggleRecordingHandler.class).getSymbolicName();

	@CanExecute
	public boolean canExecute(MApplication application) {
		// if we have hooks then a macro recording or replay is in progress
		return application.getTransientData().get(MacroHooksManager.class.getName()) == null;
	}

	@Execute
	public void execute(MApplication application, Display display, @Optional Event triggeringEvent) {
		IEclipseContext context = application.getContext();
		assert context != null; // still not certain if this is required

		waitForModifiers(display, triggeringEvent);

		ApplicationMacroPlayer player = ContextInjectionFactory.make(ApplicationMacroPlayer.class, context);
		IStatus rc = runStartHooks(application, IMacroHook.Mode.PLAYBACK);
		try {
			if (rc.isOK()) {
				player.replay();
			} else {
				System.err.println("Playback aborted: " + rc);
			}
		} finally {
			runFinishHooks(application, IMacroHook.Mode.PLAYBACK);
		}
	}

	/**
	 * The triggering event is likely a KeyDown event; wait for any modifier
	 * keys to be released
	 * @param display
	 * @param triggeringEvent (may be null)
	 */
	private void waitForModifiers(Display display, Event triggeringEvent) {
		if (triggeringEvent == null || (triggeringEvent.type != SWT.KeyUp && triggeringEvent.type != SWT.KeyDown)
				|| (triggeringEvent.type == SWT.KeyDown
						&& ((triggeringEvent.stateMask | triggeringEvent.keyCode) & SWT.MODIFIER_MASK) == 0)
				|| (triggeringEvent.type == SWT.KeyUp
						&& (triggeringEvent.stateMask & ~(triggeringEvent.keyCode & SWT.MODIFIER_MASK)) == 0)) {
			return;
		}
		final boolean ready[] = { false };
		Listener keyUpListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				// stateMask has the modifiers *prior* to this KeyUp so, providing
				// this keyCode is a modifier, we need to remove it
				int stateMask = event.stateMask & ~(event.keyCode & SWT.MODIFIER_MASK);
				ready[0] = stateMask == 0;
			}
		};

		// Wait for the keyboard modifiers to be released
		display.addFilter(SWT.KeyUp, keyUpListener);
		while (!ready[0] && !display.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.removeFilter(SWT.KeyUp, keyUpListener);
	}

	private IStatus runStartHooks(MApplication application, Mode mode) {
		MacroHooksManager mgr = (MacroHooksManager) application.getTransientData()
				.get(MacroHooksManager.class.getName());
		if (mgr != null) {
			return new Status(IStatus.ERROR, pluginId, "Macro already in progress");
		}
		mgr = ContextInjectionFactory.make(MacroHooksManager.class, application.getContext());
		application.getTransientData().put(MacroHooksManager.class.getName(), mgr);
		return mgr.runStartHooks(mode);
	}

	private IStatus runFinishHooks(MApplication application, Mode mode) {
		MacroHooksManager mgr = (MacroHooksManager) application.getTransientData()
				.remove(MacroHooksManager.class.getName());
		if (mgr == null) {
			return new Status(IStatus.ERROR, pluginId, "No macro in progress");
		}
		mgr.runFinishHooks(mode);
		return Status.OK_STATUS;
	}

}
