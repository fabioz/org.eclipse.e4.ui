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
package org.eclipse.ui.glance.internal.panels;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.IStatusFieldExtension;

import org.eclipse.ui.glance.panels.SearchPanel;
import org.eclipse.ui.glance.sources.Match;
import org.eclipse.ui.glance.utils.UIUtils;

/**
 * @author Yuri Strot
 * 
 */
@SuppressWarnings("restriction")
public class SearchStatusLine extends SearchPanel {

	@Override
	protected Control createText(Composite parent, int style) {
		Control textControl = super.createText(parent, style);
		textControl.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				setKeyFilter(true);
			}

			public void focusGained(FocusEvent e) {
				setKeyFilter(false);
			}
		});
		textControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				setKeyFilter(true);
			}
		});
		return textControl;
	}

	public static SearchStatusLine getSearchLine(IWorkbenchWindow window) {
		IStatusLineManager manager = getManager(window);
		if (manager != null) {
			IContributionItem[] items = manager.getItems();
			for (IContributionItem item : items) {
				if (item instanceof SearchItem)
					return ((SearchItem) item).getSearchPanel();
			}
		}
		return new SearchStatusLine(window);
	}

	public static IWorkbenchWindow getWindow(Control control) {
		Shell shell = control.getShell();
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
				.getWorkbenchWindows();
		for (IWorkbenchWindow window : windows) {
			if (shell.equals(window.getShell()))
				return window;
		}
		return null;
	}

	public boolean isApplicable(Control control) {
		return window.equals(getWindow(control));
	}

	public IWorkbenchWindow getWindow() {
		return window;
	}

	private int matchCount;

	@Override
	public void allFound(final Match[] matches) {
		super.allFound(matches);
		matchCount = matches.length;
		updateInfo();
	}

	private void updateInfo() {
		StringBuffer buffer = new StringBuffer();

		if (matchCount == 0) {
			buffer.append(DEFAULT_MATCH_LABEL);
		} else {
			buffer.append(matchCount);
		}

		matchText = buffer.toString();
		UIUtils.asyncExec(matchLabel, new Runnable() {

			public void run() {
				matchLabel.setText(matchText);
			}
		});
	}

	public void closePanel() {
		if (item != null) {
			fireClose();
			IStatusLineManager manager = getManager();
			if (manager != null) {
				manager.remove(item);
				manager.update(false);
			}
			item = null;
		}
	}

	@Override
	public void createContent(Composite parent) {
		super.createContent(parent);
		StatusLineLayoutData data = new StatusLineLayoutData();
		data.widthHint = getPreferedWidth();
		data.heightHint = getPreferredHeight();
		getControl().setLayoutData(data);
		createMatchLabel(parent);
	}

	@Override
	protected void textEmpty() {
		super.textEmpty();
		matchText = DEFAULT_MATCH_LABEL;
		matchLabel.setText(matchText);
	}

	private void createMatchLabel(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR);
		setLayoutData(separator);
		matchLabel = new CLabel(parent, SWT.SHADOW_NONE);
		StatusLineLayoutData data = new StatusLineLayoutData();
		data.widthHint = getTextWidth(parent, 10) + 15;
		data.heightHint = getPreferredHeight();
		matchLabel.setLayoutData(data);
		matchLabel.setText(matchText);
	}

	protected void setKeyFilter(boolean enabled) {
		IBindingService service = (IBindingService) PlatformUI.getWorkbench()
				.getService(IBindingService.class);
		if (service != null) {
			service.setKeyFilterEnabled(enabled);
		}
	}

	private class SearchItem extends ContributionItem implements IStatusField,
			IStatusFieldExtension {

		public void setImage(Image image) {
		}

		public void setText(String text) {
		}

		public void setErrorImage(Image image) {
			setImage(image);
		}

		public void setErrorText(String text) {
			setText(text);
		}

		public void setToolTipText(String string) {
			setText(string);
		}

		@Override
		public void fill(Composite parent) {
			Label separator = new Label(parent, SWT.SEPARATOR);
			createContent(parent);
			setLayoutData(separator);
		}

		public SearchStatusLine getSearchPanel() {
			return SearchStatusLine.this;
		}

		@Override
		public void dispose() {
			fireClose();
		}

	}

	private void setLayoutData(Label separator) {
		StatusLineLayoutData data = new StatusLineLayoutData();
		data.heightHint = getPreferredHeight();
		separator.setLayoutData(data);
	}

	private SearchStatusLine(IWorkbenchWindow window) {
		this.window = window;
		init();
	}

	private void init() {
		item = new SearchItem();
		IStatusLineManager manager = getManager();
		if (manager != null) {
			manager.remove(item);
			manager.appendToGroup(StatusLineManager.BEGIN_GROUP, item);
			manager.update(true);
		}
	}

	private IStatusLineManager getManager() {
		return getManager(window);
	}

	private static IStatusLineManager getManager(IWorkbenchWindow window) {
		if (window != null) {
			WorkbenchWindow ww = (WorkbenchWindow) window;
			return ww.getActionBars().getStatusLineManager();
		}
		return null;
	}

	private static final String DEFAULT_MATCH_LABEL = "no matches";

	private String matchText = DEFAULT_MATCH_LABEL;
	private CLabel matchLabel;
	private SearchItem item;
	private final IWorkbenchWindow window;

}
