package org.sagebionetworks.repo.manager.trash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
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
	private NodeTreeQueryDao nodeTreeQueryDao;

	@Autowired
	private DBOTrashCanDao trashCanDao;

	@Autowired
	private TagMessenger tagMessenger;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void moveToTrash(final UserInfo currentUser, final String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null");
		}

		// Authorize
		UserInfo.validateUserInfo(currentUser);
		String userName = currentUser.getUser().getUserId();
		if (!this.authorizationManager.canAccess(currentUser, nodeId, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName + " lacks change access to the requested object.");
		}

		// Whether it is too big for the trash can
		List<String> idList = this.nodeTreeQueryDao.getDescendants(
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
		final String trashCanId = KeyFactory.keyToString(TrashConstants.TRASH_FOLDER_ID);
		node.setParentId(trashCanId);
		this.nodeManager.updateForTrashCan(currentUser, node, ChangeType.DELETE);

		// Update the trash can table
		String userGroupId = currentUser.getIndividualGroup().getId();
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
	public void restoreFromTrash(UserInfo currentUser, String nodeId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null");
		}

		// Make sure the node was indeed deleted by the user
		UserInfo.validateUserInfo(currentUser);
		String userId = currentUser.getIndividualGroup().getId();
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
		String userName = currentUser.getUser().getUserId();
		if (!this.authorizationManager.canAccess(currentUser, newParentId, ACCESS_TYPE.CREATE)) {
			throw new UnauthorizedException(userName + " lacks change access to the requested object.");
		}

		// Now restore
		Node node = this.nodeDao.getNode(nodeId);
		node.setName(trash.getEntityName());
		node.setParentId(newParentId);
		this.nodeManager.updateForTrashCan(currentUser, node, ChangeType.CREATE);

		// Update the trash can table
		String userGroupId = currentUser.getIndividualGroup().getId();
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
	public QueryResults<TrashedEntity> viewTrashForUser(
			UserInfo currentUser, UserInfo user, Long offset, Long limit) {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null");
		}
		if (offset == null) {
			throw new IllegalArgumentException("Offset cannot be null");
		}
		if (limit == null) {
			throw new IllegalArgumentException("Limit cannot be null");
		}

		UserInfo.validateUserInfo(currentUser);
		UserInfo.validateUserInfo(user);
		final String currUserId = currentUser.getIndividualGroup().getId();
		final String userId = user.getIndividualGroup().getId();
		if (!currentUser.isAdmin()) {
			if (currUserId == null || !currUserId.equals(userId)) {
				throw new UnauthorizedException("Current user " + currUserId
						+ " does not have the permission.");
			}
		}

		List<TrashedEntity> list = this.trashCanDao.getInRangeForUser(userId, offset, limit);
		int count = this.trashCanDao.getCount(userId);
		QueryResults<TrashedEntity> results = new QueryResults<TrashedEntity>(list, count);
		return results;
	}

	@Override
	public QueryResults<TrashedEntity> viewTrash(UserInfo currentUser,
			Long offset, Long limit) throws DatastoreException,
			UnauthorizedException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}
		if (offset == null) {
			throw new IllegalArgumentException("Offset cannot be null");
		}
		if (limit == null) {
			throw new IllegalArgumentException("Limit cannot be null");
		}

		UserInfo.validateUserInfo(currentUser);
		if (!currentUser.isAdmin()) {
			String currUserId = currentUser.getIndividualGroup().getId();
			throw new UnauthorizedException("Current user " + currUserId
					+ " does not have the permission.");
		}

		List<TrashedEntity> list = this.trashCanDao.getInRange(offset, limit);
		int count = this.trashCanDao.getCount();
		QueryResults<TrashedEntity> results = new QueryResults<TrashedEntity>(list, count);
		return results;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void purgeTrashForUser(UserInfo currentUser, String nodeId)
			throws DatastoreException, NotFoundException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null.");
		}

		// Make sure the node was indeed deleted by the user
		UserInfo.validateUserInfo(currentUser);
		String userGroupId = currentUser.getIndividualGroup().getId();
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
	public void purgeTrashForUser(UserInfo currentUser)
			throws DatastoreException, NotFoundException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}

		UserInfo.validateUserInfo(currentUser);
		String userGroupId = currentUser.getIndividualGroup().getId();

		// For subtrees moved entirely into the trash can, we want to find the roots
		// of these subtrees. Deleting the roots should delete the subtrees. We use
		// a set of the trashed items to help find the roots.
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userGroupId, 0, Long.MAX_VALUE);
		purge(trashList);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void purgeTrash(UserInfo currentUser)
			throws DatastoreException, NotFoundException, UnauthorizedException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}

		UserInfo.validateUserInfo(currentUser);
		if (!currentUser.isAdmin()) {
			String currUserId = currentUser.getIndividualGroup().getId();
			throw new UnauthorizedException("Current user " + currUserId
					+ " does not have the permission.");
		}

		List<TrashedEntity> trashList = trashCanDao.getInRange(0, Long.MAX_VALUE);
		purge(trashList);
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
			getDescendants(child, descendants); // Recursion
		}
	}

	private void purge(List<TrashedEntity> trashList)
			throws DatastoreException, NotFoundException {
		// For subtrees moved entirely into the trash can, we want to find the roots
		// of these subtrees. Deleting the roots should delete the subtrees. We use
		// a set of the trashed items to help find the roots.
		Set<String> trashIdSet = new HashSet<String>();
		for (TrashedEntity trash : trashList) {
			trashIdSet.add(trash.getEntityId());
		}
		for (TrashedEntity trash : trashList) {
			String nodeId = trash.getEntityId();
			if (!trashIdSet.contains(trash.getOriginalParentId())) {
				nodeDao.delete(nodeId);
			}
			trashCanDao.delete(trash.getDeletedByPrincipalId(), nodeId);
		}
	}
}
