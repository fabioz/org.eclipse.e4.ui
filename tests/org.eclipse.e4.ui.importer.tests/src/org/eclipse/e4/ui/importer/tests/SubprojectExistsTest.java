/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc., and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.ui.importer.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;

import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Expand;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.SWTBotTestCase;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.wizards.datatransfer.EasymportWizard;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4.class)
public class SubprojectExistsTest extends SWTBotTestCase {
	
	private static File projectFolder;

	@BeforeClass
	public static void prepareTestProject() throws Exception {
		URI uri = new URI("https://github.com/eclipse/thym/archive/1.0.0.zip");
//		BundleContext bundleContext = Activator.getDefault().getBundle().getBundleContext();
//		IProxyService service = bundleContext.getService(bundleContext.getServiceReference(IProxyService.class));)
//		IProxyData[] proxyData = service.select(uri);	
//		for (IProxyData proxyData : select) {
//		}
		
		File zipProjectFile = File.createTempFile("thym", ".zip");
		InputStream input = uri.toURL().openStream();
		FileOutputStream outStream = new FileOutputStream(zipProjectFile);
		byte[] bytes = new byte[1024]; int size = 0;
		while ( (size = input.read(bytes)) > 0) {
			outStream.write(bytes, 0, size);
		}
		outStream.close();
		input.close();
		zipProjectFile.deleteOnExit();
		projectFolder = Files.createTempDirectory("thym").toFile();
		Expand unzipTask = new Expand();
		unzipTask.setSrc(zipProjectFile);
		unzipTask.setDest(projectFolder);
		unzipTask.execute();
		projectFolder = new File(projectFolder, "thym-1.0.0");
	}
	
	@AfterClass
	public static void cleanFolder() {
		if (projectFolder != null && projectFolder.isDirectory()) {
			Delete deleteTask = new Delete();
			deleteTask.setDir(projectFolder);
			deleteTask.execute();
		}
	}
	
	@Before
	public void importAChildProject() throws Exception {
		String projectPath = "tests/org.eclipse.thym.test";
		String[] pathSegments = projectPath.split("/");
		String projectName = pathSegments[pathSegments.length - 1];
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		File moduleFolder = new File(projectFolder, projectPath);
		InputStream dotProjectStream = new FileInputStream(new File(moduleFolder, IProjectDescription.DESCRIPTION_FILE_NAME));
		IProjectDescription description = project.getWorkspace().loadProjectDescription(dotProjectStream);
		description.setLocationURI(moduleFolder.toURI());
		dotProjectStream.close();
		project.create(description, new NullProgressMonitor());
	}
	
	@After
	public void cleanWorkspace() throws CoreException {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) { 
			project.delete(true, new NullProgressMonitor());	
		}
	}
	
	@Test
	public void testAlreadyImportedProject() throws Exception {
		Display.getDefault().syncExec( () -> {
			EasymportWizard wizard = new EasymportWizard();
			wizard.setInitialDirectory(projectFolder);
			WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
			dialog.setBlockOnOpen(false);
			dialog.open();
		});
		bot.button("Next >").click();
		bot.waitUntil(new DefaultCondition() {
			@Override
			public boolean test() throws Exception {
				return bot.tree().getAllItems().length > 0;
			}
			
			@Override
			public String getFailureMessage() {
				return "Couldn't get populated tree";
			}
		});
		SWTBotTreeItem notCheckedItem = null;
		int checkedCount = 0;
		int notCheckedCount = 0;
		for (SWTBotTreeItem item : bot.tree().getAllItems()) {
			if (item.isChecked()) {
				checkedCount++;
			} else {
				notCheckedCount++;
				notCheckedItem = item;
			}
		}
		assertEquals("One item shouldn't be checked (already imported", 1, notCheckedCount);
		assertTrue("There should be some more item pre-checked", checkedCount > 1);
		
		notCheckedItem.check();
		assertFalse("Item cannot be checked", notCheckedItem.isChecked());
		notCheckedItem.toggleCheck();
		assertFalse("Item cannot be checked", notCheckedItem.isChecked());

		bot.button("Deselect All").click();
		checkedCount = 0;
		notCheckedCount = 0;
		for (SWTBotTreeItem item : bot.tree().getAllItems()) {
			if (item.isChecked()) {
				checkedCount++;
			} else {
				notCheckedCount++;
				notCheckedItem = item;
			}
		}
		assertEquals("No item should be checked", 0, checkedCount);
		assertTrue("More items should be unchecked", notCheckedCount > 1);
		assertFalse("Finish button should be disabled when nothing is checked", bot.button("Finish").isEnabled());
		
		SWTBotTreeItem anItem = bot.tree().getAllItems()[2]; // pick item #2 "randomly"
		anItem.check();
		assertTrue("Finish button should be enabled when 1 item is checked", bot.button("Finish").isEnabled());
		
		bot.button("Select All").click();
		checkedCount = 0;
		notCheckedCount = 0;
		for (SWTBotTreeItem item : bot.tree().getAllItems()) {
			if (item.isChecked()) {
				checkedCount++;
			} else {
				notCheckedCount++;
				notCheckedItem = item;
			}
		}
		assertEquals("One item shouldn't be checked (already imported", 1, notCheckedCount);
		assertTrue("There should be some more item pre-checked", checkedCount > 1);
		
			// TODO: select only one, Finish, only 1 project imported
	}
	
}
