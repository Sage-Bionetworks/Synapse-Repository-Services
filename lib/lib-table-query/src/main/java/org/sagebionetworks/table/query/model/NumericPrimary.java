package org.sagebionetworks.table.query.model;

/**
 * NumericPrimary ::= {@link ValueExpressionPrimary} | {@link NumericValueFunction} 
 */
public class NumericPrimary extends SimpleBranch {

	public NumericPrimary(ValueExpressionPrimary valueExpressionPrimary) {
		super(valueExpressionPrimary);
	}

	public NumericPrimary(NumericValueFunction numericValueFunction) {
		super(numericValueFunction);
	}
}
