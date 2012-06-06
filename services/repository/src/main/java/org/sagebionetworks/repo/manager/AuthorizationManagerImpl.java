package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class AuthorizationManagerImpl implements AuthorizationManager {
	
	@Autowired
	private NodeInheritanceDAO nodeInheritanceDAO;
	
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
	@Autowired
	NodeQueryDao nodeQueryDao;
	
	@Autowired
	NodeDAO nodeDAO;


	private static boolean agreesToTermsOfUse(UserInfo userInfo) {
		User user = userInfo.getUser();
		if (user==null) return false;
		// can't agree if you are anonymous
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(user.getUserId())) return false;
		return user.isAgreesToTermsOfUse();
	}
	
	private boolean canDownload(UserInfo userInfo, final String nodeId) throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return true;
		if (!agreesToTermsOfUse(userInfo)) return false;
		// TODO node-specific checks to see if download is allowed:
		// Tier 2:
		// 1) if the node is a dataset, see if it has a EULA
		// if its parent is a dataset, see if the dataset has a EULA
		// (TODO this should probably be generalized to something like a 'Download permissions benefactor')
		//
		// 2) If so, see if there is an Agreement (a) pointing to the EULA and (b) dataset, (c) *owned* by the current user
		//	If there is no such Agreement, then do not allow the download
		//
		// Tier 3:
		// 3) Check to see if there is an ApprovalNeeded object.  The object will refer to an ACT UserGroup
		// 4) If so, see if there is an Approval (a) pointing to the ACT UserGroup and (b) dataset, (c) owned by the current user
		// If there is no such approval, then do not allow the download
		//
		return true;
	}
	
	/**
	 * 
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff the given group has the given access to the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	@Transactional(readOnly = true)
	@Override
	public boolean canAccess(UserInfo userInfo, final String nodeId, ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException {
		// if is an administrator, return true
		if (userInfo.isAdmin()) return true;
		if (accessType.equals(ACCESS_TYPE.DOWNLOAD)) {
			return canDownload(userInfo, nodeId);
		}
		{
			// if the user is the owner of the object, then she has full access to the object
			// (note, this does not include 'download' access, handled above
			Long principalId = Long.parseLong(userInfo.getIndividualGroup().getId());
			Node node = nodeDAO.getNode(nodeId);
			if (node.getCreatedByPrincipalId().equals(principalId)) return true;
		}
		// must look-up access
		String permissionsBenefactor = nodeInheritanceDAO.getBenefactor(nodeId);
		return accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, accessType);
	}

	/**
     *
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff either (1) the user has 'add child' access to the parent or (2) parent is null
	 * and user is admin
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	@Transactional(readOnly = true)
	@Override
	public boolean canCreate(UserInfo userInfo, final Node node) 
		throws NotFoundException, DatastoreException {
		// if is an administrator, return true
		if (userInfo.isAdmin()) return true;
		// must look-up access
		String parentId = node.getParentId();
		if (parentId==null) return false; // if not an admin, can't do it!
		String permissionsBenefactor = nodeInheritanceDAO.getBenefactor(parentId);
		return accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.CREATE);
	}
	
	/**
	 * @param n the number of items in the group-id list
	 * 
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	@Override
	public String authorizationSQL(int n) {
		return accessControlListDAO.authorizationSQL(n);
	}
	
	@Transactional(readOnly = true)
	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId) throws NotFoundException, DatastoreException {
		UserEntityPermissions permission = new UserEntityPermissions();
		Node node = nodeDAO.getNode(entityId);
		permission.setOwnerPrincipalId(node.getCreatedByPrincipalId());
		boolean parentIsRoot = nodeDAO.isNodesParentRoot(entityId);
		
		// Admin gets all
		if (userInfo.isAdmin()) {
			permission.setCanAddChild(true);
			permission.setCanChangePermissions(true);
			permission.setCanDelete(true);
			permission.setCanEdit(true);
			permission.setCanView(true);
			permission.setCanDownload(true);
			permission.setCanEnableInheritance(!parentIsRoot);
			return permission;
		}
		// must look-up access
		String permissionsBenefactor = nodeInheritanceDAO.getBenefactor(entityId);
		// Child can be added if this entity is not null
		permission.setCanAddChild(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.CREATE));
		permission.setCanChangePermissions(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.CHANGE_PERMISSIONS));
		permission.setCanDelete(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.DELETE));
		permission.setCanEdit(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.UPDATE));
		permission.setCanView(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.READ));
		permission.setCanDownload(this.canDownload(userInfo, entityId));
		permission.setCanEnableInheritance(!parentIsRoot && permission.getCanChangePermissions());
		return permission;
	}
}
