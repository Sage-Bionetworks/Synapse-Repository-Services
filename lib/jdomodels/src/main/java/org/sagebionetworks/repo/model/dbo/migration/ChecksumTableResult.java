package org.sagebionetworks.repo.model.dbo.migration;

import java.math.BigInteger;

public class ChecksumTableResult {
	
	private String tableName;
	private String value;
	
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	
}
