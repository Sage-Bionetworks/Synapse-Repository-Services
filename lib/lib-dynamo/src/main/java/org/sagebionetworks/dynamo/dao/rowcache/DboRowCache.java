package org.sagebionetworks.dynamo.dao.rowcache;

import org.sagebionetworks.dynamo.DynamoTable;
import org.sagebionetworks.dynamo.KeyValueSplitter;
import org.sagebionetworks.dynamo.config.DynamoBehavior;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBTable;

/**
 * Maps directly to the DynamoDB table named NodeLineage.
 */
@DynamoDBTable(tableName = DboRowCache.TABLE_NAME)
// we allow eventual consistency (at most, the cache will appear not uptodate and an extra update will be issued)
// we set update to clobber, since we don't need to version our writes.
@DynamoBehavior(consistentReads = ConsistentReads.EVENTUAL, saveBehavior = SaveBehavior.CLOBBER)
public class DboRowCache implements DynamoTable {

	public static final String TABLE_NAME = "RowCache";
	public static final String HASH_KEY_NAME = "table";
	public static final String RANGE_KEY_NAME = "row" + KeyValueSplitter.SEPARATOR + "version";

	private String hashKey;
	private String rangeKey;
	private byte[] value = null;

	public static String createHashKey(final String tableId) {
		return tableId;
	}

	public static String createRangeKey(final Long rowId, final Long versionNumber) {
		return KeyValueSplitter.createKey(rowId, versionNumber);
	}

	@DynamoDBHashKey(attributeName = DboRowCache.HASH_KEY_NAME)
	public String getHashKey() {
		return this.hashKey;
	}

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}

	@DynamoDBRangeKey(attributeName = DboRowCache.RANGE_KEY_NAME)
	public String getRangeKey() {
		return this.rangeKey;
	}

	public void setRangeKey(String rangeKey) {
		this.rangeKey = rangeKey;
	}

	@DynamoDBAttribute(attributeName = "value")
	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}

	@DynamoDBIgnore
	public Long getRowId() {
		return Long.valueOf(KeyValueSplitter.split(rangeKey)[0]);
	}

	@DynamoDBIgnore
	public Long getVersionNumber() {
		return Long.valueOf(KeyValueSplitter.split(rangeKey)[1]);
	}

	@Override
	public String toString() {
		return "DboRowCache [hashKey=" + hashKey + ", rangeKey=" + rangeKey + "]";
	}
}
