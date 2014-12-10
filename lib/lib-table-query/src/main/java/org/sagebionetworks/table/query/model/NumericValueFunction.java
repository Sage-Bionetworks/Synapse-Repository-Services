package org.sagebionetworks.table.query.model;


public class NumericValueFunction extends SQLElement {

	private MysqlFunction mysqlFunction;

	public NumericValueFunction(MysqlFunction mysqlFunction) {
		this.mysqlFunction = mysqlFunction;
	}

	public MysqlFunction getMysqlFunction() {
		return mysqlFunction;
	}

	public void visit(Visitor visitor) {
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append(mysqlFunction.name());
		visitor.append("()");
	}
}
