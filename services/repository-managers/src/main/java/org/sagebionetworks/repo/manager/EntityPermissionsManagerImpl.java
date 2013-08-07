package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EntityPermissionsManagerImpl implements EntityPermissionsManager {

	private static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfiguration.getTrashFolderEntityIdStatic());

	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDAO;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;
	@Autowired
	private UserManager userManager;

	@Override
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException, ACLInheritanceException {
		// Get the id that this node inherits its permissions from
		String benefactor = nodeInheritanceManager.getBenefactor(nodeId);		
		// This is a fix for PLFM-398
		if (!benefactor.equals(nodeId)) {
			throw new ACLInheritanceException("Cannot access the ACL of a node that inherits it permissions. This node inherits its permissions from: "+benefactor, benefactor);
		}
		AccessControlList acl = aclDAO.get(nodeId, ObjectType.ENTITY);
		return acl;
	}
		
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList updateACL(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Cannot update ACL for a resource which inherits its permissions.");
		// check permissions of user to change permissions for the resource
		if (!hasAccess(rId, CHANGE_PERMISSIONS, userInfo)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		Long ownerId = nodeDAO.getCreatedBy(acl.getId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		aclDAO.update(acl);
		acl = aclDAO.get(acl.getId(), ObjectType.ENTITY);
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList overrideInheritance(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (benefactor.equals(rId)) throw new UnauthorizedException("Resource already has an ACL.");
		// check permissions of user to change permissions for the resource
		if (!hasAccess(benefactor, CHANGE_PERMISSIONS, userInfo)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		Long ownerId = nodeDAO.getCreatedBy(acl.getId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
		Node node = nodeDAO.getNode(rId);
		// Before we can update the ACL we must grab the lock on the node.
		nodeDAO.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		// set permissions 'benefactor' for resource and all resource's descendants to resource
		nodeInheritanceManager.setNodeToInheritFromItself(rId);
		// persist acl and return
		aclDAO.create(acl);
		acl = aclDAO.get(acl.getId(), ObjectType.ENTITY);
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList restoreInheritance(String rId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// check permissions of user to change permissions for the resource
		if (!hasAccess(rId, CHANGE_PERMISSIONS, userInfo)) {
			throw new UnauthorizedException("Not authorized.");
		}
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Resource already inherits its permissions.");	

		// if parent is root, than can't inherit, must have own ACL
		if (nodeDAO.isNodesParentRoot(rId)) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");

		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDAO.getNode(rId);
		nodeDAO.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		nodeInheritanceManager.setNodeToInheritFromNearestParent(rId);
		
		// delete access control list
		AccessControlList acl = aclDAO.get(rId, ObjectType.ENTITY);
		aclDAO.delete(acl.getId());
		
		// now find the newly governing ACL
		benefactor = nodeInheritanceManager.getBenefactor(rId);
		
		return aclDAO.get(benefactor, ObjectType.ENTITY);
	}	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList applyInheritanceToChildren(String parentId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// check permissions of user to change permissions for the resource
		if (!hasAccess(parentId,CHANGE_PERMISSIONS, userInfo)) {
			throw new UnauthorizedException("Not authorized.");
		}
		
		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDAO.getNode(parentId);
		nodeDAO.lockNodeAndIncrementEtag(node.getId(), node.getETag());

		String benefactorId = nodeInheritanceManager.getBenefactor(parentId);
		applyInheritanceToChildrenHelper(parentId, benefactorId, userInfo);

		// return governing parent ACL
		return aclDAO.get(nodeInheritanceManager.getBenefactor(parentId), ObjectType.ENTITY);
	}
	
	private void applyInheritanceToChildrenHelper(final String parentId, final String benefactorId, UserInfo userInfo)
			throws NotFoundException, DatastoreException, ConflictingUpdateException {
		// Get all of the child nodes, sorted by id (to prevent deadlock)
		List<String> children = nodeDAO.getChildrenIdsAsList(parentId);
		// Update each node
		for(String idToChange: children) {
			// recursively apply to children
			applyInheritanceToChildrenHelper(idToChange, benefactorId, userInfo);
			// must be authorized to modify permissions
			if (hasAccess(idToChange, CHANGE_PERMISSIONS, userInfo)) {
				// delete child ACL, if present
				if (hasLocalACL(idToChange)) {
					// Before we can update the ACL we must grab the lock on the node.
					Node node = nodeDAO.getNode(idToChange);
					nodeDAO.lockNodeAndIncrementEtag(node.getId(), node.getETag());
					
					// delete ACL
					AccessControlList acl = aclDAO.get(idToChange, ObjectType.ENTITY);
					aclDAO.delete(acl.getId());
				}								
				// set benefactor ACL
				nodeInheritanceManager.addBeneficiary(idToChange, benefactorId);
			}
		}
	}

	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	@Override
	public boolean hasAccess(String entityId, ACCESS_TYPE accessType, UserInfo userInfo)
			throws NotFoundException, DatastoreException  {
		// In the case of the trash can, throw the EntityInTrashCanException
		// The only operations allowed over the trash can is CREATE (i.e. moving
		// items into the trash can) and DELETE (i.e. purging the trash).
		final String benefactor = nodeInheritanceManager.getBenefactor(entityId);
		if (TRASH_FOLDER_ID.equals(KeyFactory.stringToKey(benefactor))
				&& !CREATE.equals(accessType)
				&& !DELETE.equals(accessType)) {
			throw new EntityInTrashCanException("Entity " + entityId + " is in trash can.");
		}
		// Can download
		if (accessType == DOWNLOAD) {
			return canDownload(userInfo, entityId);
		}
		// Anonymous can at most READ
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			if (accessType != ACCESS_TYPE.READ) {
				return false;
			}
		}
		// Admin
		if (userInfo.isAdmin()) {
			return true;
		}
		return aclDAO.canAccess(userInfo.getGroups(), benefactor, accessType);
	}

	/**
	 * Get the permission benefactor of an entity.
	 * @throws DatastoreException 
	 */
	@Override
	public String getPermissionBenefactor(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException {
		return nodeInheritanceManager.getBenefactor(nodeId);
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId)
			throws NotFoundException, DatastoreException {

		final String benefactor = nodeInheritanceManager.getBenefactor(entityId);
		UserEntityPermissions permissions = new UserEntityPermissions();
		permissions.setCanAddChild(hasAccess(benefactor, CREATE, userInfo));
		permissions.setCanChangePermissions(hasAccess(benefactor, CHANGE_PERMISSIONS, userInfo));
		permissions.setCanDelete(hasAccess(benefactor, DELETE, userInfo));
		permissions.setCanEdit(hasAccess(benefactor, UPDATE, userInfo));
		permissions.setCanView(hasAccess(benefactor, READ, userInfo));
		permissions.setCanDownload(canDownload(userInfo, entityId));

		Node node = nodeDAO.getNode(entityId);
		permissions.setOwnerPrincipalId(node.getCreatedByPrincipalId());

		UserInfo anonymousUser = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		permissions.setCanPublicRead(hasAccess(benefactor, READ, anonymousUser));

		final boolean parentIsRoot = nodeDAO.isNodesParentRoot(entityId);
		if (userInfo.isAdmin()) {
			permissions.setCanEnableInheritance(!parentIsRoot);
		} else if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			permissions.setCanEnableInheritance(false);
		} else {
			permissions.setCanEnableInheritance(!parentIsRoot && permissions.getCanChangePermissions());
		}
		return permissions;
	}

	@Override
	public boolean hasLocalACL(String resourceId) {
		try {
			return nodeInheritanceManager.getBenefactor(resourceId).equals(resourceId);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean canDownload(UserInfo userInfo, final String nodeId)
			throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return true;
		if (!agreesToTermsOfUse(userInfo)) return false;
		
		// if there are any unmet access requirements return false
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(nodeId);
		rod.setType(RestrictableObjectType.ENTITY);
		List<Long> accessRequirementIds = AccessRequirementUtil.unmetAccessRequirementIds(
				userInfo, rod, nodeDAO, accessRequirementDAO);
		return accessRequirementIds.isEmpty();
	}

	private static boolean agreesToTermsOfUse(UserInfo userInfo) {
		User user = userInfo.getUser();
		if (user==null) return false;
		// can't agree if you are anonymous
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(user.getUserId())) return false;
		return user.isAgreesToTermsOfUse();
	}
}
