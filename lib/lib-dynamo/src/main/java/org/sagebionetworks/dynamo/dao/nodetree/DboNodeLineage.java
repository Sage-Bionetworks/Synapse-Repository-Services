package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;
import java.util.Locale;

import org.sagebionetworks.dynamo.DynamoTable;
import org.sagebionetworks.dynamo.KeyValueSplitter;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBVersionAttribute;

/**
 * Maps directly to the DynamoDB table named NodeLineage.
 */
@DynamoDBTable(tableName=DboNodeLineage.TABLE_NAME)
public class DboNodeLineage implements DynamoTable {

	public static final String TABLE_NAME = "NodeLineage";
	public static final String HASH_KEY_NAME = "NodeId" + KeyValueSplitter.SEPARATOR + "LineageType";
	public static final String RANGE_KEY_NAME = "Distance" + KeyValueSplitter.SEPARATOR + "NodeId";

	/**
	 * This is the root pointer to help locate the root node. This virtual node should not be linked
	 * with any other node except having the root as its direct child.
	 */
	static final String ROOT = "ROOT";
	static final String ROOT_HASH_KEY = DboNodeLineage.createHashKey(ROOT, LineageType.DESCENDANT);

	/**
	 * Maximum depth allowed on the tree
	 */
	static final int MAX_DEPTH = 100;
	private static final int NUM_DIGITS = (int)Math.log10(MAX_DEPTH);

	/**
	 * Creates the composite hash key from node ID and lineage type. Example hash key, "382739#A".
	 */
	static String createHashKey(final String nodeId, final LineageType lineageType) {
		if (nodeId == null) {
			throw new NullPointerException();
		}
		if (lineageType == null) {
			throw new NullPointerException();
		}
		return nodeId + KeyValueSplitter.SEPARATOR + lineageType.getType();
	}

	/**
	 * Creates the composite range key from distance and node ID. Example range key, "005#95373".
	 */
	static String createRangeKey(final int distance, final String ancOrDescId) {
		if (distance < 0) {
			throw new IllegalArgumentException("Distance must be at least 0.");
		}
		if (ancOrDescId == null) {
			throw new NullPointerException();
		}
		// Pad left with zeros so that distance can be sorted correctly as strings by DynamoDB.
		// We shouldn't have nodes that are more than max-depth deep
		String format = "%0" + NUM_DIGITS + "d";
		return String.format(Locale.ROOT, format, distance) + KeyValueSplitter.SEPARATOR + ancOrDescId;
	}

	@DynamoDBHashKey(attributeName=DboNodeLineage.HASH_KEY_NAME)
	public String getHashKey() {
		return this.nodeIdLineageType;
	}
	public void setHashKey(String nodeIdLineageType) {
		this.nodeIdLineageType = nodeIdLineageType;
	}

	@DynamoDBRangeKey(attributeName=DboNodeLineage.RANGE_KEY_NAME)
	public String getRangeKey() {
		return this.distanceNodeId;
	}
	public void setRangeKey(String distanceNodeId) {
		this.distanceNodeId = distanceNodeId;
	}

	@DynamoDBVersionAttribute
	public Long getVersion() {
		return this.version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}

	@DynamoDBAttribute
	public Date getTimestamp() {
		return this.timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@DynamoDBIgnore
	@Override
	public String toString() {
		return "DboNodeLineage [nodeIdLineageType=" + nodeIdLineageType
				+ ", distanceNodeId=" + distanceNodeId + ", version=" + version
				+ ", timestamp=" + timestamp + "]";
	}

	private String nodeIdLineageType;
	private String distanceNodeId;
	private Long version;
	private Date timestamp;
}
