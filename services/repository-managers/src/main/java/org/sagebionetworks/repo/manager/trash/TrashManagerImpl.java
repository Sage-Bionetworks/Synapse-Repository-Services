package org.sagebionetworks.repo.manager.trash;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

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
	private AccessControlListDAO aclDAO;

	@Autowired
	private TrashCanDao trashCanDao;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@Autowired
	private StackConfiguration stackConfig;

	@WriteTransaction
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
		String userName = currentUser.getId().toString();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, nodeId, ObjectType.ENTITY, ACCESS_TYPE.DELETE));

		// Move the node to the trash can folder
		Node node = nodeDao.getNode(nodeId);
		// Save before we reset the name and the parent
		final String oldNodeName = node.getName();
		final String oldParentId = node.getParentId();
		// Set the name to its ID to guarantee the entities in the trash folder all have unique names (PLFM-1760)
		node.setName(node.getId());
		final String trashCanId = KeyFactory.keyToString(TrashConstants.TRASH_FOLDER_ID);
		node.setParentId(trashCanId);
		nodeManager.updateForTrashCan(currentUser, node, ChangeType.DELETE);

		// Update the trash can table
		String userGroupId = currentUser.getId().toString();
		trashCanDao.create(userGroupId, nodeId, oldNodeName, oldParentId);

		// For all the descendants, we need to add them to the trash can table
		// and send delete messages to 2nd indices
		Collection<String> descendants = new ArrayList<String>();
		getDescendants(nodeId, descendants);
		for (String descendantId : descendants) {
			final EntityHeader entityHeader =  nodeDao.getEntityHeader(descendantId, null);
			final String nodeName = entityHeader.getName();
			final String parentId = nodeDao.getParentId(descendantId);
			trashCanDao.create(userGroupId, descendantId, nodeName, parentId);
			String etag = nodeDao.peekCurrentEtag(descendantId);
			transactionalMessenger.sendMessageAfterCommit(descendantId, ObjectType.ENTITY, etag, parentId, ChangeType.DELETE);
		}
	}

	@WriteTransaction
	@Override
	public void restoreFromTrash(UserInfo currentUser, String nodeId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null");
		}
		
		// Make sure the node is in the trash can
		final TrashedEntity trash = trashCanDao.getTrashedEntity(nodeId);
		if (trash == null) {
			throw new NotFoundException("The node " + nodeId + " is not in the trash can.");
		}

		// Make sure the node was indeed deleted by the user
		UserInfo.validateUserInfo(currentUser);
		final String userId = currentUser.getId().toString();
		final String deletedBy = trash.getDeletedByPrincipalId();
		if (!currentUser.isAdmin() && !deletedBy.equals(userId)) {
			throw new UnauthorizedException("User " + userId + " not allowed to restore "
					+ nodeId + ". The node was deleted by a different user.");
		}

		// Restore to its original parent if a new parent is not given
		if (newParentId == null) {
			newParentId = trash.getOriginalParentId();
		}
		
		// Make sure new parent is not in trash can.
		if (trashCanDao.getTrashedEntity(newParentId) != null) {
			throw new ParentInTrashCanException("The intended parent " + newParentId + " is in the trash can and cannot be restored to.");
		}

		// Authorize on the new parent
		String userName = currentUser.getId().toString();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(currentUser, newParentId, ObjectType.ENTITY, ACCESS_TYPE.CREATE));
		Node node = nodeDao.getNode(nodeId);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canUserMoveRestrictedEntity(currentUser, trash.getOriginalParentId(), newParentId));

		// Now restore
		node.setName(trash.getEntityName());
		node.setParentId(newParentId);
		nodeManager.updateForTrashCan(currentUser, node, ChangeType.CREATE);

		// Update the trash can table
		trashCanDao.delete(deletedBy, nodeId);

		// For all the descendants, we need to remove them from the trash can table
		// and send delete messages to 2nd indices
		Collection<String> descendants = new ArrayList<String>();
		getDescendants(nodeId, descendants);
		for (String descendantId : descendants) {
			// Remove from the trash can table
			trashCanDao.delete(deletedBy, descendantId);
			// Send CREATE message
			String parentId = nodeDao.getParentId(descendantId);
			String etag = nodeDao.peekCurrentEtag(descendantId);
			transactionalMessenger.sendMessageAfterCommit(descendantId, ObjectType.ENTITY, etag, parentId, ChangeType.CREATE);
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
		final String currUserId = currentUser.getId().toString();
		final String userId = user.getId().toString();
		if (!currentUser.isAdmin()) {
			if (currUserId == null || !currUserId.equals(userId)) {
				throw new UnauthorizedException("Current user " + currUserId
						+ " does not have the permission.");
			}
		}

		List<TrashedEntity> list = trashCanDao.getInRangeForUser(userId, false, offset, limit);
		int count = trashCanDao.getCount(userId);
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
			String currUserId = currentUser.getId().toString();
			throw new UnauthorizedException("Current user " + currUserId
					+ " does not have the permission.");
		}

		List<TrashedEntity> list = trashCanDao.getInRange(false, offset, limit);
		int count = trashCanDao.getCount();
		QueryResults<TrashedEntity> results = new QueryResults<TrashedEntity>(list, count);
		return results;
	}

	@WriteTransaction
	@Override
	public void purgeTrashForUser(UserInfo currentUser, String nodeId, PurgeCallback purgeCallback) throws DatastoreException,
			NotFoundException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}
		if (nodeId == null) {
			throw new IllegalArgumentException("Node ID cannot be null.");
		}

		// Make sure the node was indeed deleted by the user
		UserInfo.validateUserInfo(currentUser);
		String userGroupId = currentUser.getId().toString();
		boolean exists = trashCanDao.exists(userGroupId, nodeId);
		if (!exists) {
			throw new NotFoundException("The node " + nodeId + " is not in the trash can.");
		}

		if (purgeCallback != null) {
			purgeCallback.startPurge(nodeId);
		}
		Collection<String> descendants = new ArrayList<String>();
		getDescendants(nodeId, descendants);
		nodeDao.delete(nodeId);
		aclDAO.delete(nodeId, ObjectType.ENTITY);
		trashCanDao.delete(userGroupId, nodeId);
		if (purgeCallback != null) {
			purgeCallback.endPurge();
		}
		for (String desc : descendants) {
			if (purgeCallback != null) {
				purgeCallback.startPurge(nodeId);
			}
			trashCanDao.delete(userGroupId, desc);
			if (purgeCallback != null) {
				purgeCallback.endPurge();
			}
		}
	}

	@WriteTransaction
	@Override
	public void purgeTrashForUser(UserInfo currentUser, PurgeCallback purgeCallback) throws DatastoreException, NotFoundException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null.");
		}

		UserInfo.validateUserInfo(currentUser);
		String userGroupId = currentUser.getId().toString();

		// For subtrees moved entirely into the trash can, we want to find the roots
		// of these subtrees. Deleting the roots should delete the subtrees. We use
		// a set of the trashed items to help find the roots.
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userGroupId, true, 0, Long.MAX_VALUE);
		purgeTrash(trashList, purgeCallback);
	}

	@WriteTransaction
	@Override
	public void purgeTrash(UserInfo currentUser, PurgeCallback purgeCallback) throws DatastoreException, NotFoundException,
			UnauthorizedException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}

		UserInfo.validateUserInfo(currentUser);
		if (!currentUser.isAdmin()) {
			String currUserId = currentUser.getId().toString();
			throw new UnauthorizedException("Current user " + currUserId
					+ " does not have the permission.");
		}

		List<TrashedEntity> trashList = trashCanDao.getInRange(true, 0, Long.MAX_VALUE);
		purgeTrash(trashList, purgeCallback);
	}

	@WriteTransaction
	@Override
	public void purgeTrash(List<TrashedEntity> trashList, PurgeCallback purgeCallback) throws DatastoreException, NotFoundException {
		
		if (trashList == null) {
			throw new IllegalArgumentException("Trash list cannot be null.");
		}

		// For subtrees moved entirely into the trash can, we want to find the roots
		// of these subtrees. Deleting the roots should delete the subtrees. We use
		// a set of the trashed items to help find the roots.
		Set<String> trashIdSet = new HashSet<String>();
		for (TrashedEntity trash : trashList) {
			trashIdSet.add(trash.getEntityId());
		}

		// Purge now
		for (TrashedEntity trash : trashList) {
			String nodeId = trash.getEntityId();
			if (purgeCallback != null) {
				purgeCallback.startPurge(nodeId);
			}
			if (!trashIdSet.contains(trash.getOriginalParentId())) {
				nodeDao.delete(nodeId);
				aclDAO.delete(nodeId, ObjectType.ENTITY);
			}
			trashCanDao.delete(trash.getDeletedByPrincipalId(), nodeId);
			if (purgeCallback != null) {
				purgeCallback.endPurge();
			}
		}
	}

	@Override
	public List<TrashedEntity> getTrashBefore(Timestamp timestamp) throws DatastoreException {
		return trashCanDao.getTrashBefore(timestamp);
	}

	/**
	 * Recursively gets the IDs of all the descendants.
	 */
	private void getDescendants(String nodeId, Collection<String> descendants) {
		List<String> children = nodeDao.getChildrenIdsAsList(nodeId);
		descendants.addAll(children);
		if (children == null || children.size() == 0) {
			return;
		}
		for (String child : children) {
			getDescendants(child, descendants); // Recursion
		}
	}
}
