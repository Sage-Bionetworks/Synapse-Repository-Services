package org.sagebionetworks.repo.manager.trash;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
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
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.trash.TrashCanDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrashManagerImpl implements TrashManager {
	
	/**
	 * The maximum number of sub-folder IDs that can be loaded into memory.
	 */
	public static final int MAX_IDS_TO_LOAD = 10*1000;

	private static final String UNABLE_TO_DELETE_TOO_MANY_SUB_FOLDERS = "Unable to delete a project/folder with more than "+MAX_IDS_TO_LOAD+" sub-folders. Please delete the sub-folders first.";

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private AccessControlListDAO aclDAO;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private TrashCanDao trashCanDao;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@Override
	public boolean doesEntityHaveTrashedChildren(String parentId) {
		ValidateArgument.required(parentId, "Parent ID");
		return trashCanDao.doesEntityHaveTrashedChildren(parentId);
	}

	@WriteTransaction
	@Override
	public void moveToTrash(final UserInfo currentUser, final String nodeId, boolean priorityPurge)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(currentUser, "current user");
		ValidateArgument.required(nodeId, "node id");
		/*
		 * If the node is already deleted or does not exist then do nothing.
		 * This is a fix for PLFM-2921 and PLFM-3923
		 */
		if(!nodeDao.isNodeAvailable(nodeId)){
			return;
		}

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
		trashCanDao.create(userGroupId, nodeId, oldNodeName, oldParentId, priorityPurge);
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
			transactionalMessenger.sendMessageAfterCommit(node.getId(), ObjectType.ENTITY_CONTAINER, changeType);
		}
		// update the node
		nodeDao.updateNode(node);
	}

	@WriteTransaction
	@Override
	public void restoreFromTrash(UserInfo currentUser, String nodeId, String newParentId)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		ValidateArgument.required(currentUser, "The current user");
		ValidateArgument.required(nodeId, "The node id");
		
		// Make sure the node is in the trash can
		final TrashedEntity trash = trashCanDao.getTrashedEntity(nodeId);
		
		if (trash == null) {
			throw new NotFoundException("The node " + nodeId + " is not in the trash can.");
		}

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

		if (!newParentId.equals(trash.getOriginalParentId())) {
			if (node.getNodeType() == EntityType.file || node.getNodeType() == EntityType.folder) {
				// For files and folders restored to a different folder, this is not allowed if the new parent is an
				// STS folder. This is to ensure that our STS folders don't end up in a bad state. (Note that restoring
				// to the same original folder is always allowed.)
				Optional<UploadDestinationListSetting> projectSetting = projectSettingsManager.getProjectSettingForNode(
						currentUser, newParentId, ProjectSettingsType.upload, UploadDestinationListSetting.class);
				if (projectSetting.isPresent() && projectSettingsManager.isStsStorageLocationSetting(
						projectSetting.get())) {
					throw new IllegalArgumentException("Entities can be restored to STS-enabled folders only if " +
							"that were its original parent");
				}
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
		trashCanDao.delete(Collections.singletonList(KeyFactory.stringToKey(nodeId)));
	}

	@Override
	public List<TrashedEntity> listTrashedEntities(UserInfo currentUser, UserInfo user, long offset, long limit) {
		ValidateArgument.required(currentUser, "The current user");
		ValidateArgument.required(user, "The target user");
		ValidateArgument.requirement(offset >= 0L, "Offset cannot be < 0");
		ValidateArgument.requirement(limit >= 0L, "Limit cannot be < 0");
		
		final String currUserId = currentUser.getId().toString();
		final String userId = user.getId().toString();
		
		if (!currentUser.isAdmin() && (currUserId == null || !currUserId.equals(userId))) {
			throw new UnauthorizedException("Current user " + currUserId+ " does not have the permission.");
		}

		return trashCanDao.listTrashedEntities(userId, offset, limit);
	}
	
	@WriteTransaction
	@Override
	public void flagForPurge(UserInfo userInfo, String nodeId) throws DatastoreException, NotFoundException {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.requiredNotBlank(nodeId, "The node id");

		TrashedEntity entity = trashCanDao.getTrashedEntity(nodeId);

		if (entity == null) {
			return;
		}
		
		String userId = userInfo.getId().toString();
		String deletedBy = entity.getDeletedByPrincipalId();
		
		if (!userInfo.isAdmin() && !userId.equals(deletedBy)) {
			throw new UnauthorizedException("Insufficient permissions for user " + userId);
		}
		
		Long longId = KeyFactory.stringToKey(nodeId);
		
		trashCanDao.flagForPurge(Collections.singletonList(longId));
		
	}
	
	@WriteTransaction
	@Override
	public void purgeTrash(UserInfo userInfo, List<Long> trashIDs) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(trashIDs, "The list of ids");
		
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only an Administrator can perform this action.");
		}
	
		trashIDs.forEach(this::deleteNode);
		
		trashCanDao.delete(trashIDs);
	}
	
	@Override
	public List<Long> getTrashLeavesBefore(long numDays, long maxTrashItems) throws DatastoreException{
		return trashCanDao.getTrashLeavesIds(numDays, maxTrashItems);
	}
	
	private void deleteNode(Long nodeId) {
		boolean deleted = false;
		
		String keyId = KeyFactory.keyToString(nodeId);
		
		do {
			deleted = nodeDao.deleteTree(keyId, MAX_IDS_TO_LOAD);
		} while (!deleted);
		
		
		aclDAO.delete(keyId, ObjectType.ENTITY);
		
	}

}
