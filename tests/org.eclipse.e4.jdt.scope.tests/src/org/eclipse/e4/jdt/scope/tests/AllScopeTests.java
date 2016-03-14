/*******************************************************************************
 * Copyright (C) 2016, Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.e4.jdt.scope.tests;

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.jdt.scope.ScopeValidator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ RestrictedScopeTest.class, VisibleScopeForFolderTest.class })
public class AllScopeTests {
	
	public static final String TEST_PROJECT_NAME = "testScope"; //$NON-NLS-1$
	
	@BeforeClass
	public static void provisionAndBuildTestProject() throws Exception {
		URL url = FileLocator.toFileURL(AllScopeTests.class.getClassLoader().getResource("/" + TEST_PROJECT_NAME));
		IProjectDescription description = ResourcesPlugin.getWorkspace().loadProjectDescription(new Path(url.getFile() + File.separator + IProjectDescription.DESCRIPTION_FILE_NAME));
		description.setLocation(new Path(url.getFile()));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
		project.create(description, null);
		project.open(null);
		//
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean builderAlreadySetup = false;
		for (int i = 0; i < commands.length; ++i) {
			if (!builderAlreadySetup && commands[i].getBuilderName().equals(ScopeValidator.BUILDER_ID)) {
				builderAlreadySetup = true;
			}
		}
		if (!builderAlreadySetup) {
			ICommand command = desc.newCommand();
			command.setBuilderName(ScopeValidator.BUILDER_ID);
			ICommand[] nc = new ICommand[commands.length + 1];
			// Add it before other builders.
			System.arraycopy(commands, 0, nc, 0, commands.length);
			nc[nc.length - 1] = command;
			desc.setBuildSpec(nc);
			project.setDescription(desc, null);
		}
		
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
	}
	
	@AfterClass
	public static void removeProject() throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject(TEST_PROJECT_NAME).delete(false, false, new NullProgressMonitor());
	}

}
