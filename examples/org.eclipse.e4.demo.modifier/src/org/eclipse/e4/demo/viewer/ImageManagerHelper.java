/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.demo.viewer;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * Helps manage resources.
 */
public class ImageManagerHelper {

	static final public int IMAGE_WORKBENCH_WINDOW = 1;
	static final public int IMAGE_SASH = 2;
	static final public int IMAGE_STACK = 3;

	static final private String IMAGE_PATH = "platform:/plugin/org.eclipse.e4.demo.modifier/icons/obj16/";

	private ImageRegistry imageRegistry;

	public ImageManagerHelper() {
		imageRegistry = new ImageRegistry();
	}

	public Image getImage(String path) {
		Image image = imageRegistry.get(path);
		if (image != null)
			return image;
		URL url;
		try {
			url = new URL(path);
		} catch (MalformedURLException e) {
			e.printStackTrace(); // report and continue
			return null;
		}
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		image = desc.createImage();
		if (image != null)
			imageRegistry.put(path, image);
		return image;
	}

	public Image getImage(int id) {
		String registryID = Integer.toString(id);
		Image image = imageRegistry.get(registryID);
		if (image != null)
			return image;
		URL url = createURL(id);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		image = desc.createImage();
		if (image != null)
			imageRegistry.put(registryID, image);
		return image;
	}

	private URL createURL(int id) {
		String result = null;
		switch (id) {
		case IMAGE_WORKBENCH_WINDOW: // ISharedImages.IMG_OBJ_PROJECT
			result = IMAGE_PATH + "prj_obj.gif";
			break;
		case IMAGE_SASH: // ISharedImages.IMG_OBJ_FOLDER
			result = IMAGE_PATH + "fldr_obj.gif";
			break;
		case IMAGE_STACK: // ISharedImages.IMG_OBJ_FILE
			result = IMAGE_PATH + "file_obj.gif";
			break;
		default:
		}
		try {
			return new URL(result);
		} catch (MalformedURLException e) {
			e.printStackTrace(); // report and continue
			return null;
		}
	}

	public void dispose() {
		imageRegistry.dispose();
	}

}