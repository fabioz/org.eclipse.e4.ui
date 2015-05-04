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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	static {
		NLS.initializeMessages(Messages.class.getPackage().getName().replace('.', '/') + "/Messages", Messages.class);
	}

	public static String selectFolderToImport;
	public static String alreadyImportedAsProject_title;
	public static String alreadyImportedAsProject_description;
	public static String anotherProjectWithSameNameExists_title;
	public static String anotherProjectWithSameNameExists_description;
	public static String importProject;

	public static String eclipseProjectConfigurationLabel;

	public static String EasymportWizardPage_importProjectsInFolderTitle;
	public static String EasymportWizardPage_importProjectsInFolderDescription;
	public static String EasymportWizardPage_selectRootDirectory;
	public static String EasymportWizardPage_incorrectRootDirectory;
	public static String EasymportWizardPage_browse;
	public static String EasymportWizardPage_workingSets;
	public static String EasymportWizardPage_nestedProjects;
	public static String EasymportWizardPage_detectNestedProjects;
	public static String EasymportWizardPage_importRawProject;
	public static String EasymportWizardPage_availableDetectors;
	public static String EasymportWizardPage_importedProjects;
	public static String EasymportWizardPage_relativePath;
	public static String EasymportWizardPage_project;
	public static String EasymportWizardPage_natures;


}
