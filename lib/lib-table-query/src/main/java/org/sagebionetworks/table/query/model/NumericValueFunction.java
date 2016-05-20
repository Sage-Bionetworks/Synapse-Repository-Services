package org.sagebionetworks.table.query.model;

import java.util.List;


public class NumericValueFunction extends SQLElement implements HasAggregate, HasFunctionType {

	private MysqlFunction mysqlFunction;

	public NumericValueFunction(MysqlFunction mysqlFunction) {
		this.mysqlFunction = mysqlFunction;
	}

	public MysqlFunction getMysqlFunction() {
		return mysqlFunction;
	}
	
	@Override
	public void toSql(StringBuilder builder) {
		builder.append(mysqlFunction.name());
		builder.append("()");
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
		switch (mysqlFunction) {
		case FOUND_ROWS:
			return FunctionType.FOUND_ROWS;
		default:
			throw new IllegalArgumentException("unexpected mysqlFuntion");
		}
	}
}
