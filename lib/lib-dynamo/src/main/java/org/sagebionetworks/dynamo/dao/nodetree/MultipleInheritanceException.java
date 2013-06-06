package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.List;

/**
 * When a node has more than one ancestor of a particular generation. One special
 * case is a child that has more than one parent.
 */
public class MultipleInheritanceException extends RuntimeException {

	private static final long serialVersionUID = -74309234033802731L;

	private final int distance;
	private final List<NodeLineage> ancestors;

	public MultipleInheritanceException(String msg, int distance, List<NodeLineage> ancestors) {
		super(msg);
		this.distance = distance;
		this.ancestors = ancestors;
	}

	/**
	 * The distance where more than one ancestor are seen.
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * The list of ancestors (multiple inheritance) at the distance.
	 */
	public List<NodeLineage> getAncestors() {
		return ancestors;
	}
}
