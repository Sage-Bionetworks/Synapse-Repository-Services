package org.sagebionetworks.table.query.model;

/**
 * This matches &ltfrom clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class FromClause {

	private TableReference tableReference;

	public FromClause(TableReference tableReference) {
		super();
		this.tableReference = tableReference;
	}

	public TableReference getTableReference() {
		return tableReference;
	}
	
	
}
