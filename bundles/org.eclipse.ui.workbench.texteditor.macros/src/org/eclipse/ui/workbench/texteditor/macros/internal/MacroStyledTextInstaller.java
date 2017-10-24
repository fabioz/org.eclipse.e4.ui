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
package org.eclipse.ui.workbench.texteditor.macros.internal;

import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroContext;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.core.macros.IMacroStateListener1;
import org.eclipse.e4.ui.macros.internal.EditorUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextOperationTargetExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorExtension5;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * Helper class to deal with entering/exiting macro record/playback.
 */
public class MacroStyledTextInstaller implements IMacroStateListener, IMacroStateListener1 {

	/**
	 * Constant used to keep memento on the macro context.
	 */
	private static final String MACRO_STYLED_TEXT_INSTALLER_MEMENTO = "MACRO_STYLED_TEXT_INSTALLER_MEMENTO"; //$NON-NLS-1$

	/**
	 * Constant used to keep macro recorder on the macro context.
	 */
	private static final String MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER = "MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER"; //$NON-NLS-1$

	/**
	 * Constant used to save whether the content assist was enabled before being
	 * disabled in disableCodeCompletion.
	 */
	private static final String CONTENT_ASSIST_ENABLED = "contentAssistEnabled";//$NON-NLS-1$

	/**
	 * Constant used to save whether the quick assist was enabled before being
	 * disabled in disableCodeCompletion.
	 */
	private static final String QUICK_ASSIST_ENABLED = "quickAssistEnabled";//$NON-NLS-1$

	/**
	 * Re-enables the content assist based on the state of the key
	 * {@link #CONTENT_ASSIST_ENABLED} in the passed memento.
	 *
	 * @param memento
	 *            the memento where a key with {@link #CONTENT_ASSIST_ENABLED} with
	 *            the enabled state of the content assist to be restored.
	 */
	private void leaveMacroMode(IMemento memento, IMacroContext context) {
		IEditorPart editorPart = EditorUtils.getTargetEditorPart(context);
		if (editorPart != null) {
			ITextOperationTarget textOperationTarget = editorPart.getAdapter(ITextOperationTarget.class);
			if (textOperationTarget instanceof ITextOperationTargetExtension) {
				ITextOperationTargetExtension targetExtension = (ITextOperationTargetExtension) textOperationTarget;
				if (textOperationTarget instanceof ITextOperationTargetExtension) {
					restore(memento, targetExtension, ISourceViewer.CONTENTASSIST_PROPOSALS, CONTENT_ASSIST_ENABLED);
					restore(memento, targetExtension, ISourceViewer.QUICK_ASSIST, QUICK_ASSIST_ENABLED);
				}
			}

			if (editorPart instanceof ITextEditor) {
				ITextEditor textEditor = (ITextEditor) editorPart;
				restore(memento, textEditor, ITextEditorActionConstants.CONTENT_ASSIST);
				restore(memento, textEditor, ITextEditorActionConstants.QUICK_ASSIST);
				restore(memento, textEditor, ITextEditorActionConstants.BLOCK_SELECTION_MODE);
			}
		}

	}

