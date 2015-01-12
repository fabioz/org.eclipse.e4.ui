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
package org.eclipse.ui.internal.wizards.datatransfer.expressions;

import java.io.File;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;

public class HasFileExpression extends Expression {

	public static final String TAG = "hasFile";

	String path;

	public HasFileExpression(String path) {
		this.path = path;
	}

	public HasFileExpression(IConfigurationElement element) {
		this(element.getAttribute("path"));
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		Object root = context.getDefaultVariable();
		if (root instanceof File) {
			return EvaluationResult.valueOf( new File((File)root, this.path).exists() );
		} else if (root instanceof IContainer) {
			return EvaluationResult.valueOf( ((IContainer)root).getFile(new Path(this.path)).exists() );
		} else if (root instanceof IAdaptable) {
			IContainer container = (IContainer) ((IAdaptable)root).getAdapter(IContainer.class);
			return EvaluationResult.valueOf( container.getFile(new Path(this.path)).exists() );
		}
		return EvaluationResult.FALSE;
	}

}
