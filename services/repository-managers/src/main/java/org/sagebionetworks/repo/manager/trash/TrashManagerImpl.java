package org.sagebionetworks.repo.manager.trash;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashManagerImpl implements TrashManager {
	
	/**
	 * The maximum number of sub-folder IDs that can be loaded into memory.
	 */
	public static final int MAX_IDS_TO_LOAD = 10*1000;

	private static final String UNABLE_TO_DELETE_TOO_MANY_SUB_FOLDERS = "Unable to delete a project/folder with more than "+MAX_IDS_TO_LOAD+" sub-folders. Please delete the sub-folders first.";

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private AccessControlListDAO aclDAO;

	@Autowired
	private TrashCanDao trashCanDao;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;

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
		/*
		 * If the node is already deleted or does not exist then do nothing.
		 * This is a fix for PLFM-2921 and PLFM-3923
		 */
		if(!nodeDao.isNodeAvailable(nodeId)){
			return;
		}

		// Authorize
		UserInfo.validateUserInfo(currentUser);
		authorizationManager.canAccess(currentUser, nodeId, ObjectType.ENTITY, ACCESS_TYPE.DELETE).checkAuthorizationOrElseThrow();

		// Move the node to the trash can folder
		Node node = nodeDao.getNode(nodeId);
		// Save before we reset the name and the parent
		final String oldNodeName = node.getName();
		final String oldParentId = node.getParentId();
		// Set the name to its ID to guarantee the entities in the trash folder all have unique names (PLFM-1760)
		node.setName(node.getId());
		final String trashCanId = KeyFactory.keyToString(TrashConstants.TRASH_FOLDER_ID);
		node.setParentId(trashCanId);
		updateNodeForTrashCan(currentUser, node, ChangeType.DELETE);
		deleteAllAclsInHierarchy(nodeId);
		// Update the trash can table
		String userGroupId = currentUser.getId().toString();
		trashCanDao.create(userGroupId, nodeId, oldNodeName, oldParentId);
	}

	/**
	 * Delete all ACLs in the entire hierarchy defined by the given parent
	 * ID.
	 * @param nodeId
	 */
	void deleteAllAclsInHierarchy(final String nodeId) {
		// If this node has an ACL then delete it.
		aclDAO.delete(nodeId, ObjectType.ENTITY);
		
		// Delete all ACLs within the hierarchy
		// Get the list of all parentIds for this hierarchy.
		Set<Long> allParentIds;
		try {
			allParentIds = nodeDao.getAllContainerIds(nodeId, MAX_IDS_TO_LOAD);
		} catch (LimitExceededException e) {
			throw new IllegalArgumentException(UNABLE_TO_DELETE_TOO_MANY_SUB_FOLDERS);
		}
		// Lookup all children with ACLs for the given parents.
		List<Long> childrenWithAcls = aclDAO.getChildrenEntitiesWithAcls(new LinkedList<Long>(allParentIds));
		aclDAO.delete(childrenWithAcls, ObjectType.ENTITY);
	}

	/**
	 * Updating the node includes an etag check, setting modifiedOn, modifiedBy
	 * and sending a ENTITY_CONTAINER event to trigger the update of all children.
	 * 
	 * @param userInfo
	 * @param node
	 * @param changeType
	 */
	void updateNodeForTrashCan(UserInfo userInfo, Node node,
			ChangeType changeType) {
		// Lock the node and update the etag, modifiedOn and modifiedBy.
		final String nextETag = nodeDao.touch(userInfo.getId(), node.getId(), changeType);	
		// Clear the modified data and fill it in with the new data
		if(NodeUtils.isProjectOrFolder(node.getNodeType())){
			// This message will trigger a worker to send a message for each child of this hierarchy.
			transactionalMessenger.sendMessageAfterCommit(node.getId(), ObjectType.ENTITY_CONTAINER, nextETag, changeType);
		}
		// update the node
		nodeDao.updateNode(node);
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
		if (!nodeDao.isNodeAvailable(newParentId)) {
			throw new ParentInTrashCanException("The intended parent " + newParentId + " is in the trash can and cannot be restored to.");
		}

		authorizationManager.canAccess(currentUser, newParentId, ObjectType.ENTITY, ACCESS_TYPE.CREATE).checkAuthorizationOrElseThrow();
		Node node = nodeDao.getNode(nodeId);
		authorizationManager.canUserMoveRestrictedEntity(currentUser, trash.getOriginalParentId(), newParentId).checkAuthorizationOrElseThrow();
		
		// Only projects can move to root
		if(NodeUtils.isRootEntityId(newParentId)){
			if(!EntityType.project.equals(node.getNodeType())){
				throw new IllegalArgumentException("Ony projects can be restored to root");
			}
		}

		// Now restore
		node.setName(trash.getEntityName());
		node.setParentId(newParentId);
		updateNodeForTrashCan(currentUser, node, ChangeType.CREATE);
		// If the new parent is root then add an ACL.
		if(NodeUtils.isRootEntityId(newParentId)){
			// Create an ACL for this entity.
			AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(nodeId, currentUser, new Date());
			aclDAO.create(acl, ObjectType.ENTITY);
		}

		// Update the trash can table
		trashCanDao.delete(deletedBy, nodeId);
	}

	@Override
	public List<TrashedEntity> viewTrashForUser(
			UserInfo currentUser, UserInfo user, long offset, long limit) {
		ValidateArgument.required(currentUser, "currentUser");
		ValidateArgument.required(user, "user");
		if (offset < 0L) {
			throw new IllegalArgumentException("Offset cannot be < 0");
		}
		if (limit < 0L) {
			throw new IllegalArgumentException("Limit cannot be < 0");
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

		return trashCanDao.getInRangeForUser(userId, false, offset, limit);
	}

	@Override
	public List<TrashedEntity> viewTrash(UserInfo currentUser,
			long offset, long limit) throws DatastoreException,
			UnauthorizedException {

		if (currentUser == null) {
			throw new IllegalArgumentException("Current user cannot be null");
		}
		if (offset < 0L) {
			throw new IllegalArgumentException("Offset cannot be < 0");
		}
		if (limit < 0L) {
			throw new IllegalArgumentException("Limit cannot be < 0");
		}

		UserInfo.validateUserInfo(currentUser);
		if (!currentUser.isAdmin()) {
			String currUserId = currentUser.getId().toString();
			throw new UnauthorizedException("Current user " + currUserId
					+ " does not have the permission.");
		}

		return trashCanDao.getInRange(false, offset, limit);
	}

	@WriteTransaction
	@Override
	public void purgeTrashForUser(UserInfo currentUser, String nodeId,
			PurgeCallback purgeCallback) throws DatastoreException,
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
			throw new NotFoundException("The node " + nodeId
					+ " is not in the trash can.");
		}

		if (purgeCallback != null) {
			purgeCallback.startPurge(nodeId);
		}

		nodeDao.delete(nodeId);
		aclDAO.delete(nodeId, ObjectType.ENTITY);
		trashCanDao.delete(userGroupId, nodeId);
		if (purgeCallback != null) {
			purgeCallback.endPurge();
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
	
	@WriteTransaction
	@Override
	public void purgeTrashAdmin(List<Long> trashIDs, UserInfo userInfo){
		ValidateArgument.required(trashIDs, "trashIDs");
		ValidateArgument.required(userInfo, "userInfo");
		
		if (!userInfo.isAdmin()) {
			String userId = userInfo.getId().toString();
			throw new UnauthorizedException("Only an Administrator can perform this action.");
		}
	
		nodeDao.delete(trashIDs);
		aclDAO.delete(trashIDs, ObjectType.ENTITY);
		trashCanDao.delete(trashIDs);
	}

	@Override
	public List<TrashedEntity> getTrashBefore(Timestamp timestamp) throws DatastoreException {
		return trashCanDao.getTrashBefore(timestamp);
	}
	
	@Override
	public List<Long> getTrashLeavesBefore(long numDays, long maxTrashItems) throws DatastoreException{
		return trashCanDao.getTrashLeaves(numDays, maxTrashItems);
	}

}
