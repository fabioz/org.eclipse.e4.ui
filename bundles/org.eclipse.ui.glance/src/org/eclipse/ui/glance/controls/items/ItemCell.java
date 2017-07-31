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
package org.eclipse.ui.glance.controls.items;

import org.eclipse.jface.util.Policy;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;

import org.eclipse.ui.glance.sources.ITextBlock;
import org.eclipse.ui.glance.sources.ITextBlockListener;
import org.eclipse.ui.glance.utils.TextUtils;

/**
 * @author Yuri Strot
 * 
 */
public class ItemCell implements ITextBlock {

	private Item item;
	private int index;
	private ItemProvider provider;

	public static final String KEY_TEXT_LAYOUT = Policy.JFACE
			+ "styled_label_key_"; //$NON-NLS-1$

	public ItemCell(Item item, int index, ItemProvider provider) {
		this.item = item;
		this.index = index;
		this.provider = provider;
	}

	public Image getImage() {
		return provider.getImage(item, index);
	}

	public Object getKey() {
		Object data = item.getData();
		if (data != null)
			return data;
		return item;
	}

	/**
	 * @return the item
	 */
	public Item getItem() {
		return item;
	}

	@Override
	public String getText() {
		return provider.getColumnCount(item) == 0 ? item.getText() : provider
				.getText(item, index);
	}

	public int getLength() {
		return getText().length();
	}

	public StyleRange[] getStyles() {
		String key = KEY_TEXT_LAYOUT + index;
		Object data = item.getData(key);
		if (data instanceof StyleRange[]) {
			return TextUtils.copy((StyleRange[]) data);
		}
		return new StyleRange[0];
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	@Override
	public int hashCode() {
		return item.hashCode() ^ index;
	}

	@Override
	public boolean equals(Object obj) {
		ItemCell item = (ItemCell) obj;
		return item.item.equals(this.item) && item.index == index;
	}

	@Override
	public int compareTo(ITextBlock block) {
		ItemCell cell = (ItemCell) block;
		return provider.compare(item, cell.item);
	}

	@Override
	public void addTextBlockListener(ITextBlockListener listener) {
	}

	@Override
	public void removeTextBlockListener(ITextBlockListener listener) {
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{");
		buffer.append(item);
		buffer.append(", ");
		buffer.append(index);
		buffer.append("}");
		return buffer.toString();
	}

}
