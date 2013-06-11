package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.List;

/**
 * When a node has more than one ancestor of a particular generation. One special
 * case is a child that has more than one parent.
 */
public class MultipleInheritanceException extends RuntimeException {

	private static final long serialVersionUID = -72309234033802731L;

	private String node;
	private final int distance;
	private final List<NodeLineage> ancestors;

	public MultipleInheritanceException(String msg, String node, int distance, List<NodeLineage> ancestors) {
		super(msg);
		this.node = node;
		this.distance = distance;
		this.ancestors = ancestors;
	}

	/**
	 * The node that generates this exception.
	 */
	public String getNode() {
		return node;
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
