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

import org.eclipse.e4.ui.macros.BaseProcessor;
import org.eclipse.e4.ui.macros.IMacroAction;
import org.eclipse.e4.ui.macros.IMacroActionProcessor;

public class DebuggingProcessor implements IMacroActionProcessor {
	private IMacroActionProcessor delegate = new BaseProcessor();

	public void setDelegateRecorder(IMacroActionProcessor delegate) {
		this.delegate = delegate;
	}

	public void started() {
		System.out.println(">> STARTED");
		delegate.started();
	}

	public void finished() {
		System.out.println(">> FINISHED");
		delegate.finished();
	}

	public void aborted() {
		System.out.println(">> ABORTED");
		delegate.finished();
	}

	@Override
	public void process(IMacroAction action) {
		System.out.println(">> " + action.toString());
		delegate.process(action);
	}
}
