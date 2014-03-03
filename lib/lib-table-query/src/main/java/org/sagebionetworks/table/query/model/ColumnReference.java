package org.sagebionetworks.table.query.model;


/**
 * This matches &ltcolumn reference&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ColumnReference {
	
	Qualifier qualifier;
	String columnName;
	public ColumnReference(Qualifier qualifier, String columnName) {
		super();
		this.qualifier = qualifier;
		this.columnName = columnName;
	}
	public Qualifier getQualifier() {
		return qualifier;
	}
	public String getColumnName() {
		return columnName;
	}	
	
}
