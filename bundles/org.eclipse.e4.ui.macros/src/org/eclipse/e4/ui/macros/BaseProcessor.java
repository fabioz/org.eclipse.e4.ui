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

/** A processor that does nothing */
public class BaseProcessor implements IMacroActionProcessor {

	@Override
	public void started() {
	}

	@Override
	public void process(IMacroAction action) {
	}

	@Override
	public void finished() {
	}

	@Override
	public void aborted() {
	}
}
