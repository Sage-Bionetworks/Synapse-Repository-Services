package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcomp op&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public enum CompOp {
	
	EQUALS_OPERATOR("="),
	NOT_EQUALS_OPERATOR("<>"),
	LESS_THAN_OPERATOR("<"),
	GREATER_THAN_OPERATOR(">"),
	LESS_THAN_OR_EQUALS_OPERATOR("<="),
	GREATER_THAN_OR_EQUALS_OPERATOR(">=");
	
	String sql;
	
	CompOp(String sql){
		this.sql = sql;
	}
	
	public String toSQL(){
		return this.sql;
	}

}
