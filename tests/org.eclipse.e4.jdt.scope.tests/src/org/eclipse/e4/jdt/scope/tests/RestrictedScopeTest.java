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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.jdt.scope.ScopeValidator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RestrictedScopeTest {
	
	@Parameters(name = "{0}") //$NON-NLS-1$
	public static Collection<String> data() {
		return Arrays.asList(new String[] { "KOImport.java", "KOMember.java",  "KOParameter.java", "KOReturnType.java", "KOWildcardImport.java"} );
	}
	
	@Parameter
	public String fileToCheck;

	@Test
	@Ignore("See Bug 518990")
	public void test() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(AllScopeTests.TEST_PROJECT_NAME);
		IFile file = project.getFolder("restrict/testScope").getFile(this.fileToCheck); //$NON-NLS-1$
		IMarker[] markers = file.findMarkers(ScopeValidator.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		Assert.assertNotEquals("At least 1 marker should be found on " + file.getName(), 0, markers.length); //$NON-NLS-1$
	}

}
