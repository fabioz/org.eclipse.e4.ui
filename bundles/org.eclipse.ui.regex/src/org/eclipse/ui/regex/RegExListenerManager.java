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

package org.eclipse.ui.regex;

import org.eclipse.ui.regex.event.ListenerManager;

public class RegExListenerManager extends ListenerManager<IRegExListener> {

	public void publishExpressionError(String errMsg) {
		getListeners().forEach(listener -> listener.expressionError(errMsg));
	}

	public void publishFoundMatches(Matches foundMatches) {
		getListeners().forEach(listener -> listener.foundMatches(foundMatches));
	}

	public void publishFoundNoMatches() {
		getListeners().forEach(listener -> listener.foundNoMatches());
	}

	public void publishDoneWithReplace(ReplaceResult result) {
		getListeners().forEach(listener -> listener.doneWithReplace(result));
	}

	public void publishDoneWithSplit(String[] result) {
		getListeners().forEach(listener -> listener.doneWithSplit(result));
	}

	public void updateRequested() {
		getListeners().forEach(listener -> listener.updateRequested());
	}

}
