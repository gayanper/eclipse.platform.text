/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jface.text.rules;

import org.eclipse.jface.text.Assert;


/**
 * An implementation of <code>IRule</code> capable of detecting a numerical value.
 */
public class NumberRule implements IRule {

	protected static final int UNDEFINED= -1;

	protected IToken fToken;
	protected int fColumn= UNDEFINED;

	/**
	 * Creates a rule which will return the specified
	 * token when a numerical sequence is detected.
	 *
	 * @param token the token to be returned
	 */
	public NumberRule(IToken token) {
		Assert.isNotNull(token);
		fToken= token;
	}

	/**
	 * Sets a column constraint for this rule. If set, the rule's token
	 * will only be returned if the pattern is detected starting at the 
	 * specified column. If the column is smaller then 0, the column
	 * constraint is considered removed.
	 *
	 * @param column the column in which the pattern starts
	 */
	public void setColumnConstraint(int column) {
		if (column < 0)
			column= UNDEFINED;
		fColumn= column;
	}

	/*
	 * @see IRule#evaluate
	 */
	public IToken evaluate(ICharacterScanner scanner) {
		int c= scanner.read();
		if (Character.isDigit((char)c)) {
			if (fColumn == UNDEFINED || (fColumn == scanner.getColumn() - 1)) {
				do {
					c= scanner.read();
				} while (Character.isDigit((char) c));
				scanner.unread();
				return fToken;
			}
		}
		
		scanner.unread();
		return Token.UNDEFINED;
	}
}