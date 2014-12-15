package org.sagebionetworks.table.query.model.visitors;

import org.sagebionetworks.table.query.model.ColumnReference;


public class ToSimpleSqlVisitor implements Visitor {
	public enum SQLClause {
		SELECT, ORDER_BY, GROUP_BY, FUNCTION_PARAMETER
	};

	private StringBuilder builder = new StringBuilder(300);

	public StringBuilder getBuilder() {
		return builder;
	}

	public void append(String str) {
		builder.append(str);
	}

	public String getSql() {
		return builder.toString();
	}

	public void pushCurrentClause(SQLClause functionParameter) {
		// not used here
	}

	public void popCurrentClause(SQLClause functionParameter) {
		// not used here
	}

	public void setLHSColumn(ColumnReference columnReferenceLHS) {
		// not used here
	}
}
