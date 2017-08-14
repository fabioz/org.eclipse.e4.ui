/*******************************************************************************
 * Copyright (c) 2012 Stephan Brosinski
 *
 * All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Stephan Brosinski - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.regex.view;

import org.eclipse.ui.regex.event.ListenerManager;

public class LiveEvalListenerManager extends ListenerManager<ILiveEvalListener> {

	public void publishEvalActivated() {
		getListeners().forEach(listener -> listener.evalActivated());
	};

	public void publishEvalDeactivated() {
		getListeners().forEach(listener -> listener.evalDeactivated());
	};

	public void publishEvalDone() {
		getListeners().forEach(listener -> listener.evalDone());
	};

	public void publishDoEval() {
		getListeners().forEach(listener -> listener.doEval());
	};

}
