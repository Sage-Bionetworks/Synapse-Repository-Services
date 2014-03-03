package org.sagebionetworks.table.query.model;

/**
 * This matches &ltfrom clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class FromClause implements SQLElement {

	private TableReference tableReference;

	public FromClause(TableReference tableReference) {
		super();
		this.tableReference = tableReference;
	}

	public TableReference getTableReference() {
		return tableReference;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		builder.append("FROM ");
		tableReference.toSQL(builder);
	}
	
	
}
