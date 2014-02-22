package org.sagebionetworks.table.query.model;

public class Qualifier {
	
	TableName tableName;
	String correlationName;
	public Qualifier(TableName tableName, String correlationName) {
		super();
		this.tableName = tableName;
		this.correlationName = correlationName;
	}

}
