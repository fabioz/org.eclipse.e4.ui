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

import java.util.ArrayList;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.w3c.dom.NodeList;

public class WMTrimmedWindow extends WMUIElement {

	public WMTrimmedWindow(MTrimmedWindow element, CSSEngine engine) {
		super(element, engine);
	}

	public NodeList getChildNodes() {
		MTrimmedWindow win = (MTrimmedWindow) getNativeWidget();
		ArrayList<MApplicationElement> children = new ArrayList<MApplicationElement>();
		children.addAll(win.getChildren());
		for (MTrimBar tb : win.getTrimBars()) {
			children.addAll(tb.getChildren());
		}
		return new ListBasedNodeList(children, engine);
	}
}
