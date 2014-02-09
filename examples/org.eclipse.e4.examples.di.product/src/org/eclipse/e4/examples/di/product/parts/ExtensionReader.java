package org.eclipse.e4.examples.di.product.parts;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.examples.di.extensions.Extension;
import org.eclipse.jface.viewers.TableViewer;

public class ExtensionReader {
	@Inject
	@Named("myList")
	Set<AuthorCompany> authors;

	@Inject
	@Named("myViewer")
	TableViewer viewer;

	@Inject
	@Optional
	public void setExtensions(
			final @Extension(SamplePart.EXTENSION_POINT) List<IConfigurationElement> elements) {
		if (elements == null) {
			return;
		}
		viewer.getControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				for (IConfigurationElement element : elements) {
					if (SamplePart.ELEMENT_AUTHOR.equals(element.getName())) {
						authors.add(new AuthorCompany(element
								.getAttribute(SamplePart.ATTR_NAME), element
								.getAttribute(SamplePart.ATTR_COMPANY)));
					}
				}
				viewer.refresh();
			}
		});
	}
}
