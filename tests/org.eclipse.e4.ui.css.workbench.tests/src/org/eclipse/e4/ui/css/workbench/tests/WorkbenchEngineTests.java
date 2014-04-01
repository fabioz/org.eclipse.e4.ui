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
package org.eclipse.e4.ui.css.workbench.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.model.workbench.internal.CSSWorkbenchEngine;
import org.eclipse.e4.ui.css.model.workbench.internal.elements.WMPartElement;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WorkbenchEngineTests {
	private MApplication app;
	private MTrimmedWindow win;
	private CSSWorkbenchEngine engine;
	private MPart part1;
	private MPart part2;
	private MToolControl toolControl;

	@Before
	public void setUp() {
		MApplicationFactory applicationFactory = MApplicationFactory.INSTANCE;
		MBasicFactory basicFactory = MBasicFactory.INSTANCE;
		MMenuFactory menuFactory = MMenuFactory.INSTANCE;

		app = applicationFactory.createApplication();
		app.setElementId("app");
		win = basicFactory.createTrimmedWindow();
		win.setElementId("win");
		MPartSashContainer psc = basicFactory.createPartSashContainer();

		part1 = basicFactory.createPart();
		part1.setElementId("part1");
		part1.getTags().add("partTag");
		part1.getTags().add("tag1");

		part2 = basicFactory.createPart();
		part2.setElementId("part2");
		part2.getTags().add("partTag");
		part2.getTags().add("tag2");

		app.getChildren().add(win);
		win.getChildren().add(psc);
		psc.getChildren().add(part1);
		psc.getChildren().add(part2);

		toolControl = menuFactory.createToolControl();
		toolControl.setElementId("toolControl");

		MTrimBar tb = basicFactory.createTrimBar();
		tb.setSide(SideValue.TOP);
		tb.getChildren().add(toolControl);
		win.getTrimBars().add(tb);

		engine = new CSSWorkbenchEngine();
		engine.init(app, RegistryFactory.getRegistry());
	}

	@After
	public void tearDown() {
		if (engine != null) {
			engine.dispose();
		}
		engine = null;
	}

	public void clearAndApply(CSSEngine engine, MApplicationElement o,
			String styleSheet) {

		// Forget all previous styles
		engine.reset();

		try {
			engine.parseStyleSheet(new StringReader(styleSheet));
		} catch (IOException e) {
			fail(e.getMessage());
		}

		engine.applyStyles(o, true, true);
	}

	@Test
	public void testSelectorById() throws IOException {
		Collection<Element> matched = findElements("#part1");
		assertEquals(1, matched.size());
		Element e = matched.iterator().next();
		assertTrue(e instanceof WMPartElement);
		assertTrue(((WMPartElement) e).getNativeWidget() == part1);
	}

	@Test
	public void testSelectorByClass() throws IOException {
		Collection<Element> matched = findElements(".partTag");
		assertEquals(2, matched.size());
		for (Element e : matched) {
			assertTrue(e instanceof WMPartElement);
			assertTrue(((WMPartElement) e).getNativeWidget() == part1
					|| ((WMPartElement) e).getNativeWidget() == part2);
		}
	}

	private Collection<Element> findElements(String selectorText)
			throws IOException {
		SelectorList selectors = engine.parseSelectors(selectorText);
		assertEquals(1, selectors.getLength());
		Selector selector = selectors.item(0);

		List<Element> matched = new LinkedList<Element>();
		findElements(engine, selector, app, matched);
		return matched;
	}

	private void findElements(CSSWorkbenchEngine engine, Selector selector,
			Object root, List<Element> matched) {
		Element e = engine.getElement(root);
		if (engine.matches(selector, root, null)) {
			matched.add(e);
		}
		NodeList childNodes = e.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			findElements(engine, selector, childNodes.item(i), matched);
		}
	}

	@Test
	public void testVisibility() {
		assertTrue(part1.isVisible());
		assertTrue(part2.isVisible());
		assertTrue(win.isVisible());

		clearAndApply(engine, app, "#part1 { visibility: false; }");
		assertFalse(part1.isVisible());
		assertTrue(part2.isVisible());
		assertTrue(win.isVisible());

		String value = engine.retrieveCSSProperty(part1, "visibility", "");
		assertEquals("false", value);
	}

	@Test
	public void testToBeRendered() {
		assertTrue(part1.isToBeRendered());
		assertTrue(part2.isToBeRendered());
		assertTrue(win.isToBeRendered());

		clearAndApply(engine, app, "#part1 { wm-toBeRendered: false; }");
		assertFalse(part1.isToBeRendered());
		assertTrue(part2.isToBeRendered());
		assertTrue(win.isToBeRendered());

		String value = engine.retrieveCSSProperty(part1, "wm-toBeRendered", "");
		assertEquals("false", value);
	}

	@Test
	public void testIcon() {
		assertNull(part1.getIconURI());

		clearAndApply(engine, app, "#part1 { icon: url(about:none); }");
		assertNotNull(part1.getIconURI());

		String value = engine.retrieveCSSProperty(part1, "icon", "");
		assertEquals("about:none", value);
	}

	@Test
	public void testLabel() {
		assertNull(part1.getLabel());

		clearAndApply(engine, app, "#part1 { wm-label: 'Test'; }");
		assertEquals("Test", part1.getLabel());

		String value = engine.retrieveCSSProperty(part1, "wm-label", "");
		assertEquals("Test", value);
	}

	@Test
	public void testTooltip() {
		assertNull(part1.getTooltip());

		clearAndApply(engine, app, "#part1 { wm-tooltip: 'Test'; }");
		assertEquals("Test", part1.getTooltip());

		String value = engine.retrieveCSSProperty(part1, "wm-tooltip", "");
		assertEquals("Test", value);
	}

}
