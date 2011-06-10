package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

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

	@Override
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException {
		// get the id that this node inherits its permissions from
		String benefactor = nodeInheritanceManager.getBenefactor(nodeId);
		return aclDAO.getForResource(benefactor);
	}
	
	public void validateContent(AccessControlList acl) throws InvalidModelException {
		if (acl.getResourceId()==null) throw new InvalidModelException("Resource ID is null");
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra==null) throw new InvalidModelException("ACL row is null.");
			if (ra.getUserGroupId()==null) throw new InvalidModelException("Group ID is null");
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

	@Override
	public AccessControlList updateACL(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException {
		String rId = acl.getResourceId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Cannot update ACL for a resource which inherits its permissions.");
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, rId, AuthorizationConstants.ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		validateContent(acl);
		aclDAO.update(acl);
		return acl;
	}

	@Override
	public AccessControlList overrideInheritance(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException {
		String rId = acl.getResourceId();
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
		// set permissions 'benefactor' for resource and all resource's descendants to resource
		nodeInheritanceManager.setNodeToInheritFromItself(rId);
		// persist acl and return
		String id = aclDAO.create(acl);
		acl.setId(id);
		return acl;
	}

	@Override
	public AccessControlList restoreInheritance(String rId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException {
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, rId, AuthorizationConstants.ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Resource already inherits its permissions.");	
		// get parent; if null then I cannot continue!!
		Node node = nodeDao.getNode(rId);
		if (node.getParentId()==null) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");

		nodeInheritanceManager.setNodeToInheritFromNearestParent(rId);
		
		// delete access control list
		AccessControlList acl = aclDAO.getForResource(rId);
		aclDAO.delete(acl.getId());
		
		// now find the newly governing ACL
		benefactor = nodeInheritanceManager.getBenefactor(rId);
		
		return aclDAO.getForResource(benefactor);
	}

	@Override
	public Collection<UserGroup> getGroups() throws DatastoreException {
		return userGroupDAO.getAll(false);
	}

	@Override
	public Collection<UserGroup> getIndividuals() throws DatastoreException {
		return userGroupDAO.getAll(true);
	}

	@Override
	public List<UserGroup> getGroupsInRange(long startIncl, long endExcl) throws DatastoreException {
		return userGroupDAO.getInRange(startIncl, endExcl, false);
	}

	@Override
	public List<UserGroup> getIndividualsInRange(long startIncl, long endExcl) throws DatastoreException {
		return userGroupDAO.getInRange(startIncl, endExcl, true);
	}

	
}
