package org.eclipse.e4.extensions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.IContextConstants;
import org.eclipse.e4.ui.model.application.MContributedPart;
import org.eclipse.e4.ui.model.application.MPart;
import org.eclipse.e4.ui.model.workbench.MPerspective;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.workbench.ui.ILegacyHook;
import org.eclipse.e4.workbench.ui.internal.UISchedulerStrategy;
import org.eclipse.e4.workbench.ui.menus.PerspectiveHelper;
import org.eclipse.e4.workbench.ui.renderers.PartFactory;
import org.eclipse.e4.workbench.ui.renderers.swt.SWTPartFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.LegacyWBWImpl;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.LegacyWPSImpl;
import org.eclipse.ui.part.ViewPart;

public class LegacyViewFactory extends SWTPartFactory {

	private IConfigurationElement findPerspectiveFactory(String id) {
		IConfigurationElement[] factories = ExtensionUtils
				.getExtensions(IWorkbenchRegistryConstants.PL_PERSPECTIVES);
		IConfigurationElement theFactory = ExtensionUtils.findExtension(
				factories, id);
		return theFactory;
	}

	private IConfigurationElement findViewConfig(String id) {
		IConfigurationElement[] views = ExtensionUtils
				.getExtensions(IWorkbenchRegistryConstants.PL_VIEWS);
		IConfigurationElement viewContribution = ExtensionUtils.findExtension(
				views, id);
		return viewContribution;
	}

	private IConfigurationElement findEditorConfig(String id) {
		IConfigurationElement[] editors = ExtensionUtils
				.getExtensions(IWorkbenchRegistryConstants.PL_EDITOR);
		IConfigurationElement editorContribution = ExtensionUtils
				.findExtension(editors, id);
		return editorContribution;
	}

