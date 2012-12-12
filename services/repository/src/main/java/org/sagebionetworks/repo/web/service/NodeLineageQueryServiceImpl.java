package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.dynamo.dao.IncompletePathException;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeLineageQueryServiceImpl implements NodeLineageQueryService {

	private final Logger logger = Logger.getLogger(NodeLineageQueryServiceImpl.class);

	@Autowired
	private UserManager userManager;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	@Override
	public EntityId getRoot(String currUserName) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}

		EntityId root = this.toEntityId(this.nodeTreeDao.getRoot());
		this.checkAuthorization(currUserName, root.getId()); // throws UnauthorizedException
		return root;
	}

	@Override
	public EntityIdList getAncestors(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		try {
			List<String> keys = this.nodeTreeDao.getAncestors(this.stringToKey(nodeId));
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
	public EntityId getParent(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		try {
			String parent = this.nodeTreeDao.getParent(this.stringToKey(nodeId));
			return this.toEntityId(parent);
		} catch (IncompletePathException e) {
			this.logger.warn("getParent() for node " + nodeId, e);
			// Return a null ID
			return new EntityId();
		}
	}

	@Override
	public EntityIdList getDescendants(String currUserName, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		try {
			List<String> descList = this.nodeTreeDao.getDescendants(
					this.stringToKey(nodeId), pageSize, lastDescIdExcl);
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
	public EntityIdList getDescendants(String currUserName, String nodeId,
			int generation, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		try {
			List<String> descList = this.nodeTreeDao.getDescendants(
					this.stringToKey(nodeId), generation, pageSize, lastDescIdExcl);
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
	public EntityIdList getChildren(String currUserName, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		try {
			List<String> children = this.nodeTreeDao.getChildren(
					this.stringToKey(nodeId), pageSize, lastDescIdExcl);
			return this.toEntityIdList(children);
		} catch (IncompletePathException e) {
			this.logger.warn("getChildren() for node " + nodeId, e);
			// Return an empty list
			EntityIdList idList = new EntityIdList();
			idList.setIdList(new ArrayList<EntityId>(0));
			return idList;
		}
	}

	@Override
	public EntityId getLowestCommonAncestor(String currUserName, String nodeX,
			String nodeY) throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeX == null) {
			throw new NullPointerException("Node cannot be null.");
		}
		if (nodeY == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeX); // throws UnauthorizedException
		this.checkAuthorization(currUserName, nodeY); // throws UnauthorizedException
		try {
			String ancestor = this.nodeTreeDao.getLowestCommonAncestor(
					this.stringToKey(nodeX), this.stringToKey(nodeY));
			return this.toEntityId(ancestor);
		} catch (IncompletePathException e) {
			this.logger.warn("getLowestCommonAncestor() for nodes " + nodeX + ", " + nodeY, e);
			// Return a null ID
			return new EntityId();
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
	private void checkAuthorization(String currUserName, String nodeId)
			throws DatastoreException, UnauthorizedException {

		UserInfo currUserInfo = null;
		try {
			currUserInfo = this.userManager.getUserInfo(currUserName);
		} catch (NotFoundException e) {
			throw new UnauthorizedException("User " + currUserName + " does not exist.");
		}
		if (currUserInfo == null) {
			throw new UnauthorizedException("User " + currUserName + " does not exist.");
		}

		if (!currUserInfo.isAdmin()) {
			try {
				if (!this.authorizationManager.canAccess(
						currUserInfo, nodeId, ACCESS_TYPE.READ)) {
					throw new UnauthorizedException(currUserName
							+ " does not have read access to the requested entity.");
				}
			} catch (NotFoundException e) {
				throw new UnauthorizedException(currUserName
						+ " does not have read access to the requested entity.");
			}
		}
	}
}
