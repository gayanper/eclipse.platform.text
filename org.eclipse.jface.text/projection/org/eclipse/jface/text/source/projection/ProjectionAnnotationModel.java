/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.text.source.projection;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;


/**
 * A projection annotation model.
 * <p>
 * Internal class. Do not use. Public only for testing purposes.
 * 
 * @since 3.0
 */
public class ProjectionAnnotationModel extends AnnotationModel {
	
	
	private class Summarizer extends Thread {
				
		public Summarizer() {
			setDaemon(true);
			start();
			synchronized (fLock) {
				fHasStarted= false;
			}
		}
		
		/*
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			
			synchronized (fLock) {
				fHasStarted= true;
			}
			
			fProjectionSummary.updateSummaries(new NullProgressMonitor());
			
			synchronized (fLock) {
				if (fSummarizer == this)
					fSummarizer= null;
			}
		}
	}
	
	
	private ProjectionSummary fProjectionSummary;
	
	private Object fLock= new Object();
	private volatile Summarizer fSummarizer;
	private volatile boolean fHasStarted= false;
	
	
	public ProjectionAnnotationModel(ProjectionSummary summary) {
		fProjectionSummary= summary;
	}
	
	public void setProjectionSummary(ProjectionSummary summary) {
		fProjectionSummary= summary;
	}
	
	private void updateSummaries() {
		if (fProjectionSummary == null)
			return;
		
		synchronized (fLock) {
			if (fSummarizer == null || fHasStarted)
				fSummarizer= new Summarizer();
		}
	}

	/**
	 * Changes the state of the given annotation to collapsed. An appropriate
	 * annotation model change event is sent out.
	 * 
	 * @param annotation the annotation
	 */
	public void collapse(Annotation annotation) {
		if (annotation instanceof ProjectionAnnotation) {
			ProjectionAnnotation projection= (ProjectionAnnotation) annotation;
			if (!projection.isCollapsed()) {
				projection.markCollapsed();
				modifyAnnotation(projection, true);
				updateSummaries();
			}
		}
	}

	/**
	 * Changes the state of the given annotation to expanded. An appropriate
	 * annotation model change event is sent out.
	 * 
	 * @param annotation the annotation
	 */
	public void expand(Annotation annotation) {
		if (annotation instanceof ProjectionAnnotation) {
			ProjectionAnnotation projection= (ProjectionAnnotation) annotation;
			if (projection.isCollapsed()) {
				projection.markExpanded();
				modifyAnnotation(projection, true);
				updateSummaries();
			}
		}
	}
	
	/**
	 * Toggles the expansion state of the given annotation. An appropriate
	 * annotation model change event is sent out.
	 * 
	 * @param annotation the annotation
	 */
	public void toggleExpansionState(Annotation annotation) {
		if (annotation instanceof ProjectionAnnotation) {
			ProjectionAnnotation projection= (ProjectionAnnotation) annotation;
			
			if (projection.isCollapsed())
				projection.markExpanded();
			else
				projection.markCollapsed();
	
			modifyAnnotation(projection, true);
			updateSummaries();
		}
	}
	
	/**
	 * Expands all annotations that overlap with the given range and are collapsed.
	 * 
	 * @param offset the range offset
	 * @param length the range length
	 * @return <code>true</code> if any annotation has been expanded, <code>false</code> otherwise
	 */
	public boolean expandAll(int offset, int length) {
		return expandAll(offset, length, true);
	}
	
	/**
	 * Expands all annotations that overlap with the given range and are collapsed. Fires a model change event if
	 * requested. 
	 * 
	 * @param offset the offset of the range
	 * @param length the length of the range
	 * @param fireModelChanged <code>true</code> if a model change event
	 *            should be fired, <code>false</code> otherwise
	 * @return <code>true</code> if any annotation has been expanded, <code>false</code> otherwise
	 */
	protected boolean expandAll(int offset, int length, boolean fireModelChanged) {
		
		boolean expanding= false;
		
		Iterator iterator= getAnnotationIterator();
		while (iterator.hasNext()) {
			ProjectionAnnotation annotation= (ProjectionAnnotation) iterator.next();
			if (annotation.isCollapsed()) {
				Position position= getPosition(annotation);
				if (position.overlapsWith(offset, length) /* || is a delete at the boundary */ ) {
					annotation.markExpanded();
					modifyAnnotation(annotation, false);
					expanding= true;
				}
			}
		}
		
		if (expanding) {
			if (fireModelChanged)
				fireModelChanged();
			updateSummaries();
		}
		
		return expanding;
	}
	
	/**
	 * Modifies the annotation model.
	 * 
	 * @param deletions the list of deleted annotations
	 * @param additions the set of annotations to add together with their associated position
	 * @param modifications the list of modified annotations
	 */
	public void modifyAnnotations(Annotation[] deletions, Map additions, Annotation[] modifications) {
		try {
			replaceAnnotations(deletions, additions, false);
			if (modifications != null) {
				for (int i= 0; i < modifications.length; i++)
					modifyAnnotation(modifications[i], false);
			}
		} catch (BadLocationException x) {
		}
		fireModelChanged();
	}
}