	/**
	 * @param part
	 * @param editorElement
	 * @return
	 */
	private Control createEditor(final MContributedPart<MPart<?>> part,
			IConfigurationElement editorElement) {
		final Composite parent = (Composite) getParentWidget(part);

		EditorPart impl = null;
		try {
			impl = (EditorPart) editorElement
					.createExecutableExtension("class"); //$NON-NLS-1$
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (impl == null)
			return null;

		try {
			IEclipseContext parentContext = getContextForParent(part);
			final IEclipseContext localContext = part.getContext();
			localContext.set(IContextConstants.DEBUG_STRING, "Legacy Editor"); //$NON-NLS-1$
			final IEclipseContext outputContext = EclipseContextFactory.create(
					null, UISchedulerStrategy.getInstance());
			outputContext.set(IContextConstants.DEBUG_STRING,
					"ContributedPart-output"); //$NON-NLS-1$
			localContext.set(IServiceConstants.OUTPUTS, outputContext);
			localContext.set(IEclipseContext.class.getName(), outputContext);
			localContext.set(IEditorInput.class.getName(),
					LegacyWBWImpl.hackInput);
			parentContext.set(IServiceConstants.ACTIVE_CHILD, localContext);

			// Assign a 'site' for the newly instantiated part
			LegacyWPSImpl site = new LegacyWPSImpl(part, impl);

			impl.init(site, LegacyWBWImpl.hackInput); // HACK!! needs an
			// editorInput

			impl.createPartControl(parent);
			part.setObject(impl);
			localContext.set(MContributedPart.class.getName(), part);

			// Manage the 'dirty' state
			final EditorPart implementation = impl;
			impl.addPropertyListener(new IPropertyListener() {
				private CTabItem findItemForPart(CTabFolder ctf) {
					CTabItem[] items = ctf.getItems();
					for (int i = 0; i < items.length; i++) {
						if (items[i].getData(PartFactory.OWNING_ME) == part) {
							return items[i];
						}
					}

					return null;
				}

				public void propertyChanged(Object source, int propId) {
					if (parent instanceof CTabFolder) {
						CTabFolder ctf = (CTabFolder) parent;
						CTabItem partItem = findItemForPart(ctf);
						String itemText = partItem.getText();
						if (implementation.isDirty()
								&& itemText.indexOf('*') != 0) {
							itemText = '*' + itemText;
						} else if (itemText.indexOf('*') == 0) {
							itemText = itemText.substring(1);
						}
						partItem.setText(itemText);
					}
					// DebugUITools.openLaunchConfigurationDialogOnGroup(parent
					// .getShell(), null,
					//							"org.eclipse.debug.ui.launchGroup.debug"); //$NON-NLS-1$
				}
			});

			if (parent.getChildren().length > 0)
				return parent.getChildren()[parent.getChildren().length - 1];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Control createView(MContributedPart<MPart<?>> part,
			IConfigurationElement viewContribution) {
		Composite parent = (Composite) getParentWidget(part);

		ViewPart impl = null;
		try {
			impl = (ViewPart) viewContribution
					.createExecutableExtension("class"); //$NON-NLS-1$
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (impl == null)
			return null;

		try {
			IEclipseContext parentContext = getContextForParent(part);
			final IEclipseContext localContext = part.getContext();
			localContext.set(IContextConstants.DEBUG_STRING, "Legacy Editor"); //$NON-NLS-1$
			final IEclipseContext outputContext = EclipseContextFactory.create(
					null, UISchedulerStrategy.getInstance());
			outputContext.set(IContextConstants.DEBUG_STRING,
					"ContributedPart-output"); //$NON-NLS-1$
			localContext.set(IServiceConstants.OUTPUTS, outputContext);
			localContext.set(IEclipseContext.class.getName(), outputContext);
			parentContext.set(IServiceConstants.ACTIVE_CHILD, localContext);

			// Assign a 'site' for the newly instantiated part
			LegacyWPSImpl site = new LegacyWPSImpl(part, impl);
			impl.init(site, null);

			impl.createPartControl(parent);
			part.setObject(impl);
			localContext.set(MContributedPart.class.getName(), part);

			// HACK!! presumes it's the -last- child of the parent
			if (parent.getChildren().length > 0)
				return parent.getChildren()[parent.getChildren().length - 1];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object createWidget(MPart<?> part) {
		String partId = part.getId();

		Control newCtrl = null;
		if (part instanceof MPerspective) {
			IConfigurationElement perspFactory = findPerspectiveFactory(partId);
			if (perspFactory != null)
				newCtrl = createPerspective((MPerspective<MPart<?>>) part,
						perspFactory);
			return newCtrl;
		} else if (part instanceof MContributedPart) {
			MContributedPart cp = (MContributedPart) part;

			// HACK!! relies on legacy views -not- having a URI...
			String uri = cp.getURI();
			if (uri != null && uri.length() > 0)
				return null;

			// ensure that the legacy hook is initialized
			context.get(ILegacyHook.class.getName());
			// if this a view ?
			IConfigurationElement viewElement = findViewConfig(partId);
			if (viewElement != null)
				newCtrl = createView((MContributedPart<MPart<?>>) part,
						viewElement);

			IConfigurationElement editorElement = findEditorConfig(partId);
			if (editorElement != null)
				newCtrl = createEditor((MContributedPart<MPart<?>>) part,
						editorElement);
			if (newCtrl == null) {
				Composite pc = (Composite) getParentWidget(part);
				Label lbl = new Label(pc, SWT.BORDER);
				lbl.setText(part.getId());
				newCtrl = lbl;
			}

			return newCtrl;
		}
		return null;
	}

	/**
	 * @param part
	 * @param perspFactory
	 * @return
	 */
	private Control createPerspective(MPerspective<MPart<?>> part,
			IConfigurationElement perspFactory) {
		Widget parentWidget = getParentWidget(part);
		if (!(parentWidget instanceof Composite))
			return null;

		Composite perspArea = new Composite((Composite) parentWidget, SWT.NONE);
		perspArea.setLayout(new FillLayout());

		if (part.getChildren().size() == 0)
			PerspectiveHelper.loadPerspective(part, perspFactory);

		return perspArea;
	}

}
