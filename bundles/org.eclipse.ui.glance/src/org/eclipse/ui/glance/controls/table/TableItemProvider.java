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
package org.eclipse.ui.glance.controls.table;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.ui.glance.controls.items.ItemProvider;

/**
 * @author Yuri Strot
 * 
 */
public class TableItemProvider implements ItemProvider {

	private TableItemProvider() {
	}

	private static TableItemProvider INSTANCE;

	public static TableItemProvider getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TableItemProvider();
		}
		return INSTANCE;
	}

	public static TableItem getItem(Item item) {
		return (TableItem) item;
	}

	@Override
	public Color getBackground(Item item, int index) {
		return getItem(item).getBackground(index);
	}

	@Override
	public int getColumnCount(Item item) {
		return getItem(item).getParent().getColumnCount();
	}

	@Override
	public Color getForeground(Item item, int index) {
		return getItem(item).getForeground(index);
	}

	@Override
	public String getText(Item item, int index) {
		return getItem(item).getText(index);
	}

	@Override
	public Image getImage(Item item, int index) {
		return getItem(item).getImage(index);
	}

	@Override
	public Rectangle getImageBounds(Item item, int index) {
		return getItem(item).getImageBounds(index);
	}

	@Override
	public Rectangle getTextBounds(Item item, int index) {
		return getItem(item).getTextBounds(index);
	}

	@Override
	public Rectangle getBounds(Item item, int index) {
		return getItem(item).getBounds(index);
	}

	@Override
	public Font getFont(Item item, int index) {
		return getItem(item).getFont(index);
	}

	@Override
	public void show(Item item) {
		TableItem tItem = getItem(item);
		tItem.getParent().showItem(tItem);
	}
	
	@Override
	public int compare(Item item1, Item item2) {
		if (item1.equals(item2))
			return 0;
		Table table = getItem(item1).getParent();
		TableItem[] items = table.getItems();
		for (TableItem item : items) {
			if (item1.equals(item))
				return -1;
			if (item2.equals(item))
				return 1;
		}
		return 0;
	}

	@Override
	public void select(Item item) {
		TableItem tItem = getItem(item);
		tItem.getParent().setSelection(tItem);
	}

}
