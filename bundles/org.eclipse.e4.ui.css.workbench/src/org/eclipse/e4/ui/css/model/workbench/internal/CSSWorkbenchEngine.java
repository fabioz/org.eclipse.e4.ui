/*******************************************************************************
 * Copyright (c) 2014 Manumitting Technologies Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Brian de Alwis (MTI) - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.css.model.workbench.internal;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.contributions.IContributionFactory;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.core.impl.engine.CSSEngineImpl;
import org.eclipse.e4.ui.css.core.impl.engine.RegistryCSSElementProvider;
import org.eclipse.e4.ui.css.core.impl.engine.RegistryCSSPropertyHandlerProvider;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.osgi.service.event.Event;

public class CSSWorkbenchEngine extends CSSEngineImpl {

	private static final String ENGINE_KEY = CSSEngine.class.getName();

	/**
	 * Convenience method for fetching the appropriate engine for an MUIElement
	 * 
	 * @param e
	 * @return
	 */
	public static CSSWorkbenchEngine getEngine(MUIElement e) {
		while (!(e instanceof MApplication)) {
			e = e.getParent();
		}
		if (e instanceof MApplication) {
			return (CSSWorkbenchEngine) e.getTransientData().get(ENGINE_KEY);
		}
		return null;
	}

	/**
	 * Instantiate the object from the provided class URI
	 * 
	 * @param cssEngine
	 *            the engine context
	 * @param uri
	 *            describes the class (e.g., bundleclass://bundleId/class)
	 * @return the object or {@code null}
	 */
	public static Object createObject(CSSEngine cssEngine, String uri) {
		if (cssEngine instanceof CSSWorkbenchEngine) {
			CSSWorkbenchEngine engine = (CSSWorkbenchEngine) cssEngine;
			IContributionFactory contributionFactory = engine.contributionFactory;
			return contributionFactory.create(uri, engine.app.getContext());
		}
		return null;
	}

	@Inject
	@Optional
	private IContributionFactory contributionFactory;

	private MApplication app;

	private IThemeEngine themeEngine;

	@PostConstruct
	public void init(MApplication app, IExtensionRegistry registry) {
		setElementProvider(new RegistryCSSElementProvider());

		propertyHandlerProviders.add(new RegistryCSSPropertyHandlerProvider(
				registry));

		this.app = app;
		app.getTransientData().put(ENGINE_KEY, this);
		applyStyles(app, true);
	}

	@PreDestroy
	public void dispose() {
		if (app.getTransientData().get(ENGINE_KEY) == this) {
			app.getTransientData().remove(ENGINE_KEY);
		}
	}

	@Inject
	@Optional
	protected void modelChange(
			@EventTopic(UIEvents.UIModelTopicBase + UIEvents.TOPIC_SEP
 + UIEvents.ALL_SUB_TOPICS) Event event) {
		System.out.println(">> event change: " + event);
		Object modelElement = event.getProperty(UIEvents.EventTags.ELEMENT);
		if (modelElement instanceof MApplicationElement) {
			this.applyStyles((MApplicationElement) modelElement, false);
		}
	}

	public void reapply() {
		applyStyles(app, true);
	}

	@Inject
	@Optional
	protected void themeEngineChanged(IThemeEngine themeEngine) {
		if (this.themeEngine != null) {
			this.themeEngine.removeCSSEngine(this);
		}
		this.themeEngine = themeEngine;
		if (this.themeEngine != null) {
			themeEngine.addCSSEngine(this);
		}
	}

}
