package org.sagebionetworks.dynamo.dao.tree;

import java.util.Date;
import java.util.List;

/**
 * Operations over a tree of nodes.
 *
 * @author Eric Wu
 */
public interface NodeTreeDao {

	public static final int MAX_PAGE_SIZE = 20000;

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
	 * Gets the root node. Returns null if the root does not exist yet.
	 */
	String getRoot();

	/**
	 * Gets all the ancestors for the specified node. The returned ancestors are
	 * ordered in that the first the ancestor is the root and the last
	 * ancestor is the parent. The root will get an empty list of ancestors.
	 *
	 * @throws IncompletePathException When the node does not have a complete path to the root.
	 */
	List<String> getAncestors(String nodeId) throws IncompletePathException;

	/**
	 * Gets the parent of the specified node. Root will get the dummy ROOT as its parent.
	 *
	 */
	String getParent(String nodeId);

	/**
	 * Gets the paginated list of descendants for the specified node.
	 *
	 * @param nodeId
	 *            This node's descendants
	 * @param pageSize
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	List<String> getDescendants(String nodeId, int pageSize, String lastDescIdExcl);

	/**
	 * Gets the paginated list of descendants of a particular generation for the specified node.
	 *
	 * @param nodeId
	 *            This node's descendants
	 * @param generation
	 *            How many generations away from the node. Children are exactly 1 generation away.
	 * @param pageSize
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	List<String> getDescendants(String nodeId, int generation, int pageSize, String lastDescIdExcl);

	/**
	 * Gets the children of the specified node.
	 *
	 * @param nodeId
	 *            This node's descendants
	 * @param pageSize
	 *            Paging parameter. The max number of descendants to fetch per page.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	List<String> getChildren(String nodeId, int pageSize, String lastDescIdExcl);

	/**
	 * Finds the path from X to Y.
	 *
	 * @param nodeX
	 *            ID of node X
	 * @param nodeY
	 *            ID of node Y
	 *
	 * @return The path in between X and Y. Ancestor is the first in the returned list and descendant
	 *         is the last in the returned list. It is up to the user to check the returned list
	 *         which (X or Y) is the ancestor and which is the descendant. Null if no path exists
	 *         between X and Y.
	 */
	List<String> getPath(String nodeX, String nodeY) throws IncompletePathException;

	/**
	 * Finds the lowest common ancestor of two nodes X and Y.
	 *
	 * @param nodeX
	 *            ID of node X
	 * @param nodeY
	 *            ID of node Y
	 *
	 * @throws IncompletePathException When either node does not have a complete path to the root.
	 *
	 * @return The ID of the lowest common ancestor
	 */
	String getLowestCommonAncestor(String nodeX, String nodeY) throws IncompletePathException;
}

