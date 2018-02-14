package org.sagebionetworks.repo.model.dbo.migration;

public class ChecksumTableResult {
	
	private String tableName;
	private String checksum;
	
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getChecksum() {
		return checksum;
	}
	public void setChecksum(String value) {
		this.checksum = value;
	}

	
}
