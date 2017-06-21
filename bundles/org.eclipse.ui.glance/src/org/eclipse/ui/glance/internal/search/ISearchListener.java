/*******************************************************************************
 * Copyright (c) 2017 Exyte
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Yuri Strot - initial API and Implementation
 ******************************************************************************/
package org.eclipse.ui.glance.internal.search;

import org.eclipse.ui.glance.sources.Match;

/**
 * @author Yuri Strot
 * 
 */
public interface ISearchListener {

	public void firstFound(Match match);

	public void allFound(Match[] matches);

	public void finished();

}
