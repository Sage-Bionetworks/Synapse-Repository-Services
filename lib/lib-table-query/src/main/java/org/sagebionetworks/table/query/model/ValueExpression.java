package org.sagebionetworks.table.query.model;

/**
 * ValueExpression ::= {@link NumericValueExpression} |
 * {@link StringValueExpression} | <datetime value expression> | <interval value
 * expression>
 */
public class ValueExpression extends SimpleBranch {

	public ValueExpression(StringValueExpression stringValueExpression) {
		super(stringValueExpression);
	}

	public ValueExpression(NumericValueExpression numericValueExpression) {
		super(numericValueExpression);
	}

}
