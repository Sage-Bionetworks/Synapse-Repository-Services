package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
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
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.message.ObjectType;
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
	EvaluationDAO evaluationDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	EvaluationManager evaluationManager;
	@Autowired
	FileHandleDao fileHandleDao;

	public AuthorizationManagerImpl() {}
	
	/**
	 * For testing only
	 */
	AuthorizationManagerImpl(NodeInheritanceDAO nodeInheritanceDAO,
			AccessControlListDAO accessControlListDAO,
			AccessRequirementDAO accessRequirementDAO, ActivityDAO activityDAO,
			NodeQueryDao nodeQueryDao, NodeDAO nodeDAO, UserManager userManager, 
			EvaluationManager competitionManager, FileHandleDao fileHandleDao, 
			EvaluationDAO evaluationDAO) {
		super();
		this.nodeInheritanceDAO = nodeInheritanceDAO;
		this.accessControlListDAO = accessControlListDAO;
		this.accessRequirementDAO = accessRequirementDAO;
		this.activityDAO = activityDAO;
		this.nodeQueryDao = nodeQueryDao;
		this.nodeDAO = nodeDAO;
		this.userManager = userManager;
		this.evaluationManager = competitionManager;
		this.fileHandleDao = fileHandleDao;
		this.evaluationDAO = evaluationDAO;
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
		
		// if there are any unmet access requirements return false
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(nodeId);
		rod.setType(RestrictableObjectType.ENTITY);
		List<Long> accessRequirementIds = 
			AccessRequirementUtil.unmetAccessRequirementIds(userInfo, rod, nodeDAO, evaluationDAO, accessRequirementDAO);
		return accessRequirementIds.isEmpty();
	}
	
	private boolean canParticipate(UserInfo userInfo, final String evaluationId) throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return true;
		
		// if there are any unmet access requirements return false
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(evaluationId);
		rod.setType(RestrictableObjectType.EVALUATION);
		List<Long> accessRequirementIds = 
			AccessRequirementUtil.unmetAccessRequirementIds(userInfo, rod, nodeDAO, evaluationDAO, accessRequirementDAO);
		return accessRequirementIds.isEmpty();
	}
	
	@Override
	public boolean canAccess(UserInfo userInfo, final String nodeId, ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException {
		// if is an administrator, return true
		if (userInfo.isAdmin()) return true;
		// anonymous can only READ (if that!)
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			if (ACCESS_TYPE.READ!=accessType) return false;
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
		// if anonymous, cannot do it
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) return false;
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
		permission.setCanView(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.READ));
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			permission.setCanAddChild(false);
			permission.setCanChangePermissions(false);
			permission.setCanDelete(false);
			permission.setCanEdit(false);
			permission.setCanDownload(false);
			permission.setCanEnableInheritance(false);
		} else {
			// Child can be added if this entity is not null
			permission.setCanAddChild(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.CREATE));
			permission.setCanChangePermissions(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.CHANGE_PERMISSIONS));
			permission.setCanDelete(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.DELETE));
			permission.setCanEdit(this.accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, ACCESS_TYPE.UPDATE));
			permission.setCanDownload(this.canDownload(userInfo, entityId));
			permission.setCanEnableInheritance(!parentIsRoot && permission.getCanChangePermissions());
		}
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
			PaginatedResults<Reference> generatedBy = activityDAO.getEntitiesGeneratedBy(activityId, limit, offset);
			remaining = generatedBy.getTotalNumberOfResults() - (offset+limit);
			for(Reference ref : generatedBy.getResults()) {
				String nodeId = ref.getTargetId();
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

	@Override
	public boolean canAccessRawFileHandleByCreator(UserInfo userInfo, String creator) {
		// Admins can see anything.
		if (userInfo.isAdmin()) return true;
		// Only the creator can see the raw file handle
		return userInfo.getIndividualGroup().getId().equals(creator);
	}

	@Override
	public boolean canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType) throws DatastoreException, NotFoundException {
		// Admins can do anything
		if(userInfo.isAdmin()) return true;
		// If the object is an entity then we use existing methods
		if(ObjectType.ENTITY == objectType){
			return canAccess(userInfo, objectId, accessType);
		}else if(ObjectType.EVALUATION == objectType){
			// Anyone can read from a competition.
			if(ACCESS_TYPE.READ == accessType){
				return true;
			} else if (ACCESS_TYPE.PARTICIPATE == accessType) {
				// look up unfulfilled access requirements
				return canParticipate(userInfo, objectId);
			} else {
				// All other actions require admin access
				return evaluationManager.isEvalAdmin(userInfo, objectId);
			}
		}else{
			throw new IllegalArgumentException("Unknown ObjectType: "+objectType);
		}
	}

	@Override
	public boolean canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException {
		// Admins can do anything
		if(userInfo.isAdmin()) return true;
		// Lookup the creator by
		String creator  = fileHandleDao.getHandleCreator(fileHandleId);
		// Call the other methods
		return canAccessRawFileHandleByCreator(userInfo, creator);
	}
}
