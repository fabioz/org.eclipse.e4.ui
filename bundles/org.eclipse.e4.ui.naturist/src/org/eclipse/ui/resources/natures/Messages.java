/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc) - [102527] initial implementation
 ******************************************************************************/
package org.eclipse.ui.resources.natures;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	static {
		NLS.initializeMessages(Messages.class.getPackage().getName().replace('.', '/') + "/messages", Messages.class);
	}

	public static String ProjectNaturesPage_label;
	public static String ProjectNaturesPage_missingNatureText;
	public static String ProjectNaturesPage_addNature;
	public static String ProjectNaturesPage_removeNature;
	public static String ProjectNaturesPage_selectNatureToAddMessage;
	public static String ProjectNaturesPage_selectNatureToAddTitle;
	public static String ProjectNaturesPage_changeWarningTitle;
	public static String ProjectNaturesPage_warningMessage;
	public static String ProjectNaturesPage_changeWarningQuestion;

}
