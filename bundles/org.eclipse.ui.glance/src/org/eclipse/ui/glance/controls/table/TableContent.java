/*******************************************************************************
 * Copyright (c) 2017 Exyte
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Yuri Strot - initial API and Implementation
 ******************************************************************************/
package org.eclipse.ui.glance.controls.table;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.widgets.Table;

import org.eclipse.ui.glance.controls.decor.IPath;
import org.eclipse.ui.glance.controls.decor.IStructContent;
import org.eclipse.ui.glance.controls.decor.StructCell;
import org.eclipse.ui.glance.sources.ITextBlock;
import org.eclipse.ui.glance.sources.ITextSourceListener;

public class TableContent implements IStructContent {

	private final ListenerList listeners = new ListenerList();

	public TableContent(Table table) {
	}

	@Override
	public void addListener(ITextSourceListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ITextSourceListener listener) {
		listeners.remove(listener);
	}

	@Override
	public ITextSourceListener[] getListeners() {
		Object[] objects = listeners.getListeners();
		ITextSourceListener[] listeners = new ITextSourceListener[objects.length];
		System.arraycopy(objects, 0, listeners, 0, objects.length);
		return listeners;
	}

	@Override
	public void dispose() {
	}

	@Override
	public ITextBlock[] getBlocks() {
		return null;
	}

	@Override
	public ITextBlock getContent(StructCell cell) {
		return null;
	}

	@Override
	public IPath getPath(ITextBlock block) {
		return null;
	}

	@Override
	public void index(IProgressMonitor monitor) {
		monitor.done();
	}

}
