/*******************************************************************************
 * Copyright (c) 2015 Manumitting Technologies Inc and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manumitting Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.internal.macros;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.e4.ui.macros.BaseProcessor;
import org.eclipse.e4.ui.macros.IMacroAction;
import org.eclipse.e4.ui.macros.IMacroActionProcessor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

/**
 * Provides a visualization on the macro recording process.
 */
public class MacroStatusPopup extends Window {
	private GestureInterpreter interpreter;
	private List<IMacroAction> actions;
	private TableViewer viewer;
	private Label status;

	/** Visualize an interpreter instance */
	public static void monitor(GestureInterpreter interpreter) {
		MacroStatusPopup popup = new MacroStatusPopup(interpreter);
		interpreter.addListener(popup.listener);
	}

	private IMacroActionProcessor listener = new BaseProcessor() {
		@Override
		public void started() {
			open();
		}

		@Override
		public void finished() {
			closeWindow();
		}

		@Override
		public void aborted() {
			closeWindow();
		}

		@Override
		public void process(IMacroAction action) {
			addAction(action);
		}
	};

	private MacroStatusPopup(GestureInterpreter interpreter) {
		super((Shell) null);
		setShellStyle(SWT.DIALOG_TRIM | SWT.ON_TOP | SWT.RESIZE);
		setBlockOnOpen(false);
		this.interpreter = interpreter;
	}

	@Override
	public int open() {
		// call create() to ensure our Shell is created -- but not visible --
		// to find the Display instance so that we can restore focus
		// back to the current shell
		create();
		Shell activeShell = getShell().getDisplay().getActiveShell();
		int rc = super.open();
		if (activeShell != null) {
			activeShell.forceActive();
		}
		return rc;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(200, 150);
	}

	@Override
	protected Layout getLayout() {
		return new FillLayout();
	}

	@Override
	protected Control createContents(final Composite container) {
		final Display display = container.getDisplay();

		interpreter.addIgnoreShell(container.getShell());

		Composite parent = new Composite(container, SWT.NONE);
		status = new Label(parent, SWT.NONE);
		viewer = new TableViewer(parent, SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().applyTo(status);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getControl());
		GridLayoutFactory.fillDefaults().applyTo(parent);

		// poor man's button
		status.setText("FINISH");
		status.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		status.setBackground(display.getSystemColor(SWT.COLOR_RED));
		status.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				finish();
			}
		});

		// stop recording if the window is disposed of
		container.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				finish();
			}
		});

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		actions = new LinkedList<>();
		viewer.setInput(actions);

		return parent;
	}

	protected void finish() {
		interpreter.finish();
	}

	private void closeWindow() {
		final Shell shell = getShell();
		if (shell == null || shell.isDisposed()) {
			return;
		}
		shell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				close();
			}
		});
	}


	protected void addAction(final IMacroAction action) {
		final Shell shell = getShell();
		if (shell == null || shell.isDisposed()) {
			return;
		}
		// should just use databinding
		actions.add(action);
		shell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (shell != null && !shell.isDisposed()) {
					viewer.refresh();
					// scroll to bottom
					int count = viewer.getTable().getItemCount();
					viewer.getTable().showItem(viewer.getTable().getItem(count - 1));
				}
			}
		});
	}

}
