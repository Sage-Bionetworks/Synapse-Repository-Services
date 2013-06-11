package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.List;

/**
 * When a node has more than one parent.
 */
public class MultipleParentsException extends MultipleInheritanceException {

	private static final long serialVersionUID = 7868686309825057415L;

	public MultipleParentsException(String msg, String node, List<NodeLineage> parents) {
		super(msg, node, 1, parents);
	}
}
