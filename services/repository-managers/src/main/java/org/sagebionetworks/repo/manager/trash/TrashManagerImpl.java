package org.sagebionetworks.repo.manager.trash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOTrashCanDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TrashManagerImpl implements TrashManager {

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	@Autowired
	private DBOTrashCanDao trashCanDao;

	@Autowired
	private TagMessenger tagMessenger;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void moveToTrash(UserInfo userInfo, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (userInfo == null) {
			throw new IllegalArgumentException("userInfo cannot be null");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null");
		}

		// Authorize
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!this.authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName + " lacks change access to the requested object.");
		}

		// Whether it is too big for the trash can
		List<String> idList = this.nodeTreeDao.getDescendants(
				KeyFactory.stringToKey(nodeId).toString(), TrashConstants.MAX_TRASHABLE + 1, null);
		if (idList != null && idList.size() > TrashConstants.MAX_TRASHABLE) {
			throw new TooBigForTrashcanException(
					"Too big to fit into trashcan. Entity " + nodeId
					+ " has more than " + TrashConstants.MAX_TRASHABLE + " descendants.");
		}

		// Move the node the trash can folder
		Node node = this.nodeDao.getNode(nodeId);
		final String oldParentId = node.getParentId(); // Save it before we reset it!
		final String trashCanId = this.nodeDao.getNodeIdForPath(TrashConstants.TRASH_FOLDER_PATH);
		node.setParentId(trashCanId);
		this.nodeManager.updateForTrashCan(userInfo, node, ChangeType.DELETE);

		// Update the trash can table
		String userGroupId = userInfo.getIndividualGroup().getId();
		this.trashCanDao.create(userGroupId, nodeId, oldParentId);

		// For all the descendants, we need to add them to the trash can table
		// and send delete messages to 2nd indices
		Collection<String> descendants = new ArrayList<String>();
		this.getDescendants(nodeId, descendants);
		for (String descendantId : descendants) {
			String parentId = this.nodeDao.getParentId(descendantId);
			this.trashCanDao.create(userGroupId, descendantId, parentId);
			String etag = this.nodeDao.peekCurrentEtag(descendantId);
			this.tagMessenger.sendMessage(descendantId, parentId, etag,
					ObjectType.ENTITY, ChangeType.DELETE);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void restoreFromTrash(UserInfo userInfo, String nodeId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (userInfo == null) {
			throw new IllegalArgumentException("userInfo cannot be null");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("nodeId cannot be null");
		}

		// Make sure the node was indeed deleted by the user
		UserInfo.validateUserInfo(userInfo);
		String userId = userInfo.getIndividualGroup().getId();
		boolean exists = this.trashCanDao.exists(userId, nodeId);
		if (!exists) {
			throw new NotFoundException("The node " + nodeId + " is not in the trash can.");
		}

		// Restore to its original parent if a new parent is not given
		if (newParentId == null) {
			TrashedEntity trash = this.trashCanDao.getTrashedEntity(userId, nodeId);
			if (trash == null) {
				throw new DatastoreException("Cannot find node " + nodeId
						+ " in the trash can for user " + userId);
			}
			newParentId = trash.getOriginalParentId();
		}

		// Authorize on the new parent
		String userName = userInfo.getUser().getUserId();
		if (!this.authorizationManager.canAccess(userInfo, newParentId, ACCESS_TYPE.CREATE)) {
			throw new UnauthorizedException(userName + " lacks change access to the requested object.");
		}

		// Now restore
		Node node = this.nodeDao.getNode(nodeId);
		node.setParentId(newParentId);
		this.nodeManager.updateForTrashCan(userInfo, node, ChangeType.CREATE);
		
		// Update the trash can table
		String userGroupId = userInfo.getIndividualGroup().getId();
		this.trashCanDao.delete(userGroupId, nodeId);

		// For all the descendants, we need to remove them from the trash can table
		// and send delete messages to 2nd indices
		Collection<String> descendants = new ArrayList<String>();
		this.getDescendants(nodeId, descendants);
		for (String descendantId : descendants) {
			this.trashCanDao.delete(userGroupId, descendantId);
			String parentId = this.nodeDao.getParentId(descendantId);
			String etag = this.nodeDao.peekCurrentEtag(descendantId);
			this.tagMessenger.sendMessage(descendantId, parentId, etag,
					ObjectType.ENTITY, ChangeType.CREATE);
		}
	}

	@Override
	public QueryResults<TrashedEntity> viewTrash(UserInfo userInfo,
			Integer offset, Integer limit) {

		if (userInfo == null) {
			throw new IllegalArgumentException("userInfo cannot be null");
		}
		if (offset == null) {
			throw new IllegalArgumentException("offset cannot be null");
		}
		if (limit == null) {
			throw new IllegalArgumentException("limit cannot be null");
		}

		UserInfo.validateUserInfo(userInfo);
		String userGroupId = userInfo.getIndividualGroup().getId();
		List<TrashedEntity> list = this.trashCanDao.getInRangeForUser(userGroupId, offset, limit);
		int count = this.trashCanDao.getCount(userGroupId);
		QueryResults<TrashedEntity> results = new QueryResults<TrashedEntity>(list, count);
		return results;
	}

	/**
	 * Recursively gets the IDs of all the descendants.
	 */
	private void getDescendants(String nodeId, Collection<String> descendants) {
		List<String> children = this.nodeDao.getChildrenIdsAsList(nodeId);
		descendants.addAll(children);
		if (children == null || children.size() == 0) {
			return;
		}
		for (String child : children) {
			// Recursion
			getDescendants(child, descendants);
		}
	}
}
