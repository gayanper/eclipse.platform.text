/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jface.text.reconciler;


import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;


/**
 * Standard implementation of <code>IReconciler</code>. The reconciler
 * is configured with a single <code>IReconcilingStrategy</code> that is
 * used independ from where a dirty region is located in the reconciler's
 * document. <p>
 * Usually, clients instantiate this class and configure it before using it.
 *
 * @see IReconciler
 * @see org.eclipse.jface.text.IDocumentListener
 * @see org.eclipse.jface.text.ITextInputListener
 * @see DirtyRegion
 * @since 2.0
 */
public class MonoReconciler extends AbstractReconciler {
		
	/** The reconciling strategy */
	private IReconcilingStrategy fStrategy;
	
	
	/**
	 * Creates a new reconciler that uses the same reconciling strategy to
	 * reconcile its document independent of the type of the document's contents.
	 * 
	 * @param strategy the reconciling strategy to be used
	 * @param isIncremental the indication whether strategy is incremental or not
	 */ 
	public MonoReconciler(IReconcilingStrategy strategy, boolean isIncremental) {
		super();
		
		Assert.isNotNull(strategy);
		
		fStrategy= strategy;
		setIsIncrementalReconciler(isIncremental);
	}
		
	/*
	 * @see IReconciler#getReconcilingStrategy
	 */
	public IReconcilingStrategy getReconcilingStrategy(String contentType) {
		Assert.isNotNull(contentType);
		return fStrategy;
	}
	
	/*
	 * @see AbstractReconciler#process(DirtyRegion)
	 */
	protected void process(DirtyRegion dirtyRegion) {
		
		if(dirtyRegion != null)
			fStrategy.reconcile(dirtyRegion, dirtyRegion);
		else {
			IDocument document= getDocument();
			if (document != null)	
				fStrategy.reconcile(new Region(0, document.getLength()));
		}
	}
	
	/*
	 * @see AbstractReconciler#reconcilerDocumentChanged(IDocument)
	 */
	protected void reconcilerDocumentChanged(IDocument document) {
		fStrategy.setDocument(document);
	}	
	
	/*
	 * @see AbstractReconciler#setProgressMonitor(IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		super.setProgressMonitor(monitor);
		if (fStrategy instanceof IReconcilingStrategyExtension) {
			IReconcilingStrategyExtension extension= (IReconcilingStrategyExtension) fStrategy;
			extension.setProgressMonitor(monitor);
		}
	}
	
	/*
	 * @see AbstractReconciler#initialProcess()
	 */
	protected void initialProcess() {
		if (fStrategy instanceof IReconcilingStrategyExtension) {
			IReconcilingStrategyExtension extension= (IReconcilingStrategyExtension) fStrategy;
			extension.initialReconcile();
		}
	}
}
