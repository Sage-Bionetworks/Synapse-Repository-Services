package org.sagebionetworks.table.query.model;


public class NumericPrimary extends SQLElement {

	private ValueExpressionPrimary valueExpressionPrimary;
	private NumericValueFunction numericValueFunction;

	public NumericPrimary(ValueExpressionPrimary valueExpressionPrimary) {
		this.valueExpressionPrimary = valueExpressionPrimary;
	}

	public NumericPrimary(NumericValueFunction numericValueFunction) {
		this.numericValueFunction = numericValueFunction;
	}

	public boolean isAggregate() {
		if (valueExpressionPrimary != null) {
			return valueExpressionPrimary.isAggregate();
		} else {
			return false;
		}
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
