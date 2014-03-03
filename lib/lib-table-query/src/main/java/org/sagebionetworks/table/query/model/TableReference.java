package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable reference&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableReference {

	String tableName;

	public String getTableName() {
		return tableName;
	}

	public TableReference(String tableName) {
		super();
		this.tableName = tableName;
	}
	
	
	
}
