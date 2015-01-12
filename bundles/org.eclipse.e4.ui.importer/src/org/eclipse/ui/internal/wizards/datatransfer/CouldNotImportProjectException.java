/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat Inc., and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.internal.wizards.datatransfer;

import java.io.File;

public class CouldNotImportProjectException extends Exception {

	private File location;

	public CouldNotImportProjectException(File location, Exception cause) {
		super("Could not import project located at " + location.getAbsolutePath(), cause);
		this.location = location;
	}
}
