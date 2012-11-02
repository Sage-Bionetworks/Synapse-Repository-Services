package org.sagebionetworks.dynamo.dao;

import java.util.Date;

import org.sagebionetworks.dynamo.DynamoTable;
import org.sagebionetworks.dynamo.KeyValueSplitter;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBVersionAttribute;

/**
 * Lineage between two nodes, where one is the ancestor and the other is the descendant.
 *
 * @author Eric Wu
 */
@DynamoDBTable(tableName=NodeLineage.TABLE_NAME)
public class NodeLineage implements DynamoTable {

	public static final String TABLE_NAME = "NodeLineage";
	public static final String HASH_KEY = "NodeId" + KeyValueSplitter.SEPARATOR + "LineageType";
	public static final String RANGE_KEY = "Distance" + KeyValueSplitter.SEPARATOR + "NodeId";

	// Needed by Dynamo mapper to load from DynamoDB
	public NodeLineage() {}

	@DynamoDBHashKey(attributeName=NodeLineage.HASH_KEY)
	public String getHashKey() {
		return this.nodeIdLineageType;
	}
	public void setHashKey(String nodeIdLineageType) {
		this.nodeIdLineageType = nodeIdLineageType;
	}

	@DynamoDBRangeKey(attributeName=NodeLineage.RANGE_KEY)
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

	/**
	 * For the root node, make the ancestor/descendant ID the same as this node's ID and
	 * lineage type must be ancestor.
	 */
	NodeLineage(String nodeId, String ancOrDescId, LineageType lineageType, int distance, Date timestamp) {

		if (nodeId == null) {
			throw new NullPointerException();
		}
		if (ancOrDescId == null) {
			throw new NullPointerException();
		}
		if (lineageType == null) {
			throw new NullPointerException();
		}
		if (distance < 0) {
			throw new IllegalArgumentException("Distance cannot be negative. Distance is " + distance);
		}
		if (nodeId.equals(ancOrDescId) && !LineageType.ANCESTOR.equals(lineageType)) {
			throw new IllegalArgumentException("Root must use the ancestor lineage type.");
		}
		if (nodeId.equals(ancOrDescId) && distance != 0) {
			throw new IllegalArgumentException("Root must have 0 distance to itself.");
		}
		if (timestamp == null) {
			throw new NullPointerException();
		}

		this.nodeIdLineageType = nodeId + KeyValueSplitter.SEPARATOR + lineageType.getType();
		this.distanceNodeId = distance + KeyValueSplitter.SEPARATOR + ancOrDescId;
		this.timestamp = timestamp;
	}

	/**
	 * The ID of node U in the lineage tuple (U, V).
	 */
	@DynamoDBIgnore
	String getNodeId() {
		if (this.nodeIdLineageType == null) {
			throw new NullPointerException();
		}
		String splits[] = KeyValueSplitter.split(this.nodeIdLineageType);
		return splits[0];
	}

	/**
	 * The type of lineage, currently either ancestor or descendant.
	 * In the lineage tuple (U, V), if the type is descendant, then U is
	 * a descendant of V.
	 */
	@DynamoDBIgnore
	LineageType getLineageType() {
		if (this.nodeIdLineageType == null) {
			throw new NullPointerException();
		}
		String splits[] = KeyValueSplitter.split(this.nodeIdLineageType);
		LineageType type = LineageType.fromString(splits[1]);
		return type;
	}

	/**
	 * The ID of node V in the lineage tuple (U, V).
	 */
	@DynamoDBIgnore
	String getAncestorOrDescendantId() {
		if (this.distanceNodeId == null) {
			throw new NullPointerException();
		}
		String splits[] = KeyValueSplitter.split(this.distanceNodeId);
		return splits[1];
	}

	/**
	 * The distance of node V from node U in the node tuple (U, V).
	 * For example, if U is the parent of V, the distance is 1.
	 */
	@DynamoDBIgnore
	int getDistance() {
		if (this.distanceNodeId == null) {
			throw new NullPointerException();
		}
		String splits[] = KeyValueSplitter.split(this.distanceNodeId);
		int distance = Integer.parseInt(splits[0]);
		return distance;
	}

	private String nodeIdLineageType;
	private String distanceNodeId;
	private Long version;
	private Date timestamp;
}
