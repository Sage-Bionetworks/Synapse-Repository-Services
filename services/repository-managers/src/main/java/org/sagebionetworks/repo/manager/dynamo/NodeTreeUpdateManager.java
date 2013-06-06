package org.sagebionetworks.repo.manager.dynamo;

import java.util.Date;

/**
 * Updates the node tree in DynamoDB.
 *
 * @author Eric Wu
 */
public interface NodeTreeUpdateManager {

	/**
	 * Creates a new child-parent edge.
	 */
	void create(String childId, String parentId, Date timestamp);

	/**
	 * Updates a child-parent edge.
	 */
	void update(String childId, String parentId, Date timestamp);

	/**
	 * Deletes a node from the tree. This has the side-effect of also
	 * removing the descendants below the node being deleted.
	 */
	void delete(String nodeId, Date timestamp);
}
