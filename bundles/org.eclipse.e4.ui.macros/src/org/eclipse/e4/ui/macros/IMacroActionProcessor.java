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
package org.eclipse.e4.ui.macros;

/**
 * A party interested in receiving macro actions (e.g., to record them for
 * playback, or providing a UI for visualizing the status). The idea behind the
 * {@link IMacroActionProcessor}s was that there could could be intermediates that coalesce
 * some of these actions, but it hasn't proven necessary so far.
 */
public interface IMacroActionProcessor {

	/** A macro recording has begun */
	void started();

	/** The macro recording has finished */
	void finished();

	/** The macro recording has been aborted */
	void aborted();

	/** An action occurred during the macro */
	void process(IMacroAction action);
}
