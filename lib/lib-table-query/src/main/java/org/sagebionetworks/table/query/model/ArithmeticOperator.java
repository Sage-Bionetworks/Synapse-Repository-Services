package org.sagebionetworks.table.query.model;

/**
 * The four arithmetic operators.
 *
 */
public enum ArithmeticOperator {

	ASTERISK("*"),
	SOLIDUS("/"),
	DIV(" DIV "),
	PLUS_SIGN("+"),
	MINUS_SIGN("-"),
	MODULO("%");
	
	ArithmeticOperator(String sql){
		this.sql = sql;
	}
	String sql;
	
	public String toSQL(){
		return this.sql;
	}
}
