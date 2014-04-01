/*******************************************************************************
 * Copyright (c) 2014 Manumitting Technologies Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Brian de Alwis (MTI) - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.css.model.workbench.internal.elements;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Exposes an MPart
 */
public class WMPartElement extends WMUIElement {

	public WMPartElement(MUIElement element, CSSEngine engine) {
		super(element, engine);
	}

	@Override
	public boolean isPseudoInstanceOf(String s) {
		if ("active".equals(s)) {
			return ((MPart) getNativeWidget()).getTags().contains("active"); // CSSConstants.CSS_ACTIVE_CLASS
		}
		if ("dirty".equals(s)) {
			return ((MPart) getNativeWidget()).isDirty();
		}
		if ("closeable".equals(s)) {
			return ((MPart) getNativeWidget()).isCloseable();
		}
		return super.isPseudoInstanceOf(s);
	}

}
