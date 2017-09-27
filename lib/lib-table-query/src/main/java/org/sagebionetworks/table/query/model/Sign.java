package org.sagebionetworks.table.query.model;

/**
 * 
 * plus (+) or minus (-)
 *
 */
public enum Sign {
	
	PLUS("+"),
	MINUS("-");
	
	String sql;
	
	Sign(String sql){
		this.sql = sql;
	}
	
	public String toSQL(){
		return this.sql;
	}

}
