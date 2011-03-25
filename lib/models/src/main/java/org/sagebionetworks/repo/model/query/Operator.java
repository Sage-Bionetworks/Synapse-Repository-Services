package org.sagebionetworks.repo.model.query;

/**
 * The types of operators we currently support in a search condition.
 * 
 * @author jmhill
 *
 */
public enum Operator {
	AND("&&"),
	OR("||");
	
	String jdoString;
	Operator(String jdoString){
		this.jdoString = jdoString;
	}
	
	public String toJdoString() {
		return jdoString;
	}
}