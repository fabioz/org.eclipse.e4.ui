/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.naturist;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

public class AttachListenerToWorkspace implements IStartup {

	@Override
	public void earlyStartup() {
		CheckMissingNaturesListener checkMissingNaturesListener = new CheckMissingNaturesListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(checkMissingNaturesListener,
				IResourceChangeEvent.POST_CHANGE);
	}

}
