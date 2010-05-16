/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.demo.simpleide.editor.text;

import javax.inject.Inject;

import org.eclipse.e4.demo.simpleide.editor.IDocumentInput;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public class PlainTextEditor {
	
	@Inject
	public PlainTextEditor(Composite parent, IDocumentInput input) {
		parent.setLayout(new FillLayout());
		TextViewer viewer = new TextViewer(parent, SWT.NONE);
		viewer.setDocument(input.getDocument());
	}
}
