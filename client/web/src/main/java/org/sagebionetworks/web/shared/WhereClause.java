package org.sagebionetworks.web.shared;

public class WhereClause {
	
	public enum Operator{
		
		EQUALS("==");
		
		String sqlString;
		Operator(String sqlString){
			this.sqlString = sqlString;
		};
	}

	
}
