package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;

/**
 * Update operations over a tree of nodes.
 */
public interface NodeTreeUpdateDao {

	/**
	 * Creates a lineage pair on the tree.
	 *
	 * @return True if the create succeeds; false otherwise.
	 */
	boolean create(String child, String parent, Date timestamp) throws IncompletePathException;

	/**
	 * Updates a lineage pair on the tree.
	 *
	 * @return True if the update succeeds; false otherwise.
	 */
	boolean update(String child, String parent, Date timestamp) throws IncompletePathException, ObsoleteChangeException;

	/**
	 * Deletes a node from the tree.
	 *
	 * @return True if the delete succeeds; false otherwise.
	 */
	boolean delete(String nodeId, Date timestamp) throws ObsoleteChangeException;
	
	/**
	 * Are Dyanmo related feature enabled?
	 * @return
	 */
	boolean isDynamoEnabled();
}
