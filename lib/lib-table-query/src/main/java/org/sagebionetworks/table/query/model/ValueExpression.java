package org.sagebionetworks.table.query.model;

/**
 * ValueExpression ::= {@link NumericValueExpression} | {@link EntityId}
 */
public class ValueExpression extends SimpleBranch {

	public ValueExpression(NumericValueExpression numericValueExpression) {
		super(numericValueExpression);
	}
	
	public ValueExpression(EntityId entityId) {
		super(entityId);
	}

}
