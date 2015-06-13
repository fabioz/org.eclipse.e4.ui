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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.ui.macros.IMacroAction;
import org.eclipse.e4.ui.macros.IMacroActionProcessor;
import org.eclipse.e4.ui.model.application.MApplication;

public class ApplicationMacroRecorder implements IMacroActionProcessor {
	static final String MACRO_RECORDING_KEY = "macro:recorded";
	static final String ACTIVE_RECORDING_MACRO_KEY = "macro:current";

	@Inject
	MApplication application;

	@Inject
	EHandlerService handlerService;

	@Override
	public void started() {
		application.getTransientData().put(ACTIVE_RECORDING_MACRO_KEY, new ArrayList<IMacroAction>());
	}

	@Override
	public void finished() {
		@SuppressWarnings("unchecked")
		List<IMacroAction> recording = (List<IMacroAction>) application.getTransientData()
				.get(ACTIVE_RECORDING_MACRO_KEY);
		if (recording != null && !recording.isEmpty()) {
			application.getTransientData().put(MACRO_RECORDING_KEY, recording);
		}
	}

	@Override
	public void aborted() {
		application.getTransientData().remove(ACTIVE_RECORDING_MACRO_KEY);
	}

	@Override
	public void process(IMacroAction action) {
		List<IMacroAction> recording = (List<IMacroAction>) application.getTransientData()
				.get(ACTIVE_RECORDING_MACRO_KEY);
		recording.add(action);
	}
}
