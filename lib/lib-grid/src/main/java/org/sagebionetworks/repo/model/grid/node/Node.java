package org.sagebionetworks.repo.model.grid.node;

import java.util.stream.Stream;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface Node {

	/**
	 * The node's ID.
	 *
	 * @return the logical timestamp identifying this node
	 */
	LogicalTimestamp getId();

	/**
	 * Returns a stream of all LogicalTimestamps referenced by this node, including
	 * the node's own ID and any timestamps contained within the node's structure.
	 * <p>
	 * The stream includes the node's own ID first, followed by any referenced timestamps.
	 * Null values are filtered out.
	 *
	 * @return a stream of all referenced LogicalTimestamps
	 */
	Stream<LogicalTimestamp> streamReferencedTimestamps();

}
