package org.sagebionetworks.table.query.model;

/**
 * This matches &ltqualifier&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Qualifier {
	
	String tableName;
	String correlationName;
	public Qualifier(String tableName, String correlationName) {
		super();
		this.tableName = tableName;
		this.correlationName = correlationName;
	}

}
