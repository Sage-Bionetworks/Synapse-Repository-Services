package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EntityPermissionsManagerImpl implements EntityPermissionsManager {

	@Autowired
	private AccessControlListDAO aclDAO;	
	@Autowired
	private AuthorizationManager authorizationManager;	
	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;	
	@Autowired
	NodeDAO nodeDao;	
	@Autowired
	private UserGroupDAO userGroupDAO;	
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
		if (!authorizationManager.canAccess(userInfo, rId, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		Long ownerId = nodeDao.getCreatedBy(acl.getId());
		validateACLContent(acl, userInfo, ownerId);
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
		if (!authorizationManager.canAccess(userInfo, benefactor, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		Long ownerId = nodeDao.getCreatedBy(acl.getId());
		validateACLContent(acl, userInfo, ownerId);
		Node node = nodeDao.getNode(rId);
		// Before we can update the ACL we must grab the lock on the node.
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
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
		if (!authorizationManager.canAccess(userInfo, rId, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Resource already inherits its permissions.");	

		// if parent is root, than can't inherit, must have own ACL
		if (nodeDao.isNodesParentRoot(rId)) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");

		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDao.getNode(rId);
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
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
		if (!authorizationManager.canAccess(userInfo, parentId, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		
		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDao.getNode(parentId);
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());

		applyInheritanceToChildrenHelper(parentId, userInfo);

		// return governing parent ACL
		return aclDAO.get(nodeInheritanceManager.getBenefactor(parentId), ObjectType.ENTITY);
	}
	
	private void applyInheritanceToChildrenHelper(String parentId, UserInfo userInfo) throws NotFoundException, DatastoreException, ConflictingUpdateException {
		// Get all of the child nodes, sorted by id (to prevent deadlock)
		List<String> children = nodeDao.getChildrenIdsAsList(parentId);
		
		// Update each node
		for(String idToChange: children) {
			String benefactorId = nodeInheritanceManager.getBenefactor(parentId);
			
			// must be authorized to modify permissions
			if (authorizationManager.canAccess(userInfo, idToChange, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
				// delete child ACL, if present
				if (hasLocalACL(idToChange)) {
					// Before we can update the ACL we must grab the lock on the node.
					Node node = nodeDao.getNode(idToChange);
					nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
					
					// delete ACL
					AccessControlList acl = aclDAO.get(idToChange, ObjectType.ENTITY);
					aclDAO.delete(acl.getId());
				}								
				// set benefactor ACL
				nodeInheritanceManager.addBeneficiary(idToChange, benefactorId);
				
				// recursively apply to children
				applyInheritanceToChildrenHelper(idToChange, userInfo);
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
	public boolean hasAccess(String resourceId, ACCESS_TYPE accessType, UserInfo userInfo) throws NotFoundException, DatastoreException  {
		return authorizationManager.canAccess(userInfo, resourceId, accessType);
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
	public boolean hasAccess(String resourceId, ObjectType objectType, ACCESS_TYPE accessType, UserInfo userInfo) throws NotFoundException, DatastoreException  {
		return authorizationManager.canAccess(userInfo, resourceId, objectType, accessType);
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
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId) throws NotFoundException, DatastoreException {
		// pass it along the permission object
		return authorizationManager.getUserPermissionsForEntity(userInfo, entityId);
	}

	@Override
	public boolean hasLocalACL(String resourceId) {
		try {
			return nodeInheritanceManager.getBenefactor(resourceId).equals(resourceId);
		} catch (Exception e) {
			return false;
		}
	}

	public static void validateACLContent(AccessControlList acl, UserInfo userInfo, Long ownerId) throws InvalidModelException {
		if (acl.getId()==null) 
			throw new InvalidModelException("Resource ID is null");
		if(acl.getResourceAccess() == null) 
			acl.setResourceAccess(new HashSet<ResourceAccess>());
		if(acl.getCreationDate() == null) 
			acl.setCreationDate(new Date(System.currentTimeMillis()));
		
		// Verify that the caller maintains permissions access
		String callerPrincipalId = userInfo.getIndividualGroup().getId();
		boolean callerIsOwner = callerPrincipalId.equals(ownerId.toString());
		boolean foundCallerInAcl = false;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra==null) throw new InvalidModelException("ACL row is null.");
			if (ra.getPrincipalId()==null) throw new InvalidModelException("Group ID is null");
			if (ra.getAccessType().isEmpty()) throw new InvalidModelException("No access types specified.");
			if (ra.getPrincipalId().toString().equals(callerPrincipalId)) { 
				if (ra.getAccessType().contains(ACCESS_TYPE.CHANGE_PERMISSIONS)) {
					// Found caller in the ACL, with access to change permissions
					foundCallerInAcl = true;
				}
			}
		}
		
		if (!foundCallerInAcl && !userInfo.isAdmin() && !callerIsOwner) {
			throw new InvalidModelException("Caller is trying to revoke their own ACL editing permissions.");
		}
	}
}
