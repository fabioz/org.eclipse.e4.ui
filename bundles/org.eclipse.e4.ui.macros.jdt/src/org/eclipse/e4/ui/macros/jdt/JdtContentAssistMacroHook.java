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
package org.eclipse.e4.ui.macros.jdt;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.macros.IMacroHook;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Disable content assist for macro recording and playback as it introduces
 * non-determinism: we can't be certain of the ordering of the items shown is
 * stable.
 */
public class JdtContentAssistMacroHook implements IMacroHook {
	private boolean autoActivates;

	@Override
	public IStatus start(Mode mode) {
		IPreferenceStore preferenceStore = JavaPlugin.getDefault().getPreferenceStore();
		autoActivates = preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION);
		preferenceStore.setValue(PreferenceConstants.CODEASSIST_AUTOACTIVATION, false);
		return Status.OK_STATUS;
	}

	@Override
	public void stop(Mode mode) {
		IPreferenceStore preferenceStore = JavaPlugin.getDefault().getPreferenceStore();
		preferenceStore.setValue(PreferenceConstants.CODEASSIST_AUTOACTIVATION, autoActivates);
	}
}
