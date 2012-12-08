package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.dynamo.dao.IncompletePathException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;

public interface NodeLineageQueryService {

	/**
	 * Gets the root entity. Returns null if the root cannot be found.
	 */
	String getRoot(String currUserName) throws UnauthorizedException, DatastoreException;

	/**
	 * Gets all the ancestors for the specified node. The returned ancestors are
	 * ordered in that the first the ancestor is the root and the last
	 * ancestor is the parent. The root will get an empty list of ancestors.
	 */
	List<String> getAncestors(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException, IncompletePathException;

	/**
	 * Gets the parent of the specified node. Root will get the dummy ROOT as its parent.
	 */
	String getParent(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException, IncompletePathException;

	/**
	 * Gets the paginated list of descendants for the specified node.
	 *
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	List<String> getDescendants(String currUserName, String nodeId, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException, IncompletePathException;

	/**
	 * Gets the paginated list of descendants of a particular generation for the specified node.
	 *
	 * @param generation
	 *            How many generations away from the node. Children are exactly 1 generation away.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	List<String> getDescendants(String currUserName, String nodeId, int generation, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException, IncompletePathException;

	/**
	 * Gets the children of the specified node.
	 *
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	List<String> getChildren(String currUserName, String nodeId, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException, IncompletePathException;

	/**
	 * Finds the lowest common ancestor of two nodes X and Y.
	 *
	 * @return The ID of the lowest common ancestor
	 */
	String getLowestCommonAncestor(String currUserName, String nodeX, String nodeY)
			throws UnauthorizedException, DatastoreException, IncompletePathException;
}
