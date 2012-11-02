package org.sagebionetworks.dynamo.dao;

import java.util.Date;

class NodeLineagePair {

	NodeLineagePair(String ancestorId, Date ancTimestamp, String descendantId,
			Date descTimestamp, int ancestorDepth, int distance) {

		if (ancestorId == null) {
			throw new NullPointerException();
		}
		if (descendantId == null) {
			throw new NullPointerException();
		}

		if (ancTimestamp == null) {
			throw new NullPointerException();
		}
		if (descTimestamp == null) {
			throw new NullPointerException();
		}

		Date now = new Date();
		if (now.before(ancTimestamp)) {
			throw new IllegalArgumentException("Ancestor timestamp " + ancTimestamp + " is after present time.");
		}
		if (now.before(descTimestamp)) {
			throw new IllegalArgumentException("Descendant timestamp " + descTimestamp + " is after present time.");
		}

		if (ancestorDepth < 0) {
			throw new IllegalArgumentException("Ancestor depth must be at least 0.");
		}
		if (distance < 0) {
			throw new IllegalArgumentException("Distance must be at least 0.");
		}

		if (distance == 0) {
			if (!ancestorId.equals(descendantId)) {
				throw new IllegalArgumentException(
						"Root must have the same ancestor and descendant and the distance must be 0.");
			}
		} else if (ancestorId.equals(descendantId)) {
			throw new IllegalArgumentException(
					"Root must have the same ancestor and descendant and the distance must be 0.");
		}

		this.ancId = ancestorId;
		this.descId = descendantId;

		if (distance == 0) {
			this.a2d = null;
		} else {
			this.a2d = new NodeLineage(ancestorId, descendantId, LineageType.DESCENDANT, distance, ancTimestamp);
		}
		this.d2a = new NodeLineage(descendantId, ancestorId, LineageType.ANCESTOR, distance, descTimestamp);

		this.depth = ancestorDepth;
		this.distance = distance;
	}

	NodeLineage getAncestor2Descendant() {
		return this.a2d;
	}

	NodeLineage getDescendant2Ancestor() {
		return this.d2a;
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

	private final NodeLineage a2d;
	private final NodeLineage d2a;
	private final int depth;
	private final int distance;
	private final String ancId;
	private final String descId;
}
