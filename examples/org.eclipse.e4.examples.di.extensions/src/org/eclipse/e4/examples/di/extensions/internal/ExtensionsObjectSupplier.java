package org.eclipse.e4.examples.di.extensions.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.examples.di.extensions.Extension;
import org.eclipse.e4.examples.di.extensions.ExtensionUpdates;

public class ExtensionsObjectSupplier extends ExtendedObjectSupplier {
	@Inject
	private IExtensionRegistry registry;

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor,
			boolean track, boolean group) {

		final Class<?> desiredType = getDesiredClass(descriptor
				.getDesiredType());
		if (List.class.isAssignableFrom(desiredType)) {
			Extension qualifier = descriptor.getQualifier(Extension.class);
			String extensionPoint = qualifier.value();
			ExtensionUpdates updates = qualifier.updates();
			IExtensionPoint ep = registry.getExtensionPoint(extensionPoint);
			if (ep != null) {
				return Arrays.asList(ep.getConfigurationElements());
			} else if (descriptor.getQualifier(Optional.class) != null) {
				return null;
			}
			throw new InjectionException("Failed to find Extension Point: "
					+ extensionPoint);
		}

		// Annotation used with unsupported type
		return null;
	}

	private Class<?> getDesiredClass(Type desiredType) {
		if (desiredType instanceof Class<?>)
			return (Class<?>) desiredType;
		if (desiredType instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) desiredType).getRawType();
			if (rawType instanceof Class<?>)
				return (Class<?>) rawType;
		}
		return null;
	}
}
