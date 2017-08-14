/*******************************************************************************
 * Copyright (c) 2012 Stephan Brosinski
 *
 * All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Stephan Brosinski - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.regex.view;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.MouseListener.mouseUpAdapter;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

public class AssistPopup {
	private Shell shell;
	private List list;
	private int minimumWidth;
	private ArrayList<Proposal> proposals;
	private static final Color bgColor = new Color(Display.getCurrent(), 254, 241, 233);

	public AssistPopup(Shell parent) {
		this(parent, 0);
	}

	public AssistPopup(Shell parent, int style) {
		shell = new Shell(parent, checkStyle(style));

		list = new List(shell, SWT.SINGLE | SWT.V_SCROLL);
		list.setBackground(bgColor);

		// close dialog if user selects outside of the shell
		shell.addListener(SWT.Deactivate, e -> shell.setVisible(false));

		// resize shell when list resizes
		shell.addControlListener(new ControlAdapter() {

			@Override
			public void controlResized(ControlEvent e) {
				Rectangle shellSize = shell.getClientArea();
				list.setSize(shellSize.width, shellSize.height);
			}
		});

		// return list selection on Mouse Up or Carriage Return
		list.addMouseListener(mouseUpAdapter(e -> shell.setVisible(false)));
		list.addKeyListener(keyPressedAdapter(e -> {
			if (e.character == '\r') {
				shell.setVisible(false);
			}
		}));
	}

	private static int checkStyle(int style) {
		int mask = SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		return style & mask;
	}

	public Proposal open(Rectangle rect) {

		Point listSize = list.computeSize(rect.width, SWT.DEFAULT);
		Rectangle screenSize = shell.getDisplay().getBounds();

		// Position the dialog so that it does not run off the screen and the largest
		// number of items are visible
		int spaceBelow = screenSize.height - (rect.y + rect.height) - 30;
		int spaceAbove = rect.y - 30;

		int y = 0;
		if (spaceAbove > spaceBelow && listSize.y > spaceBelow) {
			// place popup list above table cell
			if (listSize.y > spaceAbove) {
				listSize.y = spaceAbove;
			} else {
				listSize.y += 2;
			}
			y = rect.y - listSize.y;

		} else {
			// place popup list below table cell
			if (listSize.y > spaceBelow) {
				listSize.y = spaceBelow;
			} else {
				listSize.y += 2;
			}
			y = rect.y + rect.height;
		}

		// Make dialog as wide as the cell
		listSize.x = rect.width;
		// dialog width should not be les than minimumwidth
		if (listSize.x < minimumWidth)
			listSize.x = minimumWidth;

		// Align right side of dialog with right side of cell
		int x = rect.x + rect.width - listSize.x;

		shell.setBounds(x, y, listSize.x, listSize.y);

		shell.open();
		list.setFocus();

		Display display = shell.getDisplay();
		while (!shell.isDisposed() && shell.isVisible()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		Proposal result = null;
		if (!shell.isDisposed()) {
			if (list.getSelectionIndex() != -1) {
				result = proposals.get(list.getSelectionIndex());
			}
			shell.dispose();
		}
		return result;
	}

	public void select(String string) {
		String[] items = list.getItems();

		// find the first entry in the list that starts with the
		// specified string
		if (string != null) {
			for (String item : items) {
				if (item.startsWith(string)) {
					int index = list.indexOf(item);
					list.select(index);
					break;
				}
			}
		}
	}

	public void setProposals(ArrayList<Proposal> proposals) {
		this.proposals = proposals;
		String[] proposalDescriptions = new String[proposals.size()];
		for (int i = 0; i < proposals.size(); i++) {
			proposalDescriptions[i] = proposals.get(i).getDescription();
		}
		list.setItems(proposalDescriptions);
	}

}
