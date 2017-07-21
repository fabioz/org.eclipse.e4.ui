/******************************************************************************* 
 * Copyright (c) 2017 Exyte  
 * 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 *     Yuri Strot - initial API and Implementation 
 *******************************************************************************/
package org.eclipse.ui.glance.sources;

import static org.eclipse.jface.preference.PreferenceConverter.getColor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ui.glance.internal.GlancePlugin;
import org.eclipse.ui.glance.internal.preferences.IPreferenceConstants;
import org.eclipse.ui.glance.internal.preferences.TreeColors;

/**
 * @author Yuri Strot
 * 
 */
public class ColorManager implements IPropertyChangeListener, IPreferenceConstants {

	public static final String ANNOTATION_ID = "org.eclipse.ui.glance.highlight";
	public static final String ANNOTATION_SELECTED_ID = "org.eclipse.ui.glance.select";

	private ColorManager() {
		getStore().addPropertyChangeListener(this);
		GlancePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		updateColors();
	}

	public static IPreferenceStore getStore() {
		return EditorsUI.getPreferenceStore();
	}

	public static ColorManager getInstance() {
		if (INSTANCE == null)
			INSTANCE = new ColorManager();
		return INSTANCE;
	}

	public Color getBackgroundColor() {
		return selection;
	}

	public Color getSelectedBackgroundColor() {
		return highlight;
	}

	public Color getTreeSelectionBg() {
		return treeBg;
	}

	public Color getTreeSelectionFg() {
		return treeFg;
	}

	public boolean isUseNative() {
		return useNative;
	}

	public void propertyChange(final PropertyChangeEvent event) {
		if (COLOR_HIGHLIGHT.equals(event.getProperty()) || COLOR_SELECTION.equals(event.getProperty())) {
			updateColors();
		}
	}

	public static Color lighten(Color color, int delta) {
		int r = ensureColor(color.getRed() + delta);
		int g = ensureColor(color.getGreen() + delta);
		int b = ensureColor(color.getBlue() + delta);
		return new Color(color.getDevice(), r, g, b);
	}

	private static int ensureColor(int value) {
		return value > 255 ? 255 : value;
	}

	private void updateColors() {
		for (Color color : toDispose) {
			color.dispose();
		}
		toDispose = new ArrayList<Color>();

		final Display display = PlatformUI.getWorkbench().getDisplay();
		final IPreferenceStore store = getStore();

		selection = new Color(display, getColor(store, COLOR_HIGHLIGHT));
		highlight = new Color(display, getColor(store, COLOR_SELECTION));

		TreeColors colors = TreeColors.getDefault();
		useNative = colors.isUseNative();
		if (colors.getBg() != null) {
			treeBg = new Color(display, colors.getBg());
			toDispose.add(treeBg);
		} else {
			treeBg = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
		}

		if (colors.getFg() != null) {
			treeFg = new Color(display, colors.getFg());
			toDispose.add(treeFg);
		} else {
			treeFg = display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
		}

		toDispose.add(selection);
		toDispose.add(highlight);
	}

	private static ColorManager INSTANCE;

	private Color selection;
	private Color highlight;
	private Color treeBg;
	private Color treeFg;
	private boolean useNative;

	private List<Color> toDispose = new ArrayList<Color>();
}
