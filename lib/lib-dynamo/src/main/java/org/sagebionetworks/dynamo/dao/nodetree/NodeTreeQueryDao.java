package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.List;

/**
 * Query operations over a tree of nodes.
 */
public interface NodeTreeQueryDao {

	/**
	 * Gets the root node. Returns null if the root does not exist yet.
	 */
	String getRoot();

	/**
	 * Gets all the ancestors for the specified node. The returned ancestors are
	 * ordered in that the first ancestor is the root and the last
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
}
