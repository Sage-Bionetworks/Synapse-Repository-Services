package org.sagebionetworks.table.query.model;

/**
 * <cast operand> ::= {@link NullableValueExpression}
 *
 */
public class CastOperand extends SimpleBranch {

	public CastOperand(ValueExpression child) {
		super(child);
	}

}
