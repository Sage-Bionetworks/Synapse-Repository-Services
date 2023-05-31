package org.sagebionetworks.table.query.model;

/**
 * <cast operand> ::= {@link NullableValueExpression}
 *
 */
public class CastOperand extends SimpleBranch {

	public CastOperand(NullableValueExpression child) {
		super(child);
	}

}
