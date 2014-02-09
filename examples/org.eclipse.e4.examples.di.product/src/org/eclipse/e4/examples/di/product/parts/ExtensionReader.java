package org.eclipse.e4.examples.di.product.parts;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.examples.di.extensions.Extension;

public class ExtensionReader {
	@Inject
	@Optional
	public void setExtensions(
			@Extension("org.eclipse.e4.examples.di.product.authors") List<IConfigurationElement> elements) {
		if (elements == null) {
			return;
		}
		for (IConfigurationElement author : elements) {
			System.out.println("Author: " + author.getAttribute("name"));
		}
	}
}
