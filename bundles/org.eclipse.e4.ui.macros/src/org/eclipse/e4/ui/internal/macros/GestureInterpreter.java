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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;
import org.eclipse.e4.ui.internal.macros.actions.InsertKeyStroke;
import org.eclipse.e4.ui.internal.macros.actions.InvokeCommand;
import org.eclipse.e4.ui.internal.macros.actions.WaitForShellActivation;
import org.eclipse.e4.ui.internal.macros.actions.WaitForWidgetFocus;
import org.eclipse.e4.ui.macros.IMacroAction;
import org.eclipse.e4.ui.macros.IMacroActionProcessor;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.renderers.swt.HandledContributionItem;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.internal.ActionSetContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;

/**
 * Responsible for translating UI events into macro actions and sending them
 * along to interested parties implementing {@link IMacroActionProcessor} (e.g.,
 * to record the actions for future playback, or provide a UI/visualization).
 * 
 * The idea was this interpreter would act as an orchestrator of multiple
 * sources that produce {@link IMacroAction}s. But there are sufficiently
 * intertwined that we've just kept them together for now.
 */
public class GestureInterpreter {
	private static Class<?> commandContributionItemClass;

	static {
		try {
			commandContributionItemClass = Class.forName("org.eclipse.ui.menus.CommandContributionItem");
		} catch (Exception e) {
			/* ignore */
		}
	}

	@Inject
	private MApplication application;

	@Inject
	private IEclipseContext context;

	@Inject
	private Display display;

	@Inject
	private CommandManager commandManager;

	@Inject
	private KeyBindingDispatcher dispatcher;

	@Inject
	EHandlerService handlerService;

	@Inject
	IExtensionRegistry registry;

	/** Notified of macro actions */
	private List<IMacroActionProcessor> processors = Collections
			.synchronizedList(new LinkedList<IMacroActionProcessor>());

	/**
	 * Controls whether events should be turned into macro actions; used to
	 * ignore events when popping up macro-related dialogs
	 */
	private boolean listeningPaused = false;

	/******* Command white-listing and black-listing *******/
	private static final String MACRO_COMMANDS_EXTPT = "org.eclipse.e4.ui.macros.commands";
	private static final String NAME_BLACKLIST = "blacklist";
	private static final String NAME_WHITELIST = "whitelist";
	private static final String ATTR_ID = "id";

	private Set<String> whitelistedCommandIds;
	private Set<String> blacklistedCommandIds;



