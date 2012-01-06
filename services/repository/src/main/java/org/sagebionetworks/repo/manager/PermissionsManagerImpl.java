package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class PermissionsManagerImpl implements PermissionsManager {
	
	
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

	@Transactional(readOnly = true)
	@Override
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException, ACLInheritanceException {
		// get the id that this node inherits its permissions from
		String benefactor = nodeInheritanceManager.getBenefactor(nodeId);
		// This is a fix for PLFM-398.
		if(!benefactor.equals(nodeId)){
			// Look up the type of the benefactor
			EntityHeader header = nodeDao.getEntityHeader(benefactor);
			EntityType type = EntityType.getFirstTypeInUrl(header.getType());
			throw new ACLInheritanceException("Cannot access the ACL of a node that inherits it permissions. This node inherits its permissions from: "+benefactor,type, benefactor);
		}
		return aclDAO.getForResource(nodeId);
	}
	
	public void validateContent(AccessControlList acl) throws InvalidModelException {
		if (acl.getId()==null) throw new InvalidModelException("Resource ID is null");
		if(acl.getResourceAccess() == null){
			acl.setResourceAccess(new HashSet<ResourceAccess>());
		}
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra==null) throw new InvalidModelException("ACL row is null.");
			if (ra.getGroupName()==null) throw new InvalidModelException("Group ID is null");
			if (ra.getAccessType().isEmpty()) throw new InvalidModelException("No access types specified.");
		}

		if(acl.getCreatedBy() == null ) throw new InvalidModelException("CreatedBy is null");
		
		if(acl.getModifiedBy() == null ) throw new InvalidModelException("ModifiedBy is null");
		
		// If createdBy is not set then set it with the current time.
		if(acl.getCreationDate() == null){
			acl.setCreationDate(new Date(System.currentTimeMillis()));
		}
		// If modifiedOn is not set then set it with the current time.
		if(acl.getModifiedOn() == null){
			acl.setModifiedOn(new Date(System.currentTimeMillis()));
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList updateACL(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Cannot update ACL for a resource which inherits its permissions.");
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, rId, AuthorizationConstants.ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		validateContent(acl);
		// Before we can update the ACL we must grab the lock on the node.
		String newETag = nodeDao.lockNodeAndIncrementEtag(acl.getId(), acl.getEtag());
		aclDAO.update(acl);
		acl = aclDAO.get(acl.getId());
		acl.setEtag(newETag);
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList overrideInheritance(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (benefactor.equals(rId)) throw new UnauthorizedException("Resource already has an ACL.");
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, benefactor, AuthorizationConstants.ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		if (acl.getCreatedBy()==null) acl.setCreatedBy(userInfo.getUser().getUserId());
		if (acl.getModifiedBy()==null) acl.setModifiedBy(userInfo.getUser().getUserId());
		validateContent(acl);
		Node node = nodeDao.getNode(rId);
		// Before we can update the ACL we must grab the lock on the node.
		String newEtag = nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		// set permissions 'benefactor' for resource and all resource's descendants to resource
		nodeInheritanceManager.setNodeToInheritFromItself(rId);
		// persist acl and return
		String id = aclDAO.create(acl);
		acl = aclDAO.get(acl.getId());
		acl.setEtag(newEtag);
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList restoreInheritance(String rId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, rId, AuthorizationConstants.ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Resource already inherits its permissions.");	
		// get parent; if null then I cannot continue!!
		Node node = nodeDao.getNode(rId);
		if (node.getParentId()==null) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");

		// Before we can update the ACL we must grab the lock on the node.
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		nodeInheritanceManager.setNodeToInheritFromNearestParent(rId);
		
		// delete access control list
		AccessControlList acl = aclDAO.getForResource(rId);
		aclDAO.delete(acl.getId());
		
		// now find the newly governing ACL
		benefactor = nodeInheritanceManager.getBenefactor(rId);
		
		return aclDAO.getForResource(benefactor);
	}
	
	private void requireUser(UserInfo userInfo) throws UnauthorizedException {
		if(userInfo.getUser().getUserId().equalsIgnoreCase(AuthorizationConstants.ANONYMOUS_USER_ID))
			throw new UnauthorizedException("Anonymous user cannot retrieve group information.");
	}

	@Override
	public Collection<UserGroup> getGroups(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		requireUser(userInfo);
		return userGroupDAO.getAll(false);
	}

	@Override
	public Collection<UserGroup> getIndividuals(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		requireUser(userInfo);
		return userGroupDAO.getAll(true);
	}

	@Override
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException {
		requireUser(userInfo);
		return userGroupDAO.getInRange(startIncl, endExcl, false);
	}

	@Override
	public List<UserGroup> getIndividualsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException {
		requireUser(userInfo);
		return userGroupDAO.getInRange(startIncl, endExcl, true);
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
	public boolean hasAccess(String resourceId, AuthorizationConstants.ACCESS_TYPE accessType, UserInfo userInfo) throws NotFoundException, DatastoreException  {
		return authorizationManager.canAccess(userInfo, resourceId, accessType);
	}

	/**
	 * Get the permission benefactor of an entity.
	 * @throws DatastoreException 
	 */
	@Override
	public String getPermissionBenefactor(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException {
		return nodeInheritanceManager.getBenefactor(nodeId);
	}

	
}
