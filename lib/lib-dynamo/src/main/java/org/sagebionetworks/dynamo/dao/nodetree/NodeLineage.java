package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;

import org.sagebionetworks.dynamo.KeyValueSplitter;

/**
 * Defines lineage between two nodes, where one is the ancestor and the other is the descendant.
 * This mirrors {@link DboNodeLineage} but holds the discrete parts of the composite keys.
 */
class NodeLineage {

	/**
	 * Creates from the dbo.
	 */
	NodeLineage(DboNodeLineage dbo) {

		if (dbo == null) {
			throw new NullPointerException();
		}
		String hashKey = dbo.getHashKey();
		if (hashKey == null) {
			throw new IllegalArgumentException("Hash key not initialized.");
		}
		String rangeKey = dbo.getRangeKey();
		if (rangeKey == null) {
			throw new IllegalArgumentException("Range key not initialized.");
		}

		String[] splits = KeyValueSplitter.split(hashKey);
		this.nodeId = splits[0];
		this.lineageType = LineageType.fromString(splits[1]);

		splits = KeyValueSplitter.split(rangeKey);
		this.distance = Integer.parseInt(splits[0]);
		this.ancOrDescNodeId = splits[1];

		this.version = dbo.getVersion();
		this.timestamp = dbo.getTimestamp();
	}

	/**
	 * For the root node, pass in {@link DboNodeLineage#ROOT} as the ancestor.
	 */
	NodeLineage(String nodeId, LineageType lineageType, int distance, String ancOrDescId, Date timestamp) {
		this(nodeId, lineageType, distance, ancOrDescId, timestamp, null);
	}

	/**
	 * For the root node, pass in {@link DboNodeLineage#ROOT} as the ancestor.
	 */
	NodeLineage(String nodeId, LineageType lineageType, int distance, String ancOrDescId,
			Date timestamp, Long version) {

		if (nodeId == null) {
			throw new NullPointerException();
		}
		if (ancOrDescId == null) {
			throw new NullPointerException();
		}
		if (nodeId.equals(ancOrDescId)) {
			throw new IllegalArgumentException("Cannot have a self-lineage.");
		}
		if (lineageType == null) {
			throw new NullPointerException();
		}
		if (distance <= 0) {
			throw new IllegalArgumentException("Distance must be greater than 0. Distance is " + distance);
		}

		this.nodeId = nodeId;
		this.lineageType = lineageType;
		this.distance = distance;
		this.ancOrDescNodeId = ancOrDescId;
		this.timestamp = timestamp;
		this.version = version;
	}

	/**
	 * Creates the dbo.
	 */
	DboNodeLineage createDbo() {
		DboNodeLineage dbo = new DboNodeLineage(this.nodeId, this.lineageType, this.distance, this.ancOrDescNodeId, this.version,
				this.timestamp);
		return dbo;
	}

	/**
	 * The ID of node U in the lineage tuple (U, V).
	 */
	String getNodeId() {
		return this.nodeId;
	}

	/**
	 * The type of lineage, currently either ancestor or descendant.
	 * In the lineage tuple (U, V), if the type is descendant, then U is
	 * a descendant of V.
	 */
	LineageType getLineageType() {
		return this.lineageType;
	}

	/**
	 * The ID of node V in the lineage tuple (U, V).
	 */
	String getAncestorOrDescendantId() {
		return this.ancOrDescNodeId;
	}

	/**
	 * The distance of node V from node U in the node tuple (U, V).
	 * For example, if U is the parent of V, the distance is 1.
	 */
	int getDistance() {
		return this.distance;
	}

	/**
	 * The timestamp. Can be null.
	 * @return
	 */
	Date getTimestamp() {
		return this.timestamp;
	}

	/**
	 * The version, set by DynamoDB. Can be null for new object.
	 */
	Long getVersion() {
		return this.version;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NodeLineage [nodeId=").append(nodeId)
				.append(", lineageType=").append(lineageType)
				.append(", distance=").append(distance)
				.append(", ancOrDescNodeId=").append(ancOrDescNodeId)
				.append(", version=").append(version).append(", timestamp=")
				.append(timestamp).append("]");
		return builder.toString();
	}

	private final String nodeId;
	private final LineageType lineageType;
	private final int distance;
	private final String ancOrDescNodeId;
	private final Long version;
	private final Date timestamp;
}
