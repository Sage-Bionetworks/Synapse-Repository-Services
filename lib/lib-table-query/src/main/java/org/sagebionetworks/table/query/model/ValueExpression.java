package org.sagebionetworks.table.query.model;

/**
 * ValueExpression ::= {@link NumericValueExpression}
 */
public class ValueExpression extends SimpleBranch {

	public ValueExpression(NumericValueExpression numericValueExpression) {
		super(numericValueExpression);
	}

}
