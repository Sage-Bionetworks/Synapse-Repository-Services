package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.UnauthorizedException;

public interface NodeTreeQueryService {

	/**
	 * Gets all the ancestors for the specified node. The returned ancestors are
	 * ordered in that the first the ancestor is the root and the last
	 * ancestor is the parent. The root will get an empty list of ancestors.
	 */
	EntityIdList getAncestors(Long userId, String nodeId)
			throws UnauthorizedException, DatastoreException;

	/**
	 * Gets the parent of the specified node. Root will get the dummy ROOT as its parent.
	 */
	EntityId getParent(Long userId, String nodeId)
			throws UnauthorizedException, DatastoreException;

	/**
	 * Gets the paginated list of descendants for the specified node.
	 *
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	EntityIdList getDescendants(Long userId, String nodeId, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException;

	/**
	 * Gets the paginated list of descendants of a particular generation for the specified node.
	 *
	 * @param generation
	 *            How many generations away from the node. Children are exactly 1 generation away.
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	EntityIdList getDescendants(Long userId, String nodeId, int generation, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException;

	/**
	 * Gets the children of the specified node.
	 *
	 * @param lastDescIdExcl
	 *            Paging parameter. The last descendant ID (exclusive).
	 */
	EntityIdList getChildren(Long userId, String nodeId, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException;
}
