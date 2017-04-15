package org.sagebionetworks.repo.model.query;
/**
 * The types of compare operations that we currently support.
 * 
 * @author jmhill
 *
 */
public enum Comparator{
	EQUALS("="),
	NOT_EQUALS("!="),
	GREATER_THAN(">"),
	LESS_THAN("<"),
	GREATER_THAN_OR_EQUALS(">="),
	LESS_THAN_OR_EQUALS("<="),
	IN("IN");
	
	String sql;
	
	Comparator(String sql){
		this.sql = sql;
	}
	
	/**
	 * Get the SQL for this comparator
	 * @return
	 */
	public String getSql(){
		return this.sql;
	}
}