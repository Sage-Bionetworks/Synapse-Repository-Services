package org.sagebionetworks.repo.manager.dynamo;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.dynamo.dao.nodetree.IncompletePathException;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeTreeQueryManagerImpl implements NodeTreeQueryManager {

	private final Logger logger = LogManager.getLogger(NodeTreeQueryManagerImpl.class);

	@Autowired
	private UserManager userManager;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeTreeQueryDao nodeTreeQueryDao;

	@Override
	public boolean isRoot(Long userId, String nodeId) throws UnauthorizedException,
			DatastoreException {
		this.checkAuthorization(userId, nodeId); // throws UnauthorizedException
		return this.nodeTreeQueryDao.isRoot(this.stringToKey(nodeId));
	}

	@Override
	public EntityIdList getAncestors(Long userId, String nodeId)
			throws UnauthorizedException, DatastoreException {
		this.checkAuthorization(userId, nodeId); // throws UnauthorizedException
		try {
			List<String> keys = this.nodeTreeQueryDao.getAncestors(this.stringToKey(nodeId));
			return this.toEntityIdList(keys);
		} catch (IncompletePathException e) {
			this.logger.warn("getAncestors() for node " + nodeId, e);
			// Return an empty list
			EntityIdList idList = new EntityIdList();
			idList.setIdList(new ArrayList<EntityId>(0));
			return idList;
		}
	}

	@Override
	public EntityId getParent(Long userId, String nodeId)
			throws UnauthorizedException, DatastoreException {

		this.checkAuthorization(userId, nodeId); // throws UnauthorizedException
		try {
			String parent = this.nodeTreeQueryDao.getParent(this.stringToKey(nodeId));
			return this.toEntityId(parent);
		} catch (IncompletePathException e) {
			this.logger.warn("getParent() for node " + nodeId, e);
			// Return a null ID
			return new EntityId();
		}
	}

	@Override
	public EntityIdList getDescendants(Long userId, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {
		this.checkAuthorization(userId, nodeId); // throws UnauthorizedException
		try {
			List<String> descList = this.nodeTreeQueryDao.getDescendants(
					this.stringToKey(nodeId), pageSize, this.stringToKey(lastDescIdExcl));
			return this.toEntityIdList(descList);
		} catch (IncompletePathException e) {
			this.logger.warn("getDescendants() for node " + nodeId, e);
			// Return an empty list
			EntityIdList idList = new EntityIdList();
			idList.setIdList(new ArrayList<EntityId>(0));
			return idList;
		}
	}

	@Override
	public EntityIdList getDescendants(Long userId, String nodeId,
			int generation, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException {
		this.checkAuthorization(userId, nodeId); // throws UnauthorizedException
		try {
			List<String> descList = this.nodeTreeQueryDao.getDescendants(
					this.stringToKey(nodeId), generation, pageSize, this.stringToKey(lastDescIdExcl));
			return this.toEntityIdList(descList);
		} catch (IncompletePathException e) {
			this.logger.warn("getDescendants() for node " + nodeId, e);
			// Return an empty list
			EntityIdList idList = new EntityIdList();
			idList.setIdList(new ArrayList<EntityId>(0));
			return idList;
		}
	}

	@Override
	public EntityIdList getChildren(Long userId, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {
		this.checkAuthorization(userId, nodeId); // throws UnauthorizedException
		try {
			List<String> children = this.nodeTreeQueryDao.getDescendants(
					this.stringToKey(nodeId), 1, pageSize, this.stringToKey(lastDescIdExcl));
			return this.toEntityIdList(children);
		} catch (IncompletePathException e) {
			this.logger.warn("getChildren() for node " + nodeId, e);
			// Return an empty list
			EntityIdList idList = new EntityIdList();
			idList.setIdList(new ArrayList<EntityId>(0));
			return idList;
		}
	}

	private String stringToKey(String nodeId) {
		if (nodeId == null) {
			return null;
		}
		return KeyFactory.stringToKey(nodeId).toString();
	}

	private EntityId toEntityId(String key) {
		if (key == null) {
			return null;
		}
		EntityId id = new EntityId();
		String strKey = KeyFactory.keyToString(Long.parseLong(key));
		id.setId(strKey);
		return id;
	}

	private EntityIdList toEntityIdList(List<String> keys) {
		List<EntityId> idList = new ArrayList<EntityId>(keys.size());
		for (String key : keys) {
			idList.add(this.toEntityId(key));
		}
		EntityIdList list = new EntityIdList();
		list.setIdList(idList);
		return list;
	}

	/**
	 * @throws UnauthorizedException When the current user is not authorized to view the node
	 */
	private void checkAuthorization(Long userId, String nodeId)
			throws DatastoreException, UnauthorizedException {

		if (userId == null) {
			throw new IllegalArgumentException("Current user cannot be null or empty.");
		}
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}

		UserInfo currUserInfo = null;
		try {
			currUserInfo = this.userManager.getUserInfo(userId);
		} catch (NotFoundException e) {
			throw new UnauthorizedException("User " + userId + " does not exist.");
		}
		if (currUserInfo == null) {
			throw new UnauthorizedException("User " + userId + " does not exist.");
		}

		if (!currUserInfo.isAdmin()) {
			try {
				AuthorizationManagerUtil.checkAuthorizationAndThrowException(
						authorizationManager.canAccess(
						currUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ));
			} catch (NotFoundException e) {
				throw new UnauthorizedException(userId
						+ " does not have read access to the requested entity.");
			}
		}
	}
}
