/*******************************************************************************
 * Copyright (c) 2017 Exyte
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yuri Strot - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.glance.controls.text.styled;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;

import org.eclipse.ui.glance.sources.ITextBlock;
import org.eclipse.ui.glance.sources.ITextBlockListener;
import org.eclipse.ui.glance.sources.TextChangedEvent;

/**
 * @author Yuri Strot
 * 
 */
public class StyledTextBlock implements ITextBlock, ExtendedModifyListener {

	public StyledTextBlock(StyledText text) {
		this.text = text;
		listeners = new ListenerList();
		text.addExtendedModifyListener(this);
	}

	@Override
	public String getText() {
		return text.getText();
	}

	@Override
	public void addTextBlockListener(ITextBlockListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeTextBlockListener(ITextBlockListener listener) {
		listeners.remove(listener);
	}
	
	@Override
	public void modifyText(ExtendedModifyEvent event) {
		Object[] objects = listeners.getListeners();
		TextChangedEvent textEvent = new TextChangedEvent(event.start,
				event.length, event.replacedText);
		for (Object object : objects) {
			ITextBlockListener listener = (ITextBlockListener) object;
			listener.textChanged(textEvent);
		}
	}
	
	@Override
	public int compareTo(ITextBlock o) {
		//style text support only one text block
		return 0;
	}

	private StyledText text;
	private ListenerList listeners;

}