/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jface.text;


import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;


/**
 * An information control manager that shows information on mouse hover events.
 * The mouse hover events are caught by registering a <code>MouseTrackListener</code>
 * on the manager's subject control. The manager has by default an information control closer
 * that closes the information control as soon as the mouse pointer leaves the subject area, the
 * user presses a key, or the subject control is resized, moved, or deactivated.<p>
 * When being activated by a mouse hover event, the manager disables itself, until the mouse
 * leaves the subject area. Thus, the manager is usually still disabled, when the information control
 * has already been closed by the closer.
 * 
 * @see org.eclipse.swt.events.MouseTrackListener
 * @since 2.0
 */
abstract public class AbstractHoverInformationControlManager extends AbstractInformationControlManager {		
	
	
	/**
	 * The  information control closer for the hover information. Closes the information control as 
	 * soon as the mouse pointer leaves the subject area, the user presses a key, or the subject
	 * control is resized or moved.
	 */
	class Closer extends MouseTrackAdapter 
		implements IInformationControlCloser, MouseListener, MouseMoveListener, ControlListener, KeyListener {
		
		/** The closer's subject control */
		private Control fSubjectControl;
		/** The closer's information control */
		private IInformationControl fInformationControl;
		/** The subject area */
		private Rectangle fSubjectArea;
		/** Indicates whether this closer is active */
		private boolean fIsActive= false;
		
		/**
		 * Creates a new information control closer.
		 */
		public Closer() {
		}
		
		/*
		 * @see IInformationControlCloser#setSubjectControl(Control)
		 */
		public void setSubjectControl(Control control) {
			fSubjectControl= control;
		}
		
		/*
		 * @see IInformationControlCloser#setHoverControl(IHoverControl)
		 */
		public void setInformationControl(IInformationControl control) {
			fInformationControl= control;
		}
		
		/*
		 * @see IInformationControlCloser#start(Rectangle)
		 */
		public void start(Rectangle subjectArea) {
			
			if (fIsActive)
				return;
			fIsActive= true;
			
			fSubjectArea= subjectArea;
			
			setEnabled(false);
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.addMouseListener(this);
				fSubjectControl.addMouseMoveListener(this);
				fSubjectControl.addMouseTrackListener(this);
				fSubjectControl.addControlListener(this);
				fSubjectControl.addKeyListener(this);
			}
		}
		
		/*
		 * @see IInformationControlCloser#stop()
		 */
		public void stop() {
			stop(false);
		}
		
