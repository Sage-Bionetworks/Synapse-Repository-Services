package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.visitors.ColumnTypeVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


public class NumericValueFunction extends SQLElement implements HasAggregate, HasFunctionType {

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

	public void visit(ColumnTypeVisitor visitor) {
		switch (mysqlFunction) {
		case FOUND_ROWS:
			visitor.setColumnType(ColumnType.INTEGER);
			break;
		default:
			throw new IllegalArgumentException("unexpected mysqlFuntion");
		}
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
