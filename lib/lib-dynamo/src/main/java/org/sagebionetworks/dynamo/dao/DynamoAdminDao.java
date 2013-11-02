package org.sagebionetworks.dynamo.dao;

public interface DynamoAdminDao {

	/**
	 * Deletes all the items in the specified table.
	 */
	void clear(String tableName, String hashKeyName, String rangeKeyName);
	
	/**
	 * Are Dyanmo related features enabled?
	 */
	boolean isDynamoEnabled();
}