	/**
	 * Disables the content assist and saves the previous state on the passed
	 * memento (note that it's only saved if it is actually disabled here).
	 *
	 * @param memento
	 *            memento where the previous state should be saved, to be properly
	 *            restored later on in {@link #leaveMacroMode(IMemento)}.
	 */
	private void enterMacroMode(IMemento memento, IMacroContext context) {
		IEditorPart editorPart = EditorUtils.getTargetEditorPart(context);
		if (editorPart instanceof ITextEditorExtension5) {
			ITextEditorExtension5 iTextEditorExtension5 = (ITextEditorExtension5) editorPart;
			if (iTextEditorExtension5.isBlockSelectionModeEnabled()) {
				// Note: macro can't deal with block selections... there's nothing really
				// inherent to not being able to work, but given:
				// org.eclipse.jface.text.TextViewer.verifyEventInBlockSelection(VerifyEvent)
				// and the fact that we don't generate events through the Display (because it's
				// too error prone -- so much that it could target the wrong IDE instance for
				// the events because it deals with system messages and not really events
				// inside the IDE) and the fact that we can't force a new system message time
				// for temporary events created internally, makes it really hard to work
				// around the hack in verifyEventInBlockSelection.
				// So, we simply disable block selection mode as well as the action which would
				// activate it.

				// Note: ideally we'd have a way to set a new time for the time returned in
				// org.eclipse.swt.widgets.Display.getLastEventTime()
				// -- as it is, internally the events time will be always the same because
				// there's no API to reset it -- if possible we should reset it when we
				// generate our internal events at:
				// org.eclipse.ui.workbench.texteditor.macros.internal.StyledTextKeyDownMacroInstruction.execute(IMacroPlaybackContext)
				iTextEditorExtension5.setBlockSelectionMode(false);
			}
		}

		if (editorPart instanceof ITextEditor) {
			ITextEditor textEditor = (ITextEditor) editorPart;
			disable(memento, textEditor, ITextEditorActionConstants.CONTENT_ASSIST);
			disable(memento, textEditor, ITextEditorActionConstants.QUICK_ASSIST);
			disable(memento, textEditor, ITextEditorActionConstants.BLOCK_SELECTION_MODE);
		}

		if (editorPart != null) {
			ITextOperationTarget textOperationTarget = editorPart.getAdapter(ITextOperationTarget.class);
			if (textOperationTarget instanceof ITextOperationTargetExtension) {
				ITextOperationTargetExtension targetExtension = (ITextOperationTargetExtension) textOperationTarget;

				// Disable content assist and mark it to be restored later on
				disable(memento, textOperationTarget, targetExtension, ISourceViewer.CONTENTASSIST_PROPOSALS,
						CONTENT_ASSIST_ENABLED);
				disable(memento, textOperationTarget, targetExtension, ISourceViewer.QUICK_ASSIST,
						QUICK_ASSIST_ENABLED);
			}
		}
	}

	private void restore(IMemento memento, ITextOperationTargetExtension targetExtension, int operation,
			String preference) {
		Boolean contentAssistProposalsBeforMacroMode = memento.getBoolean(preference);
		if (contentAssistProposalsBeforMacroMode != null) {
			if ((contentAssistProposalsBeforMacroMode).booleanValue()) {
				targetExtension.enableOperation(operation, true);
			} else {
				targetExtension.enableOperation(operation, false);
			}
		}
	}

	private void restore(IMemento memento, ITextEditor textEditor, String actionId) {
		Boolean b = memento.getBoolean(actionId);
		if (b != null && b) {
			Control control = textEditor.getAdapter(Control.class);
			if (control != null && !control.isDisposed()) {
				// Do nothing if already disposed.
				IAction action = textEditor.getAction(actionId);
				if (action instanceof TextEditorAction) {
					TextEditorAction textEditorAction = (TextEditorAction) action;
					textEditorAction.setEditor(textEditor);
					textEditorAction.update();
				}
			}
		}
	}

	private void disable(IMemento memento, ITextOperationTarget textOperationTarget,
			ITextOperationTargetExtension targetExtension, int operation, String preference) {
		if (textOperationTarget.canDoOperation(operation)) {
			memento.putBoolean(preference, true);
			targetExtension.enableOperation(operation, false);
		}
	}

	private void disable(IMemento memento, ITextEditor textEditor, String actionId) {
		IAction action = textEditor.getAction(actionId);
		if (action != null && action instanceof TextEditorAction) {
			TextEditorAction textEditorAction = (TextEditorAction) action;
			memento.putBoolean(actionId, true);
			textEditorAction.setEditor(null);
			textEditorAction.update();
		}
	}

	@Override
	public void onMacroPlaybackContextCreated(IMacroPlaybackContext context) {
		EditorUtils.cacheTargetEditorPart(context);
		EditorUtils.cacheTargetStyledText(context);
	}