		/**
		 * Stops the information control and if <code>delayRestart</code> is set
		 * allows restart only after a certain delay.
		 * 
		 * @param delayRestart <code>true</code> if restart should be delayed
		 */
		protected void stop(boolean delayRestart) {
			
			if (!fIsActive)
				return;
			fIsActive= false;
			
			hideInformationControl();
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeMouseListener(this);
				fSubjectControl.removeMouseMoveListener(this);
				fSubjectControl.removeMouseTrackListener(this);
				fSubjectControl.removeControlListener(this);
				fSubjectControl.removeKeyListener(this);
			}			
		}
		
		/*
		 * @see MouseMoveListener#mouseMove
		 */
		public void mouseMove(MouseEvent event) {
			if (!fSubjectArea.contains(event.x, event.y))
				stop();
		}
				
		/*
		 * @see MouseListener#mouseUp(MouseEvent)
		 */
		public void mouseUp(MouseEvent event) {
		}
		
		/*
		 * @see MouseListener#mouseDown(MouseEvent)
		 */
		public void mouseDown(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see MouseListener#mouseDoubleClick(MouseEvent)
		 */
		public void mouseDoubleClick(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see MouseTrackAdapter#mouseExit(MouseEvent)
		 */
		public void mouseExit(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see ControlListener#controlResized(ControlEvent)
		 */
		public void controlResized(ControlEvent event) {
			stop();
		}
		
		/*
		 * @see ControlListener#controlMoved(ControlEvent)
		 */
		public void controlMoved(ControlEvent event) {
			stop();
		}
		
		/*
		 * @see KeyListener#keyReleased(KeyEvent)
		 */
		public void keyReleased(KeyEvent event) {
		}
		
		/*
		 * @see KeyListener#keyPressed(KeyEvent)
		 */
		public void keyPressed(KeyEvent event) {
			stop(true);
		}
	};
	
	
	/**
	 * To be installed on the manager's subject control.  Serves two different purposes:
	 * <ul>
	 * <li> start function: initiates the computation of the information to be presented. This happens on 
	 * 		receipt of a mouse hover event and disables the information control manager,
	 * <li> restart function: tracks mouse move and shell activation event to determine when the information
	 * 		control manager needs to be reactivated.
	 * </ul>
	 */
	class MouseTracker extends ShellAdapter implements MouseTrackListener, MouseMoveListener {
		
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=18393
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19686
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19719
		
		/** Margin around the original hover event location for coputing the hover area. */
		private final static int EPSILON= 3;
		
		/** The area in which the original hover event occurred. */
		private Rectangle fHoverArea;
		/** The area for which is computed information is valid. */
		private Rectangle fSubjectArea;
		/** The tracker's subject control. */
		private Control fSubjectControl;
		
		/** Indicates whether the tracker is computing the start function. */
		private boolean fIsActive= false;
		/** Indicates whether the mouse has been lost. */
		private boolean fMouseLost= false;
		/** Indicates whether the subject control's shell has been deactivated. */
		private boolean fShellDeactivated= false;
		
		/**
		 * Creates a new mouse tracker.
		 */
		public MouseTracker() {
		}
		
		/**
		 * Sets this mouse tracker's subject area, the area to be tracked in order
		 * to reenable the information control manager.
		 * 
		 * @param subjectArea the subject area
		 */
		public void setSubjectArea(Rectangle subjectArea) {
			Assert.isNotNull(subjectArea);
			fSubjectArea= subjectArea;
		}
		
		/**
		 * Starts this mouse tracker. The given control becomes this tracker's subject control.
		 * Installs itself as mouse track listener on the subject control.
		 * 
		 * @param subjectControl the subject control
		 */
		public void start(Control subjectControl) {
			fSubjectControl= subjectControl;
			if (fSubjectControl != null && !fSubjectControl.isDisposed())
				fSubjectControl.addMouseTrackListener(this);
				
			fIsActive= false;
			fMouseLost= false;
			fShellDeactivated= false;
		}
		
		/**
		 * Stops this mouse tracker. Removes itself  as mouse track, mouse move, and
		 * shell listener from the subject control.
		 */
		public void stop() {
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeMouseTrackListener(this);
				fSubjectControl.removeMouseMoveListener(this);
				fSubjectControl.getShell().removeShellListener(this);
			}
			
			fMouseLost= false;
			fShellDeactivated= false;
		}
		
		/**
		 * Initiates the computation of the information to be presented. Sets the initial hover area
		 * to a small rectangle around the hover event location. Adds mouse move and shell activation listeners
		 * to track whether the computed information is, after completion, useful for presentation and to
		 * implement the restart function.
		 */
		public void mouseHover(MouseEvent event) {
			
			if (fIsActive)
				return;
			
			fIsActive= true;
			fMouseLost= false;
			fShellDeactivated= false;
			
			setEnabled(false);

			fHoverEventStateMask= event.stateMask;
			fHoverEventLocation= new Point(event.x, event.y);
			fHoverArea= new Rectangle(event.x - EPSILON, event.y - EPSILON, 2 * EPSILON, 2 * EPSILON );
			if (fHoverArea.x < 0) fHoverArea.x= 0;
			if (fHoverArea.y < 0) fHoverArea.y= 0;
			setSubjectArea(fHoverArea);
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.addMouseMoveListener(this);
				fSubjectControl.getShell().addShellListener(this);
			}
			doShowInformation();
		}
		
		/**
		 * Deactivates this tracker's restart function and enables the information control
		 * manager. Does not have any effect if the tracker is still executing the start function (i.e. 
		 * computing the information to be presented.
		 */
		protected void deactivate() {
			if (fIsActive)
				return;
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeMouseMoveListener(this);
				fSubjectControl.getShell().removeShellListener(this);
			}
			
			setEnabled(true);
		}
		
		/*
		 * @see MouseTrackListener#mouseEnter(MouseEvent)
		 */
		public void mouseEnter(MouseEvent e) {
		}
		
		/*
		 * @see MouseTrackListener#mouseExit(MouseEvent)
		 */
		public void mouseExit(MouseEvent e) {
			fMouseLost= true;
			deactivate();
		}
		
		/*
		 * @see MouseMoveListener#mouseMove(MouseEvent)
		 */
		public void mouseMove(MouseEvent event) {
			if (!fSubjectArea.contains(event.x, event.y))
				deactivate();
		}

		/*
		 * @see ShellListener#shellDeactivated(ShellEvent)
		 */
		public void shellDeactivated(ShellEvent e) {
			fShellDeactivated= true;
			deactivate();
		}
		
		/*
		 * @see ShellListener#shellIconified(ShellEvent)
		 */
		public void shellIconified(ShellEvent e) {
			fShellDeactivated= true;
			deactivate();
		}
		
		/**
		 * Tells this tracker that the start function processing has been completed.
		 */
		public void computationCompleted() {
			fIsActive= false;
			fMouseLost= false;
			fShellDeactivated= false;
		}
		
		/**
		 * Determines whether the computed information is still useful for presentation.
		 * This is the case, if the shell of the subject control has been deactivated, the mouse
		 * left the subject control, or the mouse moved on, so that it is no longer in the subject
		 * area.
		 * 
		 * @return <code>true</code> if information is still useful for presentation, <code>false</code> otherwise
		 */
		public boolean isMouseLost() {
			
			if (fMouseLost || fShellDeactivated)
				return true;
								
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				Display display= fSubjectControl.getDisplay();
				Point p= display.getCursorLocation();
				p= fSubjectControl.toControl(p);
				if (!fSubjectArea.contains(p) && !fHoverArea.contains(p))
					return true;
			}
			
			return false;
		}
	};
		
	/** The mouse tracker on the subject control */
	private MouseTracker fMouseTracker= new MouseTracker();
	/** The remembered hover event location */
	private Point fHoverEventLocation= new Point(-1, -1);
	/** The remembered hover event sate mask of the keyboard modifiers */
	private int fHoverEventStateMask= 0;
	
	/**
	 * Creates a new hover information control manager using the given information control creator.
	 * By default a <code>Closer</code> instance is set as this manager's closer.
	 *
	 * @param creator the information control creator
	 */
	protected AbstractHoverInformationControlManager(IInformationControlCreator creator) {
		super(creator);
		setCloser(new Closer());
	}
	
	/*
	 * @see AbstractInformationControlManager#presentInformation()
	 */
	protected void presentInformation() {
		if (fMouseTracker == null) {
			super.presentInformation();
			return;
		}
		
		Rectangle area= getSubjectArea();
		if (area != null)
			fMouseTracker.setSubjectArea(area);
		
		if (fMouseTracker.isMouseLost()) {
			fMouseTracker.computationCompleted();
			fMouseTracker.deactivate();
		} else {
			fMouseTracker.computationCompleted();
			super.presentInformation();
		}
	}
	
	/*
	 * @see AbstractInformationControlManager#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		
		boolean was= isEnabled();
		super.setEnabled(enabled);
		boolean is= isEnabled();
		
		if (was != is && fMouseTracker != null) {
			if (is)
				fMouseTracker.start(getSubjectControl());
			else
				fMouseTracker.stop();
		}
	}


	/**
	 * Disposes this manager's information control.
	 */
	public void dispose() {
		if (fMouseTracker != null) {
			fMouseTracker.stop();
			fMouseTracker.fSubjectControl= null;
			fMouseTracker= null;
		}
		super.dispose();
	}

	/**
	 * Returns the location at which the most recent mouse hover event
	 * has been issued.
	 * 
	 * @return the location of the most recent mouse hover event
	 */
	protected Point getHoverEventLocation() {
		return fHoverEventLocation;
	}

 	/**
	 * Returns the SWT event state of the most recent mouse hover event.
	 */
	protected int getHoverEventStateMask() {
		return fHoverEventStateMask;
 	}

}