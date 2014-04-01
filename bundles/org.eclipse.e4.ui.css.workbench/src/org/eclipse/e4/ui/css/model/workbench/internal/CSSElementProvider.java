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
package org.eclipse.e4.ui.css.model.workbench.internal;

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMElement;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMPartElement;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMTrimmedWindow;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMUIElement;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.w3c.dom.Element;

public class CSSElementProvider implements IElementProvider {

	private static final String ELEMENT_KEY = "CSSWMElement";

	public Element getElement(Object element, CSSEngine engine) {
		if (element instanceof WMElement) {
			return (WMElement) element;
		}
		if (!(element instanceof MApplicationElement)) {
			return null;
		}

		MApplicationElement modelElement = (MApplicationElement) element;
		Object value = modelElement.getTransientData().get(ELEMENT_KEY);
		if (value instanceof WMElement) {
			// Try not to recreate elements
			return (WMElement) value;
		}
		WMElement cssElement = null;
		if (element instanceof MPart) {
			cssElement = new WMPartElement((MPart) element, engine);
		} else if (element instanceof MTrimmedWindow) {
			cssElement = new WMTrimmedWindow((MTrimmedWindow) element, engine);
		} else if (element instanceof MUIElement) {
			cssElement = new WMUIElement((MUIElement) element, engine);
		} else {
			cssElement = new WMElement((MApplicationElement) element, engine);
		}
		modelElement.getTransientData().put(ELEMENT_KEY, cssElement);
		return cssElement;
	}

}
