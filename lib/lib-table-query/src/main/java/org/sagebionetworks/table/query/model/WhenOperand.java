package org.sagebionetworks.table.query.model;

/**
 * {@link WhenOperand} ::= {@link ValueExpression}
 *
 */
public class WhenOperand extends SimpleBranch {

	public WhenOperand(ValueExpression child) {
		super(child);
	}

}
