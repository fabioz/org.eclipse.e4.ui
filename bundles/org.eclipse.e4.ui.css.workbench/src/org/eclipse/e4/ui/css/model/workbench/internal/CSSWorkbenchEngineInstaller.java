/*******************************************************************************
 * Copyright (c) 2014 Manumitting Technologies Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Brian de Alwis (MTI) - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.css.model.workbench.internal;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MAddon;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class CSSWorkbenchEngineInstaller {
    @Execute
	public void install(MApplication app) {
    	Class<?> clazz = CSSWorkbenchEngine.class; // CSSWorkbenchInstallationAddon.class
		String addonId = clazz.getName();
		for (MAddon addon : app.getAddons()) {
			if (addonId.equals(addon.getElementId())) {
				return;
			}
		}

		MAddon addon = MApplicationFactory.INSTANCE.createAddon();
		Bundle b = FrameworkUtil.getBundle(clazz);
		addon.setContributionURI("bundleclass://" + b.getSymbolicName() + "/"
				+ clazz.getName());
		addon.setElementId(addonId);
		app.getAddons().add(addon);
    }

}
