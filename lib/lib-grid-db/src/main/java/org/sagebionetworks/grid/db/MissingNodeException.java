package org.sagebionetworks.grid.db;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Thrown when a patch operation updates a node that does not exist in the
 * replica. The patch cannot be applied until the patch that creates the node
 * has been applied.
 */
public class MissingNodeException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	public MissingNodeException(String message, LogicalTimestamp nodeId) {
		super(message + ": " + nodeId);
	}
}
