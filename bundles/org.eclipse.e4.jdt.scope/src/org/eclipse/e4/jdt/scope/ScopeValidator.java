/*******************************************************************************
 * Copyright (C) 2016, Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.e4.jdt.scope;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.core.search.TypeDeclarationMatch;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.jdt.internal.core.search.matching.TypeReferencePattern;

/**
 * This builder can be added on a Java project to implement the concept of scopes
 * and put error markers in case a class access a classpath entry that is not available
 * in its scope.
 * The scope are defined as {@link IClasspathAttribute}, using SCOPE_ATTRIBUTE and SCOPE_TAG_ATTRIBUTE.
 * @author mistria
 *
 */
public class ScopeValidator extends IncrementalProjectBuilder {

	private final class CreateMarkerForWrongScopeReferenceRequestor extends SearchRequestor {
		private ICompilationUnit compilationUnit;
		private Set<IClasspathEntry> relevantEntries;

		public CreateMarkerForWrongScopeReferenceRequestor(ICompilationUnit cu, Set<IClasspathEntry> relevantEntries) {
			this.compilationUnit = cu;
			this.relevantEntries = relevantEntries;
		}

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			IType resolvedType = null;
			if (match instanceof TypeReferenceMatch) {
				TypeReferenceMatch typeRefMatch = (TypeReferenceMatch) match;
				String type = null;
				if (typeRefMatch.getElement() instanceof IImportDeclaration) {
					type = ((IImportDeclaration)match.getElement()).getElementName().toString();
				} else {
					char[] substring = new char[match.getLength()];
					char[] content = match.getParticipant().getDocument(match.getResource().getFullPath().toString()).getCharContents();
					System.arraycopy(content, match.getOffset(), substring, 0, match.getLength());
					type = new String(substring).trim();
				}
				resolvedType = this.compilationUnit.getJavaProject().findType(type);
			} else if (match instanceof TypeDeclarationMatch) {
				TypeDeclarationMatch typeDeclationMatch = (TypeDeclarationMatch)match;
				resolvedType = (IType)typeDeclationMatch.getElement();
			}
			if (resolvedType == null) {
				// unknown type. Skipping?
				return;
			}
			IPackageFragmentRoot resolvedTypePkgRoot = (IPackageFragmentRoot)resolvedType.getPackageFragment().getParent();
			if (relevantEntries.contains(resolvedTypePkgRoot.getResolvedClasspathEntry())) {
				return;
			}
			IMarker marker = this.compilationUnit.getResource().createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, "Type not in scope - " + resolvedType.getFullyQualifiedName());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.CHAR_START, match.getOffset());
			marker.setAttribute(IMarker.CHAR_END, match.getOffset() + match.getLength());
			marker.setAttribute(IMarker.LINE_NUMBER, 1);
		}
	}

	private static final String BUNDLE_ID = "org.eclipse.e4.jdt.scope";
	public static final String BUILDER_ID = BUNDLE_ID + ".scopeBuilder";
	public static final String MARKER_TYPE = BUNDLE_ID + ".scope";
	/**
	 * This key is for a classpath entry attribute that is meant to be attached
	 * to source folders to associate them a scope tag.
	 */
	public static final String SCOPE_TAG_ATTRIBUTE = "scopeTag"; //$NON-NLS-1$
	/**
	 * This key is for a classpath entry attribute that is used by the {@link ScopeValidator}
	 * to identify to which source folders/scopes this entry is accessible.
	 * The value of the attribute is meant to be a list of source folders or scope tags (see
	 * SCOPE_TAG_ATTRIBUTE) that can access the content of the classpath entry.
	 * If a classpath entry doesn't set it, it's interpreted as being accessible from
	 * any source folder.
	 */
	public static final String SCOPE_ATTRIBUTE = "scope"; //$NON-NLS-1$
	
	public ScopeValidator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		final IJavaProject javaProject = (IJavaProject) getProject().getNature(JavaCore.NATURE_ID);
		
		if (!projectUseScope(javaProject)) {
			return new IProject[0]; 
		}
		Map<IFolder, Set<IClasspathEntry>> classpathForSrcFolder = buildScopeModel(javaProject);
		
		Set<ICompilationUnit> units = new HashSet<ICompilationUnit>();
		if (getDelta(getProject()) != null && getDelta(getProject()).getResource() != javaProject.getResource()) {
			getDelta(getProject()).accept(delta -> {
				if (delta.getKind() == IResourceDelta.CHANGED) {
					ICompilationUnit cu = delta.getResource().getAdapter(ICompilationUnit.class);
					units.add(cu);
				}
				return false;
			});
		} else /* build whole project */ {
			Queue<IJavaElement> toProcess = new LinkedList<>();
			toProcess.addAll(Arrays.asList(javaProject.getPackageFragments()));
			while (!toProcess.isEmpty()) {
				IJavaElement current = toProcess.poll();
				if (current.getElementType() == IJavaElement.COMPILATION_UNIT) {
					units.add((ICompilationUnit)current);
				} else if (current instanceof IParent) {
					toProcess.addAll(Arrays.asList(((IParent)current).getChildren()));
				}
			}
		}

		final IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
		final SearchEngine searchEngine = new SearchEngine(units.toArray(new ICompilationUnit[units.size()]));
		SearchPattern pattern = new TypeReferencePattern(null, null, SearchPattern.R_PATTERN_MATCH);
		for (ICompilationUnit cu : units) {
			IResource resource = cu.getResource();
			resource.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			Set<IClasspathEntry> relevantEntries = null;
			for (Entry<IFolder, Set<IClasspathEntry>> entry : classpathForSrcFolder.entrySet()) {
				if (entry.getKey().getFullPath().isPrefixOf(resource.getFullPath())) {
					relevantEntries = entry.getValue();
				}
			}
			boolean hasScopeRestrictions = relevantEntries.size() != resolvedClasspath.length; 
			if (hasScopeRestrictions) {
				SearchRequestor requestor = new CreateMarkerForWrongScopeReferenceRequestor(cu, relevantEntries);
				
				/* This search detects the text area causing the error and place markers underlying it
				 but it misses some references and requires some parsing */
				searchEngine.search(pattern,
						new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
						SearchEngine.createJavaSearchScope(new IJavaElement[] { cu }),
						requestor, monitor);
				
				/* This search is more exhaustive (checks all types) but doesn't assign ranges on markers
				 so they appear on the file and on 1st line, without underlying the area in error */
				//searchEngine.searchDeclarationsOfReferencedTypes(cu, requestor, monitor);
			}
		}
		return new IProject[] { getProject() };
	}

	private Map<IFolder, Set<IClasspathEntry>> buildScopeModel(IJavaProject javaProject) throws JavaModelException {
		final Map<IFolder, Set<IClasspathEntry>> classpathForSrcFolder= new HashMap<>();
		final IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
		final Map<String, Set<IFolder>> scopeTagToSrc = new HashMap<>();
		for (IClasspathEntry entry : resolvedClasspath) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IFolder sourceFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(entry.getPath());
				classpathForSrcFolder.put(sourceFolder, new HashSet<>());
				String scopeTag = getAttribute(entry, SCOPE_TAG_ATTRIBUTE);
				if (scopeTag != null) {
					if (!scopeTagToSrc.containsKey(scopeTag)) {
						scopeTagToSrc.put(scopeTag, new HashSet<>());
					}
					scopeTagToSrc.get(scopeTag).add(sourceFolder);
				}
			}
		}
		for (IClasspathEntry entry : resolvedClasspath) {
			boolean isScopedCPEntry = false;
			String scopeValue = getAttribute(entry, SCOPE_ATTRIBUTE);
			if (scopeValue != null) {
				String[] foldersOrTags = scopeValue.split(","); //$NON-NLS-1$
				isScopedCPEntry = true;
				for (String folderOrTag : foldersOrTags) {
					IFolder folder = getProject().getFolder(folderOrTag);
					if (folder.exists() && scopeTagToSrc.containsKey(folderOrTag)) {
						// TODO: log ambiguous behavior, or put a marker
					}
					if (folder.exists()) {
						classpathForSrcFolder.get(folder).add(entry);
					}
					if (scopeTagToSrc.containsKey(folderOrTag)) {
						for (IFolder sourceFolder : scopeTagToSrc.get(folderOrTag)) {
							classpathForSrcFolder.get(sourceFolder).add(entry);
						}
					}
				}
			}
			if (!isScopedCPEntry) {
				for (Entry<IFolder, Set<IClasspathEntry>> scope : classpathForSrcFolder.entrySet()) {
					scope.getValue().add(entry);
				}
			}
		}
		return classpathForSrcFolder;
	}

	private boolean projectUseScope(final IJavaProject javaProject) throws JavaModelException {
		final IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
		for (IClasspathEntry entry : resolvedClasspath) {
			String scopeValue = getAttribute(entry, SCOPE_ATTRIBUTE);
			if (scopeValue != null) {
				return true;
			}
		}
		return false;
	}

	private String getAttribute(IClasspathEntry entry, String attributeKey) {
		for (IClasspathAttribute att : entry.getExtraAttributes()) {
			if (att.getName().equals(attributeKey)) {
				return att.getValue();
			}
		}
		return null;
	}

}
