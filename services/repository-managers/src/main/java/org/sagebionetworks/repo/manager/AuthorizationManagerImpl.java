package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthorizationManagerImpl implements AuthorizationManager {
	
	@Autowired
	private NodeInheritanceDAO nodeInheritanceDAO;	
	@Autowired
	private AccessControlListDAO accessControlListDAO;	
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private ActivityDAO activityDAO;
	@Autowired
	NodeQueryDao nodeQueryDao;	
	@Autowired
	NodeDAO nodeDAO;
	@Autowired
	private UserManager userManager;

	public AuthorizationManagerImpl() {}
	
	/**
	 * For testing only
	 */
	AuthorizationManagerImpl(NodeInheritanceDAO nodeInheritanceDAO,
			AccessControlListDAO accessControlListDAO,
			AccessRequirementDAO accessRequirementDAO, ActivityDAO activityDAO,
			NodeQueryDao nodeQueryDao, NodeDAO nodeDAO, UserManager userManager) {
		super();
		this.nodeInheritanceDAO = nodeInheritanceDAO;
		this.accessControlListDAO = accessControlListDAO;
		this.accessRequirementDAO = accessRequirementDAO;
		this.activityDAO = activityDAO;
		this.nodeQueryDao = nodeQueryDao;
		this.nodeDAO = nodeDAO;
		this.userManager = userManager;
	}

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
		// if there are any unmet access requirements for Download, return false;
		Set<Long> principalIds = new HashSet<Long>();
		for (UserGroup ug : userInfo.getGroups()) {
			principalIds.add(Long.parseLong(ug.getId()));
		}
		List<Long> accessRequirementIds = accessRequirementDAO.unmetAccessRequirements(nodeId, principalIds, ACCESS_TYPE.DOWNLOAD);
		return accessRequirementIds.isEmpty();
	}
	
	@Override
	public boolean canAccess(UserInfo userInfo, final String nodeId, ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException {
		// if is an administrator, return true
		if (userInfo.isAdmin()) return true;
		{
			// if the user is the owner of the object, then she has full access to the object
			Long principalId = Long.parseLong(userInfo.getIndividualGroup().getId());
			Node node = nodeDAO.getNode(nodeId);
			if (node.getCreatedByPrincipalId().equals(principalId)) return true;
		}
		if (accessType.equals(ACCESS_TYPE.DOWNLOAD)) {
			return canDownload(userInfo, nodeId);
		}
		// must look-up access
		String permissionsBenefactor = nodeInheritanceDAO.getBenefactor(nodeId);
		return accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, accessType);
	}

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
	
	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId) throws NotFoundException, DatastoreException {
		UserEntityPermissions permission = new UserEntityPermissions();
		Node node = nodeDAO.getNode(entityId);
		permission.setOwnerPrincipalId(node.getCreatedByPrincipalId());
		boolean parentIsRoot = nodeDAO.isNodesParentRoot(entityId);
		// must look-up access (at least to determine if the anonymous user can view)
		String permissionsBenefactor = nodeInheritanceDAO.getBenefactor(entityId);
		UserInfo anonymousUser = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		permission.setCanPublicRead(this.accessControlListDAO.canAccess(anonymousUser.getGroups(), permissionsBenefactor, ACCESS_TYPE.READ));
		boolean isCreator = node.getCreatedByPrincipalId().equals(Long.parseLong(userInfo.getIndividualGroup().getId()));
		// Admin and owner/creator get all
		if (userInfo.isAdmin() || isCreator) {
			permission.setCanAddChild(true);
			permission.setCanChangePermissions(true);
			permission.setCanDelete(true);
			permission.setCanEdit(true);
			permission.setCanView(true);
			permission.setCanDownload(true);
			permission.setCanEnableInheritance(!parentIsRoot);
			return permission;
		}
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

	@Override
	public boolean canAccessActivity(UserInfo userInfo, String activityId) {
		if(userInfo.isAdmin()) return true;
		
		// check if owner
		Activity act;
		try {
			act = activityDAO.get(activityId);
			if(act.getCreatedBy().equals(userInfo.getIndividualGroup().getId()))
				return true;
		} catch (Exception e) {
			return false;
		}
		
		// check if user has read access to any in result set (could be empty)
		int limit = 1000;
		int offset = 0;
		long remaining = 1; // just to get things started
		while(remaining > 0) {			
			QueryResults<String> generatedBy = activityDAO.getEntitiesGeneratedBy(activityId, limit, offset);
			remaining = generatedBy.getTotalNumberOfResults() - (offset+limit);
			for(String nodeId : generatedBy.getResults()) {
				try {
					if(canAccess(userInfo, nodeId, ACCESS_TYPE.READ)) {
						return true;
					}
				} catch (Exception e) {
					// do nothing, same as false
				}
			}
			offset += limit; 
		}
		// no access found to generated entities, no access
		return false;
	}
}
