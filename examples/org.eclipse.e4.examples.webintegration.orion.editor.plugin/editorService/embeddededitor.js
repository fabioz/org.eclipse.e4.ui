/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*global eclipse:true orion:true dojo window editorServiceHandler editor:true */
/*jslint devel:true*/

/**
 * This file demonstrates one way in which a web application could be structured
 * to easily allow different behavior depending on whether the application is hosted
 * in a browser or in an Eclipse Workbench
 **/

/**
 * This object encapsulates the set of actions an editor may perform that would
 * behave differently depending on where the web application is hosted.
 * 
 * The web application implementor may install a set of functions in this object
 * that may differ for browser hosting vs. Eclipse Workbench hosting.
 * 
 * Not all functions need to be hooked.  Also, some functions may only be hooked for
 * one type of hosting scenerio.
 * 
 * This object would be created by the web application developer as they are defining
 * the separation of responsibilities for their web application.
 * 
 * This object is global because it needs to be referenced by the Eclipse Workbench
 **/
var editorService = {
	DIRTY_CHANGED : 1,
	dirtyChanged: function(dirty) {}, // Called by the editor when its dirty state changes

	GET_CONTENT_NAME : 2,
	getContentName: function() {},		// Called to get the current content name.  A file name for example

	GET_INITIAL_CONTENT : 3,
	getInitialContent: function() {},	// Called to get the initial contents for the editor

	SAVE : 4,
	save: function(editor) {},			// Called to persist the contents of the editor

	STATUS_CHANGED : 5,
	statusChanged: function(message, isError) {}	// Called by the editor to report status line changes
};

var editor;

function initEmbeddedEditor(){
	var editorDomNode = dojo.byId("editor");
	
	var textViewFactory = function() {
		return new orion.textview.TextView({
			parent: editorDomNode,
			stylesheet: ["../../orion/textview/textview.css", "../../orion/textview/rulers.css", "../../examples/textview/textstyler.css", "../../examples/editor/htmlStyles.css"],
			tabSize: 4
		});
	};

	var contentAssistFactory = function(editor) {
		var contentAssist = new orion.editor.ContentAssist(editor, "contentassist");
		contentAssist.addProvider(new orion.editor.CssContentAssistProvider(), "css", "\\.css$");
		contentAssist.addProvider(new orion.editor.JavaScriptContentAssistProvider(), "js", "\\.js$");
		return contentAssist;
	};
	
	// Canned highlighters for js, java, and css. Grammar-based highlighter for html
	var syntaxHighlighter = {
		styler: null, 
		
		highlight: function(fileName, textView) {
			if (this.styler) {
				this.styler.destroy();
				this.styler = null;
			}
			if (fileName) {
				var splits = fileName.split(".");
				var extension = splits.pop().toLowerCase();
				if (splits.length > 0) {
					switch(extension) {
						case "js":
							this.styler = new examples.textview.TextStyler(textView, "js");
							break;
						case "java":
							this.styler = new examples.textview.TextStyler(textView, "java");
							break;
						case "css":
							this.styler = new examples.textview.TextStyler(textView, "css");
							break;
						case "html":
							this.styler = new orion.editor.TextMateStyler(textView, orion.editor.HtmlGrammar.grammar);
							break;
					}
				}
			}
		}
	};
	
	var annotationFactory = new orion.editor.AnnotationFactory();

	var keyBindingFactory = function(editor, keyModeStack, undoStack, contentAssist) {
		
		// Create keybindings for generic editing
		var genericBindings = new orion.editor.TextActions(editor, undoStack);
		keyModeStack.push(genericBindings);
		
		// create keybindings for source editing
		var codeBindings = new orion.editor.SourceCodeActions(editor, undoStack, contentAssist);
		keyModeStack.push(codeBindings);
		
		// save binding
		editor.getTextView().setKeyBinding(new orion.textview.KeyBinding("s", true), "save");
		editor.getTextView().setAction("save", function(){
			// The save function is called through the editorService allowing Eclipse and Browser hosted instances to behave differently
			editorService.save(editor);
			return true;
		});
		
		// speaking of save...
		dojo.byId("save").onclick = function() {editorService.save(editor);};

	};
	
	editor = new orion.editor.Editor({
		textViewFactory: textViewFactory,
		undoStackFactory: new orion.editor.UndoFactory(),
		annotationFactory: annotationFactory,
		lineNumberRulerFactory: new orion.editor.LineNumberRulerFactory(),
		contentAssistFactory: contentAssistFactory,
		keyBindingFactory: keyBindingFactory, 
		statusReporter: editorService.statusChanged,
		domNode: editorDomNode
	});
	
	dojo.connect(editor, "onDirtyChange", this, editorService.dirtyChanged); // Hooks the onDirtyChange event listener through the editorService
	
	editor.installTextView();
	
	// Set editor input by calling through editorService
	editor.onInputChange(editorService.getContentName(), null, editorService.getInitialContent());
	
	// Set the syntax highlighter
	syntaxHighlighter.highlight(editorService.getContentName(), editor.getTextView());
} // end of initEmbeddedEditor
	
