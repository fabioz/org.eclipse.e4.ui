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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.jdt.scope.ScopeValidator;
import org.junit.Assert;
import org.junit.Test;

public class VisibleScopeForFolderTest {
	
	@Test
	public void testPlainFolder() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(AllScopeTests.TEST_PROJECT_NAME);
		IFile file = project.getFolder("seeAll/testScope").getFile("OK.java"); //$NON-NLS-1$
		IMarker[] markers = file.findMarkers(ScopeValidator.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		Assert.assertEquals("No scope marker should be found on " + file.getName(), 0, markers.length); //$NON-NLS-1$
	}
	

	@Test
	public void testTaggedFolder() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(AllScopeTests.TEST_PROJECT_NAME);
		IFile file = project.getFolder("seeAllTagged/testScope").getFile("OK2.java"); //$NON-NLS-1$
		IMarker[] markers = file.findMarkers(ScopeValidator.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		Assert.assertEquals("No scope marker should be found on " + file.getName(), 0, markers.length); //$NON-NLS-1$
	}


}
