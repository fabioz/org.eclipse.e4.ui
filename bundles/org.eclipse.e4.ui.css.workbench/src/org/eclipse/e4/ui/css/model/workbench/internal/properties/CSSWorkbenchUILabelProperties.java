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
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;

/**
 * Handles {@link MUILabel} properties for the icon, label, and tooltip text.
 */
public class CSSWorkbenchUILabelProperties implements ICSSPropertyHandler {

	private static final String PROPERTY_TOOLTIP = "wm-tooltip";
	private static final String PROPERTY_LABEL = "wm-label";
	private static final String PROPERTY_ICON = "icon";

	public boolean applyCSSProperty(Object element, String property,
			CSSValue value, String pseudo, CSSEngine engine) throws Exception {
		Element wbElement = engine.getElement(element);
		if (!(element instanceof WMUIElement && ((WMUIElement) wbElement).getUIElement() instanceof MUILabel)) {
			return false;
		}
		MUILabel uiElement = (MUILabel) ((WMUIElement) wbElement)
				.getUIElement();
		if (PROPERTY_ICON.equals(property)) {
			if (value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE
					&& ((CSSPrimitiveValue) value).getPrimitiveType() == CSSPrimitiveValue.CSS_URI) {
				uiElement.setIconURI(((CSSPrimitiveValue) value)
						.getStringValue());
				return true;
			}
		} else if (PROPERTY_LABEL.equals(property)) {
			uiElement.setLabel(value.getCssText());
			return true;
		} else if (PROPERTY_TOOLTIP.equals(property)) {
			uiElement.setTooltip(value.getCssText());
			return true;
		}
		return false;
	}

	public String retrieveCSSProperty(Object element, String property,
			String pseudo, CSSEngine engine) throws Exception {
		Element wbElement = engine.getElement(element);
		if (!(element instanceof WMUIElement && ((WMUIElement) wbElement)
				.getUIElement() instanceof MUILabel)) {
			return null;
		}
		MUILabel uiElement = (MUILabel) ((WMUIElement) wbElement)
				.getUIElement();
		if (PROPERTY_ICON.equals(property)) {
			return uiElement.getIconURI();
		} else if (PROPERTY_LABEL.equals(property)) {
			return uiElement.getLabel();
		} else if (PROPERTY_TOOLTIP.equals(property)) {
			return uiElement.getTooltip();
		}
		return null;
	}

}
