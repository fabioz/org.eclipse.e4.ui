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

import java.util.Map;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMUIElement;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
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
		if (!(wbElement instanceof WMUIElement && ((WMUIElement) wbElement)
				.getUIElement() instanceof MUILabel)) {
			return false;
		}
		MUILabel uiElement = (MUILabel) ((WMUIElement) wbElement)
				.getUIElement();
		if (PROPERTY_LABEL.equals(property)) {
			String label = value.getCssText();
			uiElement.setLabel(label);
			return true;
		} else if (PROPERTY_ICON.equals(property)) {
			// Must check and remove an override (e.g., on MParts)
			if (value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE
					&& ((CSSPrimitiveValue) value).getPrimitiveType() == CSSPrimitiveValue.CSS_URI) {
				String iconURI = ((CSSPrimitiveValue) value).getStringValue();
				if (uiElement instanceof MApplicationElement) {
					Map<String, Object> td = ((MApplicationElement) uiElement)
							.getTransientData();
					if (!iconURI.equals(td.get(PROPERTY_ICON))) {
						td.put(PROPERTY_ICON, iconURI);
						td.remove(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY);
						uiElement.setIconURI(iconURI);
					}
				} else {
					uiElement.setIconURI(iconURI);
				}
				return true;
			}
		} else if (PROPERTY_TOOLTIP.equals(property)) {
			// Must check and remove an override (e.g., on MParts)
			String tooltip = value.getCssText();
			if (uiElement instanceof MApplicationElement) {
				Map<String, Object> td = ((MApplicationElement) uiElement)
						.getTransientData();
				if (!tooltip.equals(td.get(PROPERTY_TOOLTIP))) {
					td.put(PROPERTY_TOOLTIP, tooltip);
					td.remove(IPresentationEngine.OVERRIDE_TITLE_TOOL_TIP_KEY);
					uiElement.setTooltip(tooltip);
				}
			} else {
				uiElement.setTooltip(tooltip);
			}
			return true;
		}
		return false;
	}

	public String retrieveCSSProperty(Object element, String property,
			String pseudo, CSSEngine engine) throws Exception {
		Element wbElement = engine.getElement(element);
		if (!(wbElement instanceof WMUIElement)) {
			return null;
		}
		MUILabel uiElement = null;
		if (((WMUIElement) wbElement).getUIElement() instanceof MUILabel) {
			uiElement = (MUILabel) ((WMUIElement) wbElement).getUIElement();
		} else if (((WMUIElement) wbElement).getUIElement() instanceof MPlaceholder
				&& ((MPlaceholder) ((WMUIElement) wbElement).getUIElement())
						.getRef() instanceof MUILabel) {
			uiElement = (MUILabel) ((MPlaceholder) ((WMUIElement) wbElement)
					.getUIElement()).getRef();
		}
		if (uiElement == null) {
			return null;
		}
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
