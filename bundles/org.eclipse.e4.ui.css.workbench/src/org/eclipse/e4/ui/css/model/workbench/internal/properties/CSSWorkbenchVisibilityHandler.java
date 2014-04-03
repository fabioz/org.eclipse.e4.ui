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
package org.eclipse.e4.ui.css.model.workbench.internal.properties;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMUIElement;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSValue;

public class CSSWorkbenchVisibilityHandler implements ICSSPropertyHandler {

	public boolean applyCSSProperty(Object element, String property,
			CSSValue value, String pseudo, CSSEngine engine) throws Exception {
		Element wbElement = engine.getElement(element);
		if (!(wbElement instanceof WMUIElement)) {
			return false;
		}
		MUIElement uiElement = ((WMUIElement) wbElement).getUIElement();
		if ("visibility".equals(property)) {
			uiElement.setVisible(!"hidden".equals(value.getCssText()));
		} else if ("wm-toBeRendered".equals(property)) {
			Boolean asBoolean = (Boolean) engine.getCSSValueConverter(
					Boolean.class).convert(value, engine, null);
			uiElement.setToBeRendered(asBoolean);
		} else {
			return false;
		}
		return true;
	}

	public String retrieveCSSProperty(Object element, String property,
			String pseudo, CSSEngine engine) throws Exception {
		Element wbElement = engine.getElement(element);
		if (!(wbElement instanceof WMUIElement)) {
			return null;
		}
		MUIElement uiElement = ((WMUIElement) wbElement).getUIElement();
		if ("visibility".equals(property)) {
			return uiElement.isVisible() ? "visible" : "hidden";
		} else if ("wm-toBeRendered".equals(property)) {
			return Boolean.toString(uiElement.isToBeRendered());
		}
		return null;
	}

}
