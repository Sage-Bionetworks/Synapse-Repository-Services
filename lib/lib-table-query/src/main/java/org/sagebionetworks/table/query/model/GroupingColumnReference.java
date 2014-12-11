package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltgrouping column reference&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class GroupingColumnReference extends SQLElement {

	ColumnReference columnReference;

	public GroupingColumnReference(ColumnReference columnReference) {
		this.columnReference = columnReference;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	public void visit(Visitor visitor) {
		visit(columnReference, visitor);
	}
}
