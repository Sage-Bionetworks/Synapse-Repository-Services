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

@DynamoDBTable(tableName = DboCurrentRowCache.TABLE_NAME)
// we can't allow eventual consistency
// we set update to clobber, since we don't version our writes.
@DynamoBehavior(consistentReads = ConsistentReads.CONSISTENT, saveBehavior = SaveBehavior.CLOBBER)
public class DboCurrentRowCache implements DynamoTable {

	public static final String TABLE_NAME = "CurrentRowCache";
	public static final String HASH_KEY_NAME = "table";
	public static final String RANGE_KEY_NAME = "row";

	public static final int AVERAGE_RECORD_SIZE = 20 + 10 + 10 + 3 * 4;

	private String hashKey;
	private Long rangeKey;
	private Long version = null;

	/**
	 * Creates the composite hash key
	 */
	public static String createHashKey(final Long tableId) {
		return tableId.toString();
	}

	/**
	 * Creates the composite range key
	 */
	public static Long createRangeKey(final Long rowId) {
		return rowId;
	}

	@DynamoDBHashKey(attributeName = DboCurrentRowCache.HASH_KEY_NAME)
	public String getHashKey() {
		return this.hashKey;
	}

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}

	@DynamoDBRangeKey(attributeName = DboCurrentRowCache.RANGE_KEY_NAME)
	public Long getRangeKey() {
		return this.rangeKey;
	}

	public void setRangeKey(Long rangeKey) {
		this.rangeKey = rangeKey;
	}

	@DynamoDBAttribute(attributeName = "version")
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@DynamoDBIgnore
	public Long getRowId() {
		return rangeKey;
	}

	@Override
	public String toString() {
		return "DboCurrentRowCache [hashKey=" + hashKey + ", rangeKey=" + rangeKey + ", version=" + version + "]";
	}
}
