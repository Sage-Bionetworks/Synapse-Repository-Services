package org.sagebionetworks.dynamo.dao;

import java.util.Date;
import java.util.List;

/**
 * Operations over a tree of nodes.
 *
 * @author ewu
 */
public interface NodeTreeDao {

	/**
	 * Creates a lineage pair on the tree.
	 *
	 * @return True if the create succeeds; false otherwise.
	 */
	boolean create(String child, String parent, Date timestamp);

	/**
	 * Updates a lineage pair on the tree.
	 *
	 * @return True if the update succeeds; false otherwise.
	 */
	boolean update(String child, String parent, Date timestamp);

	/**
	 * Deletes a node from the tree.
	 *
	 * @return True if the delete succeeds; false otherwise.
	 */
	boolean delete(String nodeId, Date timestamp);

	/**
	 * Gets all the ancestors for the specified node. The returned ancestors are
	 * ordered in that the first the ancestor is the parent and the last
	 * ancestor is the root.
	 */
	List<String> getAncestors(String nodeId);

	/**
	 * Gets the parent of the specified node.
	 */
	String getParent(String nodeId);

	/**
	 * Gets all the descendants for the specified node.
	 */
	List<String> getDescendants(String nodeId);

	/**
	 * Gets the descendants of a particular generation for the specified node.
	 *
	 * @param nodeId
	 *            Node ID
	 * @param generation
	 *            How many generations away from the node. Children are exactly 1 generation away.
	 */
	List<String> getDescendants(String nodeId, int generation);

	/**
	 * Gets the children of the specified node.
	 */
	List<String> getChildren(String nodeId);

	/**
	 * Finds the path from X to Y.
	 *
	 * @param nodeX
	 *            ID of node X
	 * @param nodeY
	 *            ID of node Y
	 *
	 * @return The path in between X and Y. Null if such path does not exist. An
	 *         empty list indicates X is the immediate parent of Y.
	 */
	List<String> getPath(String nodeX, String nodeY);

	/**
	 * Finds the lowest common ancestor of two nodes X and Y.
	 *
	 * @param nodeX
	 *            ID of node X
	 * @param nodeY
	 *            ID of node Y
	 *
	 * @return The ID of the lowest common ancestor
	 */
	String getLowestCommonAncestor(String nodeX, String nodeY);
}

