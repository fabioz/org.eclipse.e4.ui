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

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.ui.macros.IMacroAction;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class ApplicationMacroPlayer {
	@Inject
	MApplication application;
	@Inject
	Display display;

	private LinkedList<IMacroAction> recording;

	public void replay() {
		recording = new LinkedList<>((List<IMacroAction>) application.getTransientData()
				.get(ApplicationMacroRecorder.ACTIVE_RECORDING_MACRO_KEY));
		if (recording == null || recording.isEmpty()) {
			System.out.println("Recording is empty");
			return;
		}

		hookListeners();
		Event e = new Event();
		e.display = display;
		while (!display.isDisposed() && processNext(e)) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		unhookListeners();
	}

	/** Return true if still more to come */
	private boolean processNext(Event e) {
		if (recording.isEmpty()) {
			unhookListeners();
			// System.out.println("REPLAY FINISHED");
			return false;
		}
		IMacroAction nextAction = recording.peek();

		switch (nextAction.process(e)) {
		case NEXT:
			// System.out.println("PROCESSED " + nextAction);
			recording.removeFirst();
			return true;
		case WAITING:
			// FIXME: set display timeout
			// System.out.println("WAITING ON " + nextAction);
			return true;
		case ABORT:
		default:
			unhookListeners();
			System.out.println("REPLAY ABORTED");
			return false;
		}
	}

	private void hookListeners() {
		display.addFilter(SWT.Activate, shellActivatedListener);
		display.addFilter(SWT.FocusIn, focusInListener);
	}

	private void unhookListeners() {
		display.removeFilter(SWT.Activate, shellActivatedListener);
		display.removeFilter(SWT.FocusIn, focusInListener);
	}

	private Listener shellActivatedListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (event.widget instanceof Shell) {
				processNext(event);
			}
		}
	};

	private Listener focusInListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			processNext(event);
		}
	};
}
