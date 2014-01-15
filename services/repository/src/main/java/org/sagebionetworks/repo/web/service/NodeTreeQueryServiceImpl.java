package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.dynamo.NodeTreeQueryManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeTreeQueryServiceImpl implements NodeTreeQueryService {

	@Autowired
	private NodeTreeQueryManager nodeTreeQueryManager;

	@Override
	public EntityIdList getAncestors(Long userId, String nodeId)
			throws UnauthorizedException, DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getAncestors(userId, nodeId);
	}

	@Override
	public EntityId getParent(Long userId, String nodeId)
			throws UnauthorizedException, DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getParent(userId, nodeId);
	}

	@Override
	public EntityIdList getDescendants(Long userId, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getDescendants(userId, nodeId, pageSize, lastDescIdExcl);
	}

	@Override
	public EntityIdList getDescendants(Long userId, String nodeId,
			int generation, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getDescendants(
				userId, nodeId, generation, pageSize, lastDescIdExcl);
	}

	@Override
	public EntityIdList getChildren(Long userId, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getChildren(userId, nodeId, pageSize, lastDescIdExcl);
	}
}
