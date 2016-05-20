package org.sagebionetworks.table.query.model;

import java.util.List;


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
	public void toSql(StringBuilder builder) {
		if (valueExpressionPrimary != null) {
			valueExpressionPrimary.toSql(builder);
		} else {
			numericValueFunction.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, valueExpressionPrimary);
		checkElement(elements, type, numericValueFunction);
	}
}
