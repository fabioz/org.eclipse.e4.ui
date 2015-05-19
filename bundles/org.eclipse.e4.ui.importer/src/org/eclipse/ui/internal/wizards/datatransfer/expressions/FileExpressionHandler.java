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

import org.eclipse.core.expressions.ElementHandler;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class FileExpressionHandler extends ElementHandler {

	@Override
	public Expression create(ExpressionConverter converter, IConfigurationElement element) throws CoreException {
		String name = element.getName();
		if (HasFileExpression.TAG.equals(name)) {
			return new HasFileExpression(element);
		} else if (HasFileRecursivelyExpression.TAG.equals(name)) {
			return new HasFileRecursivelyExpression(element);
		} else if (HasFileWithSuffixRecursivelyExpression.TAG.equals(name)) {
			return new HasFileWithSuffixRecursivelyExpression(element);
		} else if (HasFolderExpression.TAG.equals(name)) {
			return new HasFolderExpression(element);
		}
		return null;
	}
}
