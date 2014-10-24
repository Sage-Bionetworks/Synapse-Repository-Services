package org.sagebionetworks.table.query.model;


public class NumericValueFunction extends SQLElement {

	private MysqlFunction mysqlFunction;

	public NumericValueFunction(MysqlFunction mysqlFunction) {
		this.mysqlFunction = mysqlFunction;
	}

	public MysqlFunction getMysqlFunction() {
		return mysqlFunction;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		builder.append(mysqlFunction.name()).append("()");
	}
}
