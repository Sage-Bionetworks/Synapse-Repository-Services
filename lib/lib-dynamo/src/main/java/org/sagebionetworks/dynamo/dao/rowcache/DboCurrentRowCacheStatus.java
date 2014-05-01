package org.sagebionetworks.dynamo.dao.rowcache;

import org.sagebionetworks.dynamo.DynamoTable;
import org.sagebionetworks.dynamo.config.DynamoBehavior;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBVersionAttribute;

@DynamoDBTable(tableName = DboCurrentRowCacheStatus.TABLE_NAME)
// we can't allow eventual consistency
// we set update to update, since we version our writes.
@DynamoBehavior(consistentReads = ConsistentReads.CONSISTENT, saveBehavior = SaveBehavior.UPDATE)
public class DboCurrentRowCacheStatus implements DynamoTable {

	public static final String TABLE_NAME = "CurrentRowCacheStatus";
	public static final String HASH_KEY_NAME = "table";

	private String hashKey;
	private Long latestVersionNumber = null;
	private Long recordVersion = null;

	/**
	 * Creates the composite hash key
	 */
	public static String createHashKey(final String tableId) {
		return tableId;
	}

	@DynamoDBHashKey(attributeName = DboCurrentRowCacheStatus.HASH_KEY_NAME)
	public String getHashKey() {
		return this.hashKey;
	}

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}

	@DynamoDBAttribute(attributeName = "latestVersionNumber")
	public Long getLatestVersionNumber() {
		return latestVersionNumber;
	}

	public void setLatestVersionNumber(Long latestVersionNumber) {
		this.latestVersionNumber = latestVersionNumber;
	}

	@DynamoDBVersionAttribute
	public Long getRecordVersion() {
		return recordVersion;
	}

	public void setRecordVersion(Long recordVersion) {
		this.recordVersion = recordVersion;
	}

	@Override
	public String toString() {
		return "DboCurrentRowCacheStatus [hashKey=" + hashKey + ", latestVersionNumber=" + latestVersionNumber + ", recordVersion="
				+ recordVersion
				+ "]";
	}
}
