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
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.macros.BaseProcessor;
import org.eclipse.e4.ui.macros.IMacroActionProcessor;
import org.eclipse.e4.ui.macros.IMacroHook;
import org.eclipse.e4.ui.macros.IMacroHook.Mode;
import org.eclipse.e4.ui.model.application.MApplication;
import org.osgi.framework.FrameworkUtil;

public class ToggleRecordingHandler {
	private static String pluginId = FrameworkUtil.getBundle(ToggleRecordingHandler.class).getSymbolicName();

	@Execute
	public void execute(MApplication application) {
		IEclipseContext context = application.getContext();
		assert context != null;
		GestureInterpreter interpreter = context.get(GestureInterpreter.class);
		if (interpreter == null) {
			// start recording
			interpreter = ContextInjectionFactory.make(GestureInterpreter.class, context);

			IMacroActionProcessor recorder = ContextInjectionFactory.make(ApplicationMacroRecorder.class, context);
			// interpreter.addListener(recorder);

			DebuggingProcessor debugging = ContextInjectionFactory.make(DebuggingProcessor.class, context);
			debugging.setDelegateRecorder(recorder);
			interpreter.addListener(debugging);

			openMacroWindow(interpreter, application);
			start(interpreter, application);
		} else {
			finish(interpreter, application);
		}
	}

	private void start(GestureInterpreter interpreter, final MApplication application) {
		IStatus rc = runStartHooks(application, IMacroHook.Mode.RECORDING);
		if (!rc.isOK()) {
			runFinishHooks(application, IMacroHook.Mode.RECORDING);
		} else {
			interpreter.addListener(new BaseProcessor() {
				@Override
				public void finished() {
					runFinishHooks(application, IMacroHook.Mode.RECORDING);
				}

				@Override
				public void aborted() {
					runFinishHooks(application, IMacroHook.Mode.RECORDING);
				}
			});
			interpreter.start();
		}
	}

	private void finish(GestureInterpreter interpreter, MApplication application) {
		interpreter.finish();
	}

	private void openMacroWindow(final GestureInterpreter interpreter, final MApplication application) {
		MacroStatusPopup.monitor(interpreter);
	}

	// should probably be moved outside of the handlers
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
