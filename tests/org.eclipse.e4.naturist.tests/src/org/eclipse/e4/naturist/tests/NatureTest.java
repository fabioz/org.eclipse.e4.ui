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
package org.eclipse.e4.naturist.tests;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.e4.ui.naturist.CheckMissingNaturesListener;
import org.junit.Assert;
import org.junit.Test;

public class NatureTest extends ResourceTest {

	@Test
	public void testMissingNatureAddsMarker() throws Exception {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject project = ws.getRoot().getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(new String[] {NATURE_MISSING});
		project.setDescription(desc, IResource.FORCE | IResource.AVOID_NATURE_CONFIG, getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		IMarker[] markers = project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ZERO);
		Assert.assertEquals(1, markers.length);
		IMarker marker = markers[0];
		Assert.assertEquals(NATURE_MISSING, marker.getAttribute("natureId"));
	}

	@Test
	public void testKnownNatureDoesntAddMarker() throws Exception {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject project = ws.getRoot().getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(new String[] {NATURE_SIMPLE});
		project.setDescription(desc, getMonitor());
		project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		Job.getJobManager().join(CheckMissingNaturesListener.MARKER_TYPE, getMonitor());
		Assert.assertEquals(0, project.findMarkers(CheckMissingNaturesListener.MARKER_TYPE, false, IResource.DEPTH_ZERO).length);
	}
}