	@Override
	public void onMacroRecordContextCreated(IMacroRecordContext context) {
		EditorUtils.cacheTargetEditorPart(context);
		EditorUtils.cacheTargetStyledText(context);
	}

	/**
	 * Implemented to properly deal with macro recording/playback (i.e.: the editor
	 * may need to disable content assist during macro recording and it needs to
	 * record keystrokes to be played back afterwards).
	 */
	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange) {

		if (stateChange == StateChange.RECORD_STARTED) {
			enterMacroMode(macroService.getMacroRecordContext(), macroService.getMacroPlaybackContext());
			enableRecording(macroService, macroService.getMacroRecordContext());

		} else if (stateChange == StateChange.RECORD_FINISHED) {
			leaveMacroMode(macroService.getMacroRecordContext(), macroService.getMacroPlaybackContext());
			disableRecording(macroService.getMacroRecordContext());

		} else if (stateChange == StateChange.PLAYBACK_STARTED) {
			enterMacroMode(macroService.getMacroPlaybackContext(), macroService.getMacroRecordContext());

		} else if (stateChange == StateChange.PLAYBACK_FINISHED) {
			leaveMacroMode(macroService.getMacroPlaybackContext(), macroService.getMacroRecordContext());

		}
	}

	private void enterMacroMode(IMacroContext context, IMacroContext otherContext) {
		StyledText currentStyledText = EditorUtils.getTargetStyledText(context);
		StyledText otherStyledText = EditorUtils.getTargetStyledText(otherContext);
		if (currentStyledText == otherStyledText) {
			return; // If they're the same in both it means we already entered macro mode in the
					// other before.
		}
		Object object = context.get(MACRO_STYLED_TEXT_INSTALLER_MEMENTO);
		if (object == null) {
			XMLMemento mementoStateBeforeMacro = XMLMemento.createWriteRoot("AbstractTextEditorXmlMemento"); //$NON-NLS-1$
			enterMacroMode(mementoStateBeforeMacro, context);
		}
	}

	private void leaveMacroMode(IMacroContext context, IMacroContext otherContext) {
		StyledText currentStyledText = EditorUtils.getTargetStyledText(context);
		StyledText otherStyledText = EditorUtils.getTargetStyledText(otherContext);
		if (currentStyledText == otherStyledText) {
			return; // If they're the same in both it means we still can't exit macro mode.
		}

		// Restores content assist if it was disabled (based on the memento)
		Object object = context.get(MACRO_STYLED_TEXT_INSTALLER_MEMENTO);
		if (object instanceof XMLMemento) {
			XMLMemento mementoStateBeforeMacro = (XMLMemento) object;
			leaveMacroMode(mementoStateBeforeMacro, context);
		}
	}

	private void enableRecording(EMacroService macroService, IMacroRecordContext context) {
		// When recording install a recorder for key events (and uninstall
		// if not recording).
		// Note: affects only current editor
		Object object = context.get(MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER);
		if (object == null) {
			if (macroService.isRecording()) {
				StyledText targetStyledText = EditorUtils.getTargetStyledText(context);
				if (targetStyledText != null && !targetStyledText.isDisposed()) {
					StyledTextMacroRecorder styledTextMacroRecorder = new StyledTextMacroRecorder(macroService);
					styledTextMacroRecorder.install(targetStyledText);
					context.set(MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER, styledTextMacroRecorder);
				}
			}
		}
	}

	private void disableRecording(IMacroRecordContext context) {
		StyledText currentStyledText = EditorUtils.getTargetStyledText(context);
		Object object = context.get(MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER);
		if (object instanceof StyledTextMacroRecorder) {
			StyledTextMacroRecorder styledTextMacroRecorder = (StyledTextMacroRecorder) object;
			if (currentStyledText != null && !currentStyledText.isDisposed()) {
				styledTextMacroRecorder.uninstall(currentStyledText);
			}
		}
	}

}
