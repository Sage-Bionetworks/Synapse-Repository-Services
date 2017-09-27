package org.sagebionetworks.table.query.model;

import java.util.List;


public class NumericValueFunction extends SQLElement implements HasAggregate, HasFunctionType {
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {

	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this element does not contain any SQLElements
	}

	@Override
	public boolean isElementAggregate() {
		return true;
	}

	@Override
	public FunctionType getFunctionType() {
		throw new UnsupportedOperationException();
	}
}
