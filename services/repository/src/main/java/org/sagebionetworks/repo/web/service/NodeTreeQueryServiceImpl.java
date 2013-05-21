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
	public EntityId getRoot(String currUserName) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}

		return this.nodeTreeQueryManager.getRoot(currUserName);
	}

	@Override
	public EntityIdList getAncestors(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getAncestors(currUserName, nodeId);
	}

	@Override
	public EntityId getParent(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getParent(currUserName, nodeId);
	}

	@Override
	public EntityIdList getDescendants(String currUserName, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getDescendants(currUserName, nodeId, pageSize, lastDescIdExcl);
	}

	@Override
	public EntityIdList getDescendants(String currUserName, String nodeId,
			int generation, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getDescendants(
				currUserName, nodeId, generation, pageSize, lastDescIdExcl);
	}

	@Override
	public EntityIdList getChildren(String currUserName, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node cannot be null.");
		}

		return this.nodeTreeQueryManager.getChildren(currUserName, nodeId, pageSize, lastDescIdExcl);
	}
}
