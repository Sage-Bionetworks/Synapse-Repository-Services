package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;

/**
 * Wraps a pair of lineage pointers between two nodes. One is the downward pointer
 * from ancestor to descendant, the other is the upward pointer from descendant
 * to ancestor.
 */
class NodeLineagePair {
	
	NodeLineagePair(DboNodeLineage dbo, int ancestorDepth) {
		this(new NodeLineage(dbo), ancestorDepth);
	}

	NodeLineagePair(NodeLineage lineage, int ancestorDepth) {

		if (lineage == null) {
			throw new NullPointerException();
		}
		if (ancestorDepth < 0) {
			throw new IllegalArgumentException("Depth cannot be negative.");
		}

		String ancId = lineage.getNodeId();
		String descId = lineage.getAncestorOrDescendantId();
		if (LineageType.ANCESTOR.equals(lineage.getLineageType())) {
			String id = ancId;
			ancId = descId;
			descId = id;
		}

		Date timestamp = lineage.getTimestamp();
		int distance = lineage.getDistance();
		Long version = lineage.getVersion();

		this.ancId = ancId;
		this.descId = descId;
		this.a2d = new NodeLineage(this.ancId, LineageType.DESCENDANT,
				distance, this.descId, timestamp, version);
		this.d2a = new NodeLineage(this.descId,  LineageType.ANCESTOR,
				distance, this.ancId, timestamp, version);
		this.depth = ancestorDepth;
		this.distance = distance;
	}

	/**
	 * Creates a lineage pair. For the root, pass in {@link DboNodeLineage#ROOT} as the ancestor.
	 */
	NodeLineagePair(String ancestorId, String descendantId,
			int ancestorDepth, int distance, Date timestamp) {

		if (ancestorId == null) {
			throw new NullPointerException();
		}
		if (descendantId == null) {
			throw new NullPointerException();
		}
		if (timestamp == null) {
			throw new NullPointerException();
		}

		Date now = new Date();
		if (now.before(timestamp)) {
			throw new IllegalArgumentException("Timestamp "+ timestamp
					+ " is after present time.");
		}

		if (ancestorDepth < 0) {
			throw new IllegalArgumentException("Ancestor depth must be at least 0.");
		}
		if (distance <= 0) {
			throw new IllegalArgumentException("Distance must be greater than 0.");
		}

		this.ancId = ancestorId;
		this.descId = descendantId;
		this.a2d = new NodeLineage(this.ancId, LineageType.DESCENDANT,
				distance, this.descId, timestamp);
		this.d2a = new NodeLineage(this.descId,  LineageType.ANCESTOR,
				distance, this.ancId, timestamp);
		this.depth = ancestorDepth;
		this.distance = distance;
	}

	DboNodeLineage getAncestor2Descendant() {
		return this.a2d.createDbo();
	}

	DboNodeLineage getDescendant2Ancestor() {
		return this.d2a.createDbo();
	}

	int getAncestorDepth() {
		return this.depth;
	}

	int getDistance() {
		return this.distance;
	}

	String getAncestorId() {
		return this.ancId;
	}

	String getDescendantId() {
		return this.descId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NodeLineagePair [ancId=").append(ancId)
				.append(", descId=").append(descId).append(", depth=")
				.append(depth).append(", distance=").append(distance)
				.append("]");
		return builder.toString();
	}

	private final NodeLineage a2d;
	private final NodeLineage d2a;
	private final String ancId;
	private final String descId;
	private final int depth;
	private final int distance;
}
