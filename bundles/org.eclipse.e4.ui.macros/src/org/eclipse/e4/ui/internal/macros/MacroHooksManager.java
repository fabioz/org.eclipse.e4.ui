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

import javax.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.macros.IMacroHook;
import org.eclipse.e4.ui.model.application.MApplication;

public class MacroHooksManager {

	static final String HOOKS_EXTPT = "org.eclipse.e4.ui.macros.hooks";
	static final String NAME_HOOK = "hook";
	static final String ATTR_CLASS = "class";

	@Inject
	private MApplication application;

	@Inject
	private IExtensionRegistry registry;

	private List<IMacroHook> hooks;

	public IStatus runStartHooks(IMacroHook.Mode mode) {
		if (hooks != null) {
			System.out.println(">> Hooks exist: already in progress?");
			return new Status(IStatus.ERROR, "org.eclipse.e4.ui.macros", "Macro operation already in progress");
		}
		// System.out.println(">> Running starting hooks (" + mode + ")...");
		hooks = new LinkedList<>();
		for (IConfigurationElement ce : registry.getConfigurationElementsFor(HOOKS_EXTPT)) {
			if (ce.getName().equals(NAME_HOOK) && ce.getAttribute(ATTR_CLASS) != null) {
				try {
					IMacroHook hook = (IMacroHook) ce.createExecutableExtension(ATTR_CLASS);
					IStatus rc = hook.start(mode);
					if (!rc.isOK()) {
						return rc;
					}
					hooks.add(hook);
				} catch (CoreException e) {
					System.err.println("Unable to instantiate class: " + e);
				}
			}
		}
		return Status.OK_STATUS;
	}

	public void runFinishHooks(IMacroHook.Mode mode) {
		// may not have been running
		if (hooks != null) {
			// System.out.println(">> Running finished hooks (" + mode +
			// ")...");
			for (IMacroHook hook : hooks) {
				hook.stop(mode);
			}
			hooks = null;
		}
	}

}
