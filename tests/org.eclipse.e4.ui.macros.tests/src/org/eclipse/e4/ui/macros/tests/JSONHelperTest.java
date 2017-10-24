/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - http://eclip.se/8519
 *******************************************************************************/
package org.eclipse.e4.ui.macros.tests;

import org.eclipse.e4.core.macros.internal.JSONHelper;
import org.junit.Assert;
import org.junit.Test;

public class JSONHelperTest {

	@Test
	public void testJSONHelper() {
		StringBuilder builder = new StringBuilder("chars\"\\/\b\f\n\r\t");
		builder.append(new Character((char) 5));
		builder.append(new Character((char) 6));
		String quoted = JSONHelper.quote(builder.toString());
		Assert.assertEquals("\"chars\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0005\\u0006\"", quoted);
	}
}
