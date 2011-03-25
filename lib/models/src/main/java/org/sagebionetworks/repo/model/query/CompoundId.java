package org.sagebionetworks.repo.model.query;

/**
 * A compound id can include a table name and a field name.
 * 
 * @author jmhill
 *
 */
public class CompoundId {
	String tableName;
	String fieldName;
	public CompoundId(String tableName, String fieldName) {
		super();
		this.tableName = tableName;
		this.fieldName = fieldName;
	}
	public String getTableName() {
		return tableName;
	}
	public String getFieldName() {
		return fieldName;
	}
	
}