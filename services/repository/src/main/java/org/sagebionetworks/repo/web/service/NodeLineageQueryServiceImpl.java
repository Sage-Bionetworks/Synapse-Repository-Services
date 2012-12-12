package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.dynamo.dao.IncompletePathException;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeLineageQueryServiceImpl implements NodeLineageQueryService {

	@Autowired
	private UserManager userManager;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	@Override
	public String getRoot(String currUserName) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}

		String root = this.keyToString(this.nodeTreeDao.getRoot());
		this.checkAuthorization(currUserName, root); // throws UnauthorizedException
		return root;
	}

	@Override
	public List<String> getAncestors(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException, IncompletePathException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		List<String> ancestorList = this.nodeTreeDao.getAncestors(this.stringToKey(nodeId));
		return this.keyToString(ancestorList);
	}

	@Override
	public String getParent(String currUserName, String nodeId)
			throws UnauthorizedException, DatastoreException, IncompletePathException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		String parent = this.nodeTreeDao.getParent(this.stringToKey(nodeId));
		return this.keyToString(parent);
	}

	@Override
	public List<String> getDescendants(String currUserName, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		List<String> descList = this.nodeTreeDao.getDescendants(
				this.stringToKey(nodeId), pageSize, lastDescIdExcl);
		return this.keyToString(descList);
	}

	@Override
	public List<String> getDescendants(String currUserName, String nodeId,
			int generation, int pageSize, String lastDescIdExcl)
			throws UnauthorizedException, DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		List<String> descList = this.nodeTreeDao.getDescendants(
				this.stringToKey(nodeId), generation, pageSize, lastDescIdExcl);
		return this.keyToString(descList);
	}

	@Override
	public List<String> getChildren(String currUserName, String nodeId,
			int pageSize, String lastDescIdExcl) throws UnauthorizedException,
			DatastoreException {

		if (currUserName == null) {
			throw new NullPointerException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new NullPointerException("Node cannot be null.");
		}

		this.checkAuthorization(currUserName, nodeId); // throws UnauthorizedException
		List<String> children = this.nodeTreeDao.getChildren(
				this.stringToKey(nodeId), pageSize, lastDescIdExcl);
		return this.keyToString(children);
	}

	@Override
	public String getLowestCommonAncestor(String currUserName, String nodeX,
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
		String ancestor = this.nodeTreeDao.getLowestCommonAncestor(
				this.stringToKey(nodeX), this.stringToKey(nodeY));
		return this.keyToString(ancestor);
	}

	private String stringToKey(String nodeId) {
		if (nodeId == null) {
			return null;
		}
		return KeyFactory.stringToKey(nodeId).toString();
	}

	private String keyToString(String key) {
		if (key == null) {
			return null;
		}
		return KeyFactory.keyToString(Long.parseLong(key));
	}

	private List<String> keyToString(List<String> keys) {
		List<String> strs = new ArrayList<String>(keys.size());
		for (int i = 0; i < keys.size(); i++) {
			strs.add(this.keyToString(keys.get(i)));
		}
		return strs;
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