// Created embedded editor
dojo.addOnLoad(function() {
	
	// Install functions for servicing browser hosted applications
	function installBrowserHooks() {

		// Register a getContentName implementation
		editorService.getContentName = function() {
			return "sample.js";
		};
		
		// Register a getInitialContent implementation
		editorService.getInitialContent = function() {
			return "var foo = function() {window.alert('bar');}; //Initial text.  Try editing it";
		};
			
		// Register a save implementation.
		editorService.save = function(editor) {
			window.alert(editor.getContents());
	
			// Mark editor as saved
			editor.onInputChange(null, null, null, true);
		};
		
		// Register an implementation to display status changes reported by the editor
		editorService.statusChanged = function(message, isError) {
			var status;
			if (isError) {
				status =  "ERROR: " + message;
			} else {
				status = message;
			}
			
			var dirtyIndicator = "";
			if (editor.isDirty()) {
				dirtyIndicator = "*";
			}
			dojo.byId("status").innerHTML = dirtyIndicator + status;
		};
		
		// Prevent the browser tab/window from closing with unsaved changes.
		// Not needed when running in a workbench since the editor lifecycle code takes
		// care of this
		window.onbeforeunload = function() {
			if (editor.isDirty()) {
				 return "There are unsaved changes.";
			}
		};
	}
		
	// Install functions for servicing Eclipse Workbench hosted applications
	function installWorkbenchHooks() {
		// Register a function that will be called by the editor when the editor's dirty state changes
		editorService.dirtyChanged = function(dirty) {
			// This is a function created in Eclipse and registered with the page.
			editorServiceHandler(editorService.DIRTY_CHANGED, dirty);
		};

		// Register a getContentName implementation
		editorService.getContentName = function() {
			// This is a function created in Eclipse and registered with the page.
			return editorServiceHandler(editorService.GET_CONTENT_NAME);
		};
		
		// Register an implementation that can return initial content for the editor
		editorService.getInitialContent = function() {
			// This is a function created in Eclipse and registered with the page.
			return editorServiceHandler(editorService.GET_INITIAL_CONTENT);
		};
		
		// Register an implementation that should run when the editors status changes.
		editorService.statusChanged = function(message, isError) {
			// This is a function created in Eclipse and registered with the page.
			editorServiceHandler(editorService.STATUS_CHANGED, message);
		};

		// Register an implementation that can save the editors contents.		
		editorService.save = function() {
			// This is a function created in Eclipse and registered with the page.
			var result = editorServiceHandler(editorService.SAVE, editor.getContents());
			if (result) {
				editor.onInputChange(null, null, null, true);
			}
			return result;
		};
	}
	
	// Return true if the page is hosted in an Eclipse Workbench, false if hosted in a browser
	function isHostedInWorkbench() {
		// Check if Eclipse has registered the "EditorServiceHandler" BrowserFunction
		return typeof editorServiceHandler=== 'function';
	}
	
	// Install the appropriate editorService for the current hosting environment
	if (isHostedInWorkbench()) {
		installWorkbenchHooks();
	} else {
		installBrowserHooks();
	}
	
	// Initialize the editor
	initEmbeddedEditor();
});