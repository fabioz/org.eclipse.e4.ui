/*******************************************************************************
 * Copyright (c) 2015 Manumitting Technologies Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manumitting Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.internal.macros;

import java.util.Comparator;
import java.util.TreeSet;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MBindingContext;
import org.eclipse.e4.ui.model.application.commands.MBindingTable;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MHandler;
import org.eclipse.e4.ui.model.application.commands.MKeyBinding;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.framework.FrameworkUtil;

/**
 * E4 Model Processor to set up macro recording and playback commands
 */
public class MacroRecorderInstaller {
	private static final String TOGGLE_RECORDING_COMMAND = "org.eclipse.e4.ui.macros.toggleRecording";

	private static final String PLAY_MACRO_COMMAND = "org.eclipse.e4.ui.macros.playRecording";

	@Inject
	private MApplication application;

	@Inject
	private EModelService modelService;

	private String bundleId = FrameworkUtil.getBundle(getClass()).getSymbolicName();


	@Execute
	public void process() {
		MCommand recordMacro = installCommand(TOGGLE_RECORDING_COMMAND, "Start/Stop recording a macro",
				"Start recording a macro or stop the current macro");
		installHandler(recordMacro, ToggleRecordingHandler.class);
		installKeyBinding("org.eclipse.ui.contexts.window", "M2+M3+M", recordMacro);

		MCommand replay = installCommand(PLAY_MACRO_COMMAND, "Play macro", "Replay the last recorded macro");
		installHandler(replay, ReplayMacroHandler.class);
		installKeyBinding("org.eclipse.ui.contexts.window", "M1+M2+M", replay);
	}

	private MCommand installCommand(String commandId, String name, String description) {
		for (MCommand cmd : application.getCommands()) {
			if (commandId.equals(cmd.getElementId())) {
				// Already exists
				return cmd;
			}
		}

		MCommand command = modelService.createModelElement(MCommand.class);
		command.setElementId(commandId);
		command.setCommandName(name);
		command.setDescription(description);
		command.setContributorURI("platform:/plugin/" + bundleId);

		application.getCommands().add(command);
		return command;
	}

	private MHandler installHandler(MCommand command, Class<?> handlerClass) {
		String handlerBundleId = FrameworkUtil.getBundle(handlerClass).getSymbolicName();
		String contributionURI = "bundleclass://" + handlerBundleId + "/" + handlerClass.getName();

		for (MHandler handler : application.getHandlers()) {
			if (contributionURI.equals(handler.getContributionURI())) {
				// Already exists
				return handler;
			}
		}

		MHandler handler = modelService.createModelElement(MHandler.class);
		handler.setElementId(handlerClass.getName());
		handler.setCommand(command);
		handler.setContributionURI(contributionURI);
		handler.setContributorURI("platform:/plugin/" + handlerBundleId);
		application.getHandlers().add(handler);
		return handler;
	}

	private MKeyBinding installKeyBinding(String bindingContextId, String keySeq, MCommand command) {
		MBindingContext bindingContext = findBindingContext(bindingContextId);
		if (bindingContext == null) {
			bindingContext = modelService.createModelElement(MBindingContext.class);
			bindingContext.setElementId(bindingContextId);
			application.getBindingContexts().add(bindingContext);
		}

		MBindingTable bindingTable = null;
		for (MBindingTable bt : application.getBindingTables()) {
			if (bt.getBindingContext() == bindingContext) {
				bindingTable = bt;
				break;
			}
		}
		if (bindingTable == null) {
			bindingTable = modelService.createModelElement(MBindingTable.class);
			bindingTable.setBindingContext(bindingContext);
			application.getBindingTables().add(bindingTable);
		}
		for (MKeyBinding kb : bindingTable.getBindings()) {
			if (kb.getCommand() == command) {
				// already bound
				return kb;
			}
		}

		MKeyBinding keyBinding = modelService.createModelElement(MKeyBinding.class);
		keyBinding.setKeySequence(keySeq);
		keyBinding.setContributorURI("platform:/plugin/" + bundleId);
		keyBinding.setCommand(command);
		bindingTable.getBindings().add(keyBinding);
		return keyBinding;
	}

	private MBindingContext findBindingContext(String bindingContextId) {
		TreeSet<MBindingContext> contexts = new TreeSet<>(new Comparator<MBindingContext>() {
			@Override
			public int compare(MBindingContext o1, MBindingContext o2) {
				return o1.getElementId().compareTo(o2.getElementId());
			}
		});
		contexts.addAll(application.getBindingContexts());
		while (!contexts.isEmpty()) {
			MBindingContext bc = contexts.first();
			if (bindingContextId.equals(bc.getElementId())) {
				return bc;
			}
			contexts.remove(bc);
			contexts.addAll(bc.getChildren());
		}
		return null;
	}

}
