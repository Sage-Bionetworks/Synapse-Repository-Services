package org.sagebionetworks.repo.model.grid.node;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface Node {

	/**
	 * The node's ID.
	 * 
	 * @return
	 */
	LogicalTimestamp getId();

}
