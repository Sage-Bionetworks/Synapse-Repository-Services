package org.sagebionetworks.repo.manager.trash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
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
	public void moveToTrash(final UserInfo userInfo, final String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null");
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

		// Move the node to the trash can folder
		Node node = this.nodeDao.getNode(nodeId);
		// Save before we reset the name and the parent
		final String oldNodeName = node.getName();
		final String oldParentId = node.getParentId();
		// Set the name to its ID to guarantee the entities in the trash folder all have unique names (PLFM-1760)
		node.setName(node.getId());
		final String trashCanId = this.nodeDao.getNodeIdForPath(TrashConstants.TRASH_FOLDER_PATH);
		if (trashCanId == null) {
			throw new DatastoreException("Trash can folder does not exist.");
		}
		node.setParentId(trashCanId);
		this.nodeManager.updateForTrashCan(userInfo, node, ChangeType.DELETE);

		// Update the trash can table
		String userGroupId = userInfo.getIndividualGroup().getId();
		this.trashCanDao.create(userGroupId, nodeId, oldNodeName, oldParentId);

		// For all the descendants, we need to add them to the trash can table
		// and send delete messages to 2nd indices
		Collection<String> descendants = new ArrayList<String>();
		this.getDescendants(nodeId, descendants);
		for (String descendantId : descendants) {
			final EntityHeader entityHeader =  this.nodeDao.getEntityHeader(descendantId, null);
			final String nodeName = entityHeader.getName();
			final String parentId = this.nodeDao.getParentId(descendantId);
			this.trashCanDao.create(userGroupId, descendantId, nodeName, parentId);
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
			throw new IllegalArgumentException("User info cannot be null");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null");
		}

		// Make sure the node was indeed deleted by the user
		UserInfo.validateUserInfo(userInfo);
		String userId = userInfo.getIndividualGroup().getId();
		boolean exists = this.trashCanDao.exists(userId, nodeId);
		if (!exists) {
			throw new NotFoundException("The node " + nodeId + " is not in the trash can.");
		}

		// Restore to its original parent if a new parent is not given
		final TrashedEntity trash = this.trashCanDao.getTrashedEntity(userId, nodeId);
		if (newParentId == null) {
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
		node.setName(trash.getEntityName());
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
			// Remove from the trash can table
			this.trashCanDao.delete(userGroupId, descendantId);
			// Send CREATE message
			String parentId = this.nodeDao.getParentId(descendantId);
			String etag = this.nodeDao.peekCurrentEtag(descendantId);
			this.tagMessenger.sendMessage(descendantId, parentId, etag,
					ObjectType.ENTITY, ChangeType.CREATE);
		}
	}

	@Override
	public QueryResults<TrashedEntity> viewTrash(UserInfo userInfo,
			Long offset, Long limit) {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null");
		}
		if (offset == null) {
			throw new IllegalArgumentException("Offset cannot be null");
		}
		if (limit == null) {
			throw new IllegalArgumentException("Limit cannot be null");
		}

		UserInfo.validateUserInfo(userInfo);
		String userGroupId = userInfo.getIndividualGroup().getId();
		List<TrashedEntity> list = this.trashCanDao.getInRangeForUser(userGroupId, offset, limit);
		int count = this.trashCanDao.getCount(userGroupId);
		QueryResults<TrashedEntity> results = new QueryResults<TrashedEntity>(list, count);
		return results;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void purge(UserInfo userInfo, String nodeId)
			throws DatastoreException, NotFoundException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null.");
		}

		// Make sure the node was indeed deleted by the user
		UserInfo.validateUserInfo(userInfo);
		String userGroupId = userInfo.getIndividualGroup().getId();
		boolean exists = this.trashCanDao.exists(userGroupId, nodeId);
		if (!exists) {
			throw new NotFoundException("The node " + nodeId + " is not in the trash can.");
		}

		Collection<String> descendants = new ArrayList<String>();
		this.getDescendants(nodeId, descendants);
		nodeDao.delete(nodeId);
		trashCanDao.delete(userGroupId, nodeId);
		for (String desc : descendants) {
			trashCanDao.delete(userGroupId, desc);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void purge(UserInfo userInfo) throws DatastoreException, NotFoundException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}

		UserInfo.validateUserInfo(userInfo);
		String userGroupId = userInfo.getIndividualGroup().getId();

		// For subtrees moved entirely into the trash can, we want to find the roots
		// of these subtrees. Deleting the roots should delete the subtrees. We use
		// a set of the trashed items to help find the roots.
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userGroupId, 0, Long.MAX_VALUE);
		Set<String> trashIdSet = new HashSet<String>();
		for (TrashedEntity trash : trashList) {
			trashIdSet.add(trash.getEntityId());
		}
		for (TrashedEntity trash : trashList) {
			String nodeId = trash.getEntityId();
			if (!trashIdSet.contains(trash.getOriginalParentId())) {
				nodeDao.delete(nodeId);
			}
			trashCanDao.delete(userGroupId, nodeId);
		}
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
