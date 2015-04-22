package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.visitors.ColumnTypeVisitor;
import org.sagebionetworks.table.query.model.visitors.IsAggregateVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


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

	public void visit(ColumnTypeVisitor visitor) {
		switch (mysqlFunction) {
		case FOUND_ROWS:
			visitor.setColumnType(ColumnType.INTEGER);
			break;
		default:
			throw new IllegalArgumentException("unexpected mysqlFuntion");
		}
	}

	public void visit(IsAggregateVisitor visitor) {
		visitor.setIsAggregate();
	}
}
