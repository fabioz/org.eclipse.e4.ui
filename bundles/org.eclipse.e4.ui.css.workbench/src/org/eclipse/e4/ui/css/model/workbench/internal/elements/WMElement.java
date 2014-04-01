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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.e4.ui.css.core.dom.ElementAdapter;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents a Modelled Workbench element; it wraps most of
 * {@link MApplicationElement}.
 */
public class WMElement extends ElementAdapter {
	/**
	 * Convenience method for getting the CSS class of a widget.
	 * 
	 * @param widget
	 *            SWT widget with associated CSS class name
	 * @return CSS class name
	 */
	public static String getCSSClass(MApplicationElement element) {
		return join(element.getTags());
	}

	private static String join(Collection<String> tags) {
		if (tags.isEmpty()) {
			return "";
		}
		if (tags.size() == 1) {
			return tags instanceof List<?> ? ((List<String>) tags).get(0)
					: tags.iterator().next();
		}
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> it = tags.iterator(); it.hasNext(); sb
				.append(' ')) {
			sb.append(it.next());
		}
		return sb.toString();
	}

	/**
	 * Convenience method for getting the CSS ID of a widget.
	 * 
	 * @param widget
	 *            SWT widget with associated CSS id
	 * @return CSS ID
	 */
	public static String getID(MApplicationElement element) {
		return element.getElementId();
	}

	protected String localName;
	protected String namespaceURI;

	public WMElement(MApplicationElement element, CSSEngine engine) {
		super(element, engine);
		EClass eclass = element instanceof EObject ? ((EObject) element)
				.eClass() : null;
		localName = eclass == null ? element.getClass().getSimpleName()
				: eclass.getName();
		namespaceURI = eclass == null ? element.getClass().getPackage()
				.getName() : eclass.getEPackage().getNsURI();
	}

	public String getNamespaceURI() {
		return namespaceURI;
	}

	public String getCSSId() {
		return getID(getUIElement());
	}

	public String getCSSClass() {
		return getCSSClass(getUIElement());
	}

	public String getCSSStyle() {
		return "";
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	/**
	 * Check for attribute. Attributes are looked up in the modelled workbench
	 * element's persisted and transient data maps too.
	 */
	@Override
	public String getAttribute(String attr) {
		MUIElement element = getUIElement();
		if ("class".equals(attr)) {
			return getCSSClass(element);
		}
		String value = element.getPersistedState().get(attr);
		if (value != null) {
			return value;
		}
		Object tvalue = element.getTransientData().get(attr);
		if (tvalue != null) {
			return tvalue.toString();
		}
		return "";
	}

	@Override
	public boolean isPseudoInstanceOf(String s) {
		if (getUIElement().getTags().contains(s)) {
			return true;
		}
		return super.isPseudoInstanceOf(s);
	}

	/** Return the modelled workbench element */
	public MUIElement getUIElement() {
		return (MUIElement) getNativeWidget();
	}

	/********** CHILDREN **********/

	public NodeList getChildNodes() {
		if (getNativeWidget() instanceof MElementContainer<?>) {
			return new ListBasedNodeList(
					((MElementContainer<?>) getNativeWidget()).getChildren(),
					engine);
		}
		return new ListBasedNodeList(Collections.emptyList(), engine);
	}

	public Node getParentNode() {
		return getElement(getUIElement().getParent());
	}
}
