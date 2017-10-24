/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - http://eclip.se/8519
 *******************************************************************************/
package org.eclipse.e4.core.macros;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

/**
 * Plugin activator.
 */
public class Activator extends Plugin {

	private static Activator plugin;

	public Activator() {
		super();
		plugin = this;
	}

	public static Activator getDefault() {
		return plugin;
	}

	public static void log(Throwable exception) {
		try {
			if (plugin != null) {
				plugin.getLog().log(new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(),
						exception.getMessage(), exception));
			} else {
				// The plugin is not available. Just print to stderr.
				exception.printStackTrace();
			}
		} catch (Exception e) {
			// Print the original error if something happened, not the one
			// related to the log not working.
			exception.printStackTrace();
		}
	}
}
