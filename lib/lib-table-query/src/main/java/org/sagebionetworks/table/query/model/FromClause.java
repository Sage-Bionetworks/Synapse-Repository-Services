package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltfrom clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class FromClause extends SQLElement {

	private TableReference tableReference;

	public FromClause(TableReference tableReference) {
		super();
		this.tableReference = tableReference;
	}

	public TableReference getTableReference() {
		return tableReference;
	}

	public void visit(Visitor visitor) {
		visit(tableReference, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append("FROM ");
		visit(tableReference, visitor);
	}
}
