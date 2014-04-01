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

import java.util.List;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ListBasedNodeList implements NodeList {
	private List<?> elements;
	private CSSEngine engine;

	public ListBasedNodeList(List<?> elements, CSSEngine engine) {
		this.elements = elements;
		this.engine = engine;
	}

	public int getLength() {
		return elements.size();
	}

	public Node item(int index) {
		return engine.getElement(elements.get(index));
	}
}
