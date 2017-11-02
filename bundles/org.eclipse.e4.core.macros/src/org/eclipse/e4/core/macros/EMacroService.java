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
package org.eclipse.e4.core.macros;

/**
 * Extension with the public API for dealing with macros.
 *
 * Can be accessed by getting it as a service:
 * <p>
 * &nbsp;&nbsp;&nbsp;site.getService(EMacroService.class)
 * </p>
 * or by having it injected:
 * <p>
 * &nbsp;&nbsp;&nbsp;@Inject<br/>
 * &nbsp;&nbsp;&nbsp;EMacroService fMacroService;
 * </p>
 * <p>
 * The idea is that clients will become aware that a macro record is taking
 * place (through {@link #isRecording()} and will add their related macro
 * instructions through {@link #addMacroInstruction(IMacroInstruction)}.
 * </p>
 * <p>
 * It's also important to note that any macro instruction added through
 * addMacroInstruction also needs to have an {@link IMacroInstructionFactory}
 * registered through the org.eclipse.e4.core.macros.macroInstructionsFactory
 * extension point (with a match through
 * {@link org.eclipse.e4.core.macros.IMacroInstruction#getId()}).
 * </p>
 */
public interface EMacroService {

	/**
	 * @return whether a macro is currently being recorded (note that it's possible
	 *         for the user to start recording and then playback a macro
	 *         simultaneously -- although the inverse is not true).
	 */
	boolean isRecording();

	/**
	 * @return whether a macro is currently being played back.
	 */
	boolean isPlayingBack();

	/**
	 * Adds a macro instruction to be added to the current macro being recorded. Any
	 * macro instruction added also needs to have an
	 * {@link IMacroInstructionFactory} registered through the
	 * org.eclipse.e4.core.macros.macroInstructionsFactory extension point (with a
	 * match through {@link org.eclipse.e4.core.macros.IMacroInstruction#getId()})
	 *
	 * Does nothing if there's no macro being currently recorded.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be added to the macro currently being
	 *            recorded.
	 */
	void addMacroInstruction(IMacroInstruction macroInstruction);

	int PRIORITY_LOW = 0;

	int PRIORITY_HIGH = 10;

	/**
	 * Adds a macro instruction to be added to the current macro being recorded. The
	 * difference between this method and
	 * {@link #addMacroInstruction(IMacroInstruction)} is that it's meant to be used
	 * when an event may trigger the creation of multiple macro instructions and
	 * only one of those should be recorded.
	 *
	 * For instance, if a given KeyDown event is recorded in a StyledText and later
	 * an action is triggered by this event, the recorded action should overwrite
	 * the KeyDown event.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be added to the macro currently being
	 *            recorded.
	 * @param event
	 *            the event that triggered the creation of the macro instruction to
	 *            be added. If there are multiple macro instructions added for the
	 *            same event, only the one with the highest priority will be kept
	 *            (if 2 events have the same priority, the last one will replace the
	 *            previous one).
	 * @param priority
	 *            the priority of the macro instruction being added (to be compared
	 *            against the priority of other added macro instructions for the
	 *            same event).
	 * @see #addMacroInstruction(IMacroInstruction)
	 */
	void addMacroInstruction(IMacroInstruction macroInstruction, Object event, int priority);

	/**
	 * Toggles the macro record mode (i.e.: if it's currently not recording, starts
	 * recording a macro, otherwise, stops the current record -- at which point a
	 * macro should be saved with what was recorded so far).
	 *
	 * Note that when playing back, calling toggleMacroRecord() should do nothing
	 * (while it's Ok to start recording and then playback a previous macro to add
	 * previously recorded macro instructions to the current macro, the opposite is
	 * not true).
	 */
	void toggleMacroRecord();

	/**
	 * Plays back the last recorded macro.
	 *
	 * Note: it's Ok to call it while a macro is being recorded (which should
	 * playback the given macro and add its contents to the new macro being
	 * recorded).
	 *
	 * @throws MacroPlaybackException
	 */
	void playbackLastMacro() throws MacroPlaybackException;

	/**
	 * Adds a macro state listener to be notified on changes in the macro
	 * record/playback state.
	 *
	 * @param listener
	 *            the listener to be added.
	 */
	void addMacroStateListener(IMacroStateListener listener);

	/**
	 * @param listener
	 *            the macro listener which should no longer be notified of changes.
	 */
	void removeMacroStateListener(IMacroStateListener listener);

	/**
	 * @return the macro record context created when macro record started or null if
	 *         it's not currently recording.
	 */
	IMacroRecordContext getMacroRecordContext();

	/**
	 * @return the macro playback context created when the macro playback started or
	 *         null if it's not currently playing back.
	 */
	IMacroPlaybackContext getMacroPlaybackContext();

	// Deal with managing accepted commands during macro record/playback.
	// (by default should load the command behavior
	// through the org.eclipse.e4.core.macros.commandHandling extension
	// point,
	// but it's possible to programmatically change it as needed later on).

	/**
	 * @param commandId
	 *            the id of the Eclipse Core Command.
	 *
	 * @return whether the command should be recorded for playback when recording a
	 *         macro (i.e.: an {@link org.eclipse.e4.core.macros.IMacroInstruction}
	 *         will be automatically created to play it back when in record mode).
	 *
	 * @see org.eclipse.e4.core.macros.commandHandling extension point
	 */
	@SuppressWarnings("javadoc")
	boolean isCommandRecorded(String commandId);

	/**
	 * @param commandId
	 *            the Eclipse Core Command id to be customized during macro
	 *            record/playback.
	 *
	 * @param recordInMacro
	 *            if true, the command activation will be automatically recorded in
	 *            the macro -- which means that an
	 *            {@link org.eclipse.e4.core.macros.IMacroInstruction} will be
	 *            automatically created to play it back when in record mode. If
	 *            false, the activation of the command will not be recorded.
	 *
	 * @see org.eclipse.e4.core.macros.commandHandling extension point
	 */
	@SuppressWarnings("javadoc")
	void setRecordCommandInMacro(String commandId, boolean recordInMacro);

	/**
	 * Adds a macro instructions listener (it may be added to validate the current
	 * state of the macro recording).
	 *
	 * @param macroInstructionsListener
	 *            the listener for macro instructions.
	 */
	void addMacroInstructionsListener(IMacroInstructionsListener macroInstructionsListener);

	/**
	 * Removes a macro instructions listener.
	 *
	 * @param macroInstructionsListener
	 *            the listener for macro instructions.
	 */
	void removeMacroInstructionsListener(IMacroInstructionsListener macroInstructionsListener);

}
