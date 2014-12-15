package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;


public class NumericPrimary extends SQLElement {

	private ValueExpressionPrimary valueExpressionPrimary;
	private NumericValueFunction numericValueFunction;

	public NumericPrimary(ValueExpressionPrimary valueExpressionPrimary) {
		this.valueExpressionPrimary = valueExpressionPrimary;
	}

	public NumericPrimary(NumericValueFunction numericValueFunction) {
		this.numericValueFunction = numericValueFunction;
	}

	public NumericValueFunction getNumericValueFunction() {
		return numericValueFunction;
	}

	public ValueExpressionPrimary getValueExpressionPrimary() {
		return valueExpressionPrimary;
	}

	@Override
	public void visit(Visitor visitor) {
		if (valueExpressionPrimary != null) {
			visit(valueExpressionPrimary, visitor);
		} else {
			visit(numericValueFunction, visitor);
		}
	}
}
