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
import org.eclipse.e4.ui.model.application.ui.MUILabel;

/**
 * Expose an {@link MUIElement} and {@link MUILabel} element.
 */
public class WMUIElement extends WMElement {

	public WMUIElement(MUIElement element, CSSEngine engine) {
		super(element, engine);
	}

}
