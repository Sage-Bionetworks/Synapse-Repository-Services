package org.sagebionetworks.table.query.model.visitors;


public class GetTableNameVisitor implements Visitor {
	private String tableName = null;

	public void setTableName(String tableName) {
		if (this.tableName != null) {
			throw new IllegalArgumentException("Table name already set");
		}
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}
}
