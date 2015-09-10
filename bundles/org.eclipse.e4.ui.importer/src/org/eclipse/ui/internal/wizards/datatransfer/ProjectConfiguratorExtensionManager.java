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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.expressions.ElementHandler;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.internal.wizards.datatransfer.expressions.FileExpressionHandler;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.osgi.framework.Bundle;

public class ProjectConfiguratorExtensionManager {

	private static final String EXTENSION_POINT_ID = Activator.PLUGIN_ID + ".projectConfigurators"; //$NON-NLS-1$

	private IConfigurationElement[] extensions;
	private ExpressionConverter expressionConverter;
	private Map<IConfigurationElement, ProjectConfigurator> configuratorsByExtension = new HashMap<IConfigurationElement, ProjectConfigurator>();

	/**
	 * Each instance of this class will have it's own internal registry, that will load (maximum) once each extension class,
	 * depending on whether the extension has been active for one case handled by this Manager.
	 */
	public ProjectConfiguratorExtensionManager() {
		this.extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
		// Force Eclipse configurator to be 1st
		int eclipseConfiguratorIndex = 0;
		while (eclipseConfiguratorIndex < this.extensions.length && !this.extensions[eclipseConfiguratorIndex].getAttribute("class").equals(EclipseProjectConfigurator.class.getName())) {
			eclipseConfiguratorIndex++;
		}
		if (eclipseConfiguratorIndex != 0 && eclipseConfiguratorIndex < this.extensions.length) {
			// swap
			IConfigurationElement tmp = this.extensions[eclipseConfiguratorIndex];
			this.extensions[eclipseConfiguratorIndex] = this.extensions[0];
			this.extensions[0] = tmp;
		}
		this.expressionConverter = new ExpressionConverter(new ElementHandler[] {
			ElementHandler.getDefault(),
			new FileExpressionHandler()
		});
	}

	/**
	 * 
	 * @param container
	 * @return The active connectors for given container, order is important: top-priority are 1st
	 */
	private List<ProjectConfigurator> getAllActiveProjectConfiguratorsUntyped(Object container) {
		List<ProjectConfigurator> res = new ArrayList<ProjectConfigurator>();
		for (IConfigurationElement extension : this.extensions) {
			boolean addIt = false;
			String bundleOrFragmentId = extension.getContributor().getName();
			Bundle contributingBundle = Platform.getBundle(bundleOrFragmentId);
			if (contributingBundle.getState() == Bundle.ACTIVE) {
				addIt = true;
			} else {
				// Else, only load class and activate bundle if necessary (checked by activeWhen)
				IConfigurationElement[] activeWhenElements = extension.getChildren("activeWhen");
				if (activeWhenElements.length == 0) {
					// by default, if no activeWhen, enable extension
					addIt = true;
				} else if (activeWhenElements.length == 1) {
					IConfigurationElement activeWhen = activeWhenElements[0];
					IConfigurationElement[] activeWhenChildren = activeWhen.getChildren();
					if (activeWhenChildren.length == 1) {
						try {
							Expression expression = this.expressionConverter.perform(activeWhen.getChildren()[0]);
							IEvaluationContext context = new EvaluationContext(null, container);
							addIt = expression.evaluate(context).equals(EvaluationResult.TRUE);
						} catch (CoreException ex) {
							Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not evaluate expression for " + extension.getContributor().getName(), ex));
						}
					} else {
						Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
								"Could not evaluate xpression for " + extension.getContributor().getName() + ": there must be exactly one child of 'activeWhen'"));
					}
				} else {
					throw new IllegalArgumentException("Only one 'activeWhen' is authorized on extension point " + EXTENSION_POINT_ID + ", for extension contributed by " +
							extension.getContributor().getName());
				}
			}
			if (addIt) {
				ProjectConfigurator configurator = getConfigurator(extension);
				if (configurator instanceof EclipseProjectConfigurator) {
					// give priority
					res.add(0, configurator);
				} else {
					res.add(configurator);
				}
			}
		}
		return res;
	}

	/**
	 * 
	 * @param container
	 * @return The active connectors for given container, order is important: top-priority are 1st
	 */
	public List<ProjectConfigurator> getAllActiveProjectConfigurators(IContainer container) {
		return this.getAllActiveProjectConfiguratorsUntyped(container);
	}

	/**
	 * 
	 * @param folder
	 * @return The active connectors for given folder, order is important: top-priority are 1st
	 */
	public List<ProjectConfigurator> getAllActiveProjectConfigurators(File folder) {
		Assert.isTrue(folder.isDirectory());
		return this.getAllActiveProjectConfiguratorsUntyped(folder);
	}

	private ProjectConfigurator getConfigurator(IConfigurationElement extension) {
		if (!this.configuratorsByExtension.containsKey(extension)) {
			try {
				ProjectConfigurator configurator = (ProjectConfigurator) extension.createExecutableExtension("class"); //$NON-NLS-1$
				this.configuratorsByExtension.put(extension, configurator);
				return configurator;
			} catch (CoreException ex) {
				Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
				return null;
			}
		} else {
			return this.configuratorsByExtension.get(extension);
		}
	}
	
	public static List<String> getAllExtensionLabels() {
		IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
		List<String> res = new ArrayList<String>(extensions.length);
		for (IConfigurationElement extension : extensions) {
			res.add(extension.getAttribute("label"));
		}
		return res;
	}

	public static Object getLabel(ProjectConfigurator configurator) {
		IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
		for (IConfigurationElement extension : extensions) {
			if (configurator.getClass().getName().equals(extension.getAttribute("class"))) {
				return extension.getAttribute("label");
			}
		}
		return "Missing label for " + configurator.getClass().getName(); //$NON-NLS-1$
	}

}
