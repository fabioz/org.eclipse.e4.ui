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
import org.eclipse.e4.ui.css.model.workbench.internal.CSSWorkbenchEngine;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMElement;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.css.CSSValue;

public class CSSWorkbenchRendererPropertyHandler implements ICSSPropertyHandler {

	public boolean applyCSSProperty(Object element, String property,
			CSSValue value, String pseudo, CSSEngine engine) throws Exception {
		WMElement wbElement = (WMElement) engine.getElement(element);
		if (wbElement == null) {
			return false;
		}
		MUIElement uiElement = wbElement.getUIElement();
		String uri = value.getCssText();
		Object renderer = CSSWorkbenchEngine.createObject(engine, uri);
		if (renderer == null) {
			return false;
		}
		uiElement.setRenderer(renderer);
		return true;
	}

	public String retrieveCSSProperty(Object element, String property,
			String pseudo, CSSEngine engine) throws Exception {
		WMElement wbElement = (WMElement) engine.getElement(element);
		if (wbElement == null) {
			return null;
		}
		MUIElement uiElement = wbElement.getUIElement();
		Object renderer = uiElement.getRenderer();
		if (renderer == null) {
			return null;
		}
		Bundle b = FrameworkUtil.getBundle(renderer.getClass());
		if (b == null) {
			return null;
		}
		return "bundleclass://" + b.getSymbolicName() + "/"
				+ renderer.getClass().getName();
	}

}
