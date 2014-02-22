package org.sagebionetworks.table.query.model;

public class ColumnReference {
	
	Qualifier qualifier;
	String columnName;
	public ColumnReference(Qualifier qualifier, String columnName) {
		super();
		this.qualifier = qualifier;
		this.columnName = columnName;
	}
	
	
}