	/** SWT.KeyDown listener to turn key presses into {@link IMacroAction}s */
	private Listener keyDownListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			dispatcher.getKeyDownFilter().handleEvent(event);
			if (listeningPaused) {
				return;
			}
			if (event.widget instanceof Control && shouldIgnore(event)) {
				return;
			}
			// if not swallowed by the key dispatcher
			if (event.doit && (event.keyCode & SWT.KEY_MASK) != SWT.NONE) {
				// List<KeyStroke> strokes =
				// KeyBindingDispatcher.generatePossibleKeyStrokes(event);
				// System.out.println(">> keyCode: " + event.keyCode + "
				// Strokes: " + strokes);
				inject(new InsertKeyStroke(event, event.widget));
			}
		}
	};

	/**
	 * SWT.Selection listener for menu and toolbar selections to pull out
	 * commands
	 */
	private Listener menuToolbarSelectionListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (listeningPaused) {
				return;
			}
			// we're only interested in MenuItem and ToolItems
			if (event.widget instanceof Control || shouldIgnore(event)) {
				return;
			}

			Object object = event.widget.getData();
			if (!processPotentialCommand(object)) {
				abort();
				MessageDialog.openError(null, "Unknown Action", "Command cannot be determined from this item");
			}
		}
	};

	/**
	 * SWT.Activate listener for shells
	 */
	private Listener shellActivatedListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (listeningPaused) {
				return;
			}
			// should we track shell-activation because of a click in that
			// window vs a command that opens a new Shell?
			if (event.widget instanceof Shell && !shouldIgnore(event)) {
				inject(new WaitForShellActivation(event.widget));
			}
		}
	};

	/**
	 * SWT.FocusIn listener
	 */
	private Listener focusInListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (listeningPaused) {
				return;
			}
			if (event.widget instanceof Control && !shouldIgnore(event)) {
				inject(new WaitForWidgetFocus(event.widget));
			}
		}
	};

	/** Turn successful command executions into actions */
	private IExecutionListener commandExecutionListener = new IExecutionListener() {
		private Map<String, ParameterizedCommand> inProgress = new HashMap<>();

		@Override
		public void preExecute(String commandId, ExecutionEvent event) {
			// System.out.println("preExecute: " + commandId);
			if (listeningPaused) {
				return;
			}
			try {
				if (!checkWhitelistedCommand(event.getCommand())) {
					return;
				}
				ParameterizedCommand pc = ParameterizedCommand.generateCommand(event.getCommand(),
						event.getParameters());
				inProgress.put(commandId, pc);
			} catch (NotDefinedException e) {
				// ignore
			}
		}

		@Override
		public void postExecuteSuccess(String commandId, Object returnValue) {
			// System.out.println("postExecuteSuccess: " + commandId);
			ParameterizedCommand pc = inProgress.remove(commandId);
			if (pc != null) {
				inject(new InvokeCommand(handlerService, pc));
			}
		}

		@Override
		public void notHandled(String commandId, NotHandledException exception) {
			// ignore these commands
			inProgress.remove(commandId);
		}

		@Override
		public void postExecuteFailure(String commandId, ExecutionException exception) {
			// ignore these commands
			inProgress.remove(commandId);
		}
	};

	/**
	 * Mouse events are difficult to turn into keyboard macro actions, except
	 * for menu and tool items.
	 */
	private Listener mouseUpListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (listeningPaused) {
				return;
			}
			// Ignore SWT.MouseUp events on Menu and ToolBar as they're
			// turned into SWT.Selection events on the corresponding item
			// and handled in our SWT.Selection listener
			if (!(event.widget instanceof Menu) && !(event.widget instanceof ToolBar)) {
				// use asyncExec as any mouse and key events may be dispatched
				// to the MessageDialog instead
				event.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						abort();
						MessageDialog.openError(null, "Macro Recording Aborted",
								"Mouse click cannot be incorporated into macro");
					}
				});
			}
		}
	};

	/** List of {@link Shell}s that should be ignored */
	private Set<Shell> ignoredShells = new HashSet<Shell>();

	public void start() {
		loadCommandWhiteAndBlacklists();
		if (context != null) {
			context.set(GestureInterpreter.class, this);
		}

		hookListeners();
		synchronized (processors) {
			for (IMacroActionProcessor recorder : processors) {
				recorder.started();
			}
		}
	}

	public void finish() {
		unhookListeners();

		synchronized (processors) {
			for (IMacroActionProcessor recorder : processors) {
				recorder.finished();
			}
		}
		if (context != null && context.get(GestureInterpreter.class) == this) {
			context.remove(GestureInterpreter.class);
		}
	}

	public void abort() {
		unhookListeners();

		synchronized (processors) {
			for (IMacroActionProcessor recorder : processors) {
				recorder.aborted();
			}
		}
		if (context != null && context.get(GestureInterpreter.class) == this) {
			context.remove(GestureInterpreter.class);
		}
	}

	private void inject(IMacroAction action) {
		synchronized (processors) {
			for (IMacroActionProcessor recorder : processors) {
				recorder.process(action);
			}
		}
	}

	protected boolean processPotentialCommand(Object object) {
		if(object instanceof ParameterizedCommand) {
			inject(new InvokeCommand(handlerService, (ParameterizedCommand) object));
			return true;
		} else if (object instanceof IContributionItem) {
			IContributionItem item = (IContributionItem) object;
			String id = item.getId();
			if (commandContributionItemClass != null && commandContributionItemClass.isInstance(item)) {
				CommandContributionItem cci = (CommandContributionItem) item;
				return processPotentialCommand(cci.getCommand());
			} else if (item instanceof HandledContributionItem) {
				HandledContributionItem hci = (HandledContributionItem) item;
				return processPotentialCommand(hci.getModel().getWbCommand());
			} else if (item instanceof ActionContributionItem) {
				return processPotentialCommand(((ActionContributionItem) item).getAction());
			} else if (item instanceof ActionSetContributionItem) {
				return processPotentialCommand(((ActionSetContributionItem) item).getInnerItem());
			} else if (item instanceof ActionContributionItem) {
				return processPotentialCommand(((ActionContributionItem) item).getAction());
			}
			return false;
		} else if (object instanceof IAction) {
			IAction action = (IAction) object;
			if (action.getActionDefinitionId() != null) {
				Command cmd = commandManager.getCommand(action.getActionDefinitionId());
				if (cmd.isDefined()) {
					return processPotentialCommand(new ParameterizedCommand(cmd, new Parameterization[0]));
				}
				System.err.println("Unable to find referenced command: " + action.getActionDefinitionId());
			} else {
				System.err.println("We do not currently support selecting actions");
			}
			return false;
		}
		return false;
	}

	protected boolean shouldIgnore(Event event) {
		Shell shell = getShell(event.widget);
		while (shell != null) {
			if (ignoredShells.contains(shell)) {
				return true;
			}
			shell = (Shell) shell.getParent();
		}
		return false;
	}

	private static Shell getShell(Widget widget) {
		if (widget instanceof MenuItem) {
			return ((MenuItem) widget).getParent().getShell();
		} else if (widget instanceof ToolItem) {
			return ((ToolItem) widget).getParent().getShell();
		}
		return ((Control) widget).getShell();
	}

	private void loadCommandWhiteAndBlacklists() {
		whitelistedCommandIds = new HashSet<>();
		blacklistedCommandIds = new HashSet<>();
		String csv = application.getPersistedState().get(MACRO_COMMANDS_EXTPT + ":" + NAME_WHITELIST);
		if (csv != null) {
			Collections.addAll(whitelistedCommandIds, csv.split(","));
		}
		csv = application.getPersistedState().get(MACRO_COMMANDS_EXTPT + ":" + NAME_BLACKLIST);
		if (csv != null) {
			Collections.addAll(blacklistedCommandIds, csv.split(","));
		}
		for (IConfigurationElement ce : registry.getConfigurationElementsFor(MACRO_COMMANDS_EXTPT)) {
			if (NAME_BLACKLIST.equals(ce.getName()) && ce.getAttribute(ATTR_ID) != null) {
				blacklistedCommandIds.add(ce.getAttribute(ATTR_ID));
			} else if (NAME_WHITELIST.equals(ce.getName()) && ce.getAttribute(ATTR_ID) != null) {
				whitelistedCommandIds.add(ce.getAttribute(ATTR_ID));
			}
		}
	}

	protected boolean checkWhitelistedCommand(Command command) throws NotDefinedException {
		String commandId = command.getId();
		String commandName = command.getName();
		if (blacklistedCommandIds.contains(commandId)) {
			abort(); // unhooks listeners
			MessageDialog.openError(null, "Disallowed Command",
					NLS.bind("Command \"{0}\" ({1}) cannot be used during macro recording", commandName, commandId));
			return false;
		} else if (!whitelistedCommandIds.contains(commandId)) {
			pauseListeners();
			boolean shouldWhitelist = MessageDialog.openQuestion(null, "Whitelist command?",
					NLS.bind("Should \"{0}\" ({1}) be allowed for macros?", commandName, commandId));
			resumeListeners();
			if (shouldWhitelist) {
				System.err.println("FIXME: <whitelist id=\"" + commandId + "\" />");
				whitelistedCommandIds.add(commandId);
				String csv = application.getPersistedState().get(MACRO_COMMANDS_EXTPT + ":" + NAME_WHITELIST);
				application.getPersistedState().put(MACRO_COMMANDS_EXTPT + ":" + NAME_WHITELIST,
						csv == null ? commandId : csv + "," + commandId);
			} else {
				// FIXME: record the user's direction
				System.err.println("FIXME: <blacklist id=\"" + commandId + "\" />");
				// FIXME: record this more permanently
				blacklistedCommandIds.add(commandId);
				String csv = application.getPersistedState().get(MACRO_COMMANDS_EXTPT + ":" + NAME_BLACKLIST);
				application.getPersistedState().put(MACRO_COMMANDS_EXTPT + ":" + NAME_BLACKLIST,
						csv == null ? commandId : csv + "," + commandId);
				abort();
			}
			return shouldWhitelist;
		}
		return true;
	}

	private void resumeListeners() {
		listeningPaused = false;
	}

	private void pauseListeners() {
		listeningPaused = true;
	}

	private void hookListeners() {
		Listener keyFilter = dispatcher.getKeyDownFilter();
		display.removeFilter(SWT.KeyDown, keyFilter);
		display.removeFilter(SWT.Traverse, keyFilter);
		display.addFilter(SWT.Traverse, keyDownListener);
		display.addFilter(SWT.KeyDown, keyDownListener);

		display.addFilter(SWT.MouseUp, mouseUpListener);
		display.addFilter(SWT.Selection, menuToolbarSelectionListener);
		display.addFilter(SWT.Activate, shellActivatedListener);
		display.addFilter(SWT.FocusIn, focusInListener);
		commandManager.addExecutionListener(commandExecutionListener);
	}

	private void unhookListeners() {
		Listener keyFilter = dispatcher.getKeyDownFilter();
		display.addFilter(SWT.KeyDown, keyFilter);
		display.addFilter(SWT.Traverse, keyFilter);
		display.removeFilter(SWT.Traverse, keyDownListener);
		display.removeFilter(SWT.KeyDown, keyDownListener);

		display.removeFilter(SWT.MouseUp, mouseUpListener);
		display.removeFilter(SWT.Selection, menuToolbarSelectionListener);
		display.removeFilter(SWT.Activate, shellActivatedListener);
		display.removeFilter(SWT.FocusIn, focusInListener);
		commandManager.removeExecutionListener(commandExecutionListener);
	}

	public void addListener(IMacroActionProcessor recorder) {
		processors.add(recorder);
	}

	public void removeListener(IMacroActionProcessor recorder) {
		processors.remove(recorder);
	}

	/** Ignore events coming from the provided shell */
	public void addIgnoreShell(Shell shell) {
		ignoredShells.add(shell);
	}

}
