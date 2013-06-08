package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestricableODUtil;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
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
	private AccessApprovalDAO  accessApprovalDAO;
	@Autowired
	private ActivityDAO activityDAO;
	@Autowired
	NodeQueryDao nodeQueryDao;	
	@Autowired
	NodeDAO nodeDAO;
	@Autowired
	UserGroupDAO userGroupDAO;
	@Autowired
	EvaluationDAO evaluationDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	FileHandleDao fileHandleDao;

	public AuthorizationManagerImpl() {}
	
	/**
	 * For testing only
	 */
	AuthorizationManagerImpl(NodeInheritanceDAO nodeInheritanceDAO,
			AccessControlListDAO accessControlListDAO,
			AccessRequirementDAO accessRequirementDAO, 
			AccessApprovalDAO accessApprovalDAO, 
			ActivityDAO activityDAO,
			NodeQueryDao nodeQueryDao, 
			NodeDAO nodeDAO, 
			UserManager userManager, 
			FileHandleDao fileHandleDao, 
			EvaluationDAO evaluationDAO,
			UserGroupDAO userGroupDAO) {
		super();
		this.nodeInheritanceDAO = nodeInheritanceDAO;
		this.accessControlListDAO = accessControlListDAO;
		this.accessRequirementDAO = accessRequirementDAO;
		this.accessApprovalDAO = accessApprovalDAO;
		this.activityDAO = activityDAO;
		this.nodeQueryDao = nodeQueryDao;
		this.nodeDAO = nodeDAO;
		this.userManager = userManager;
		this.fileHandleDao = fileHandleDao;
		this.evaluationDAO = evaluationDAO;
		this.userGroupDAO = userGroupDAO;
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
	public boolean isUserCreatorOrAdmin(UserInfo userInfo, String creator) {
		// Admins can see anything.
		if (userInfo.isAdmin()) return true;
		// Only the creator can see the raw file handle
		return userInfo.getIndividualGroup().getId().equals(creator);
	}

	@Override
	public boolean canAccessRawFileHandleByCreator(UserInfo userInfo, String creator) {
		return isUserCreatorOrAdmin(userInfo, creator);
	}

	@Override
	public boolean canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType) throws DatastoreException, NotFoundException {
		// Admins can do anything
		if(userInfo.isAdmin()) return true;
		// If the object is an entity then we use existing methods
		if(ObjectType.ENTITY == objectType){
			return canAccess(userInfo, objectId, accessType);
		}else if (ObjectType.EVALUATION == objectType){
			// Anyone can read from a competition.
			if (ACCESS_TYPE.READ == accessType) {
				return true;
			} else if (ACCESS_TYPE.PARTICIPATE == accessType) {
				// look up unfulfilled access requirements
				return canParticipate(userInfo, objectId);
			} else {
				// All other actions require admin access
				Evaluation evaluation = evaluationDAO.get(objectId);
				return EvaluationUtil.isEvalAdmin(userInfo, evaluation);
			}
		} else if (ObjectType.ACCESS_REQUIREMENT==objectType) {
			AccessRequirement accessRequirement = accessRequirementDAO.get(objectId);
			return canAdminAccessRequirement(userInfo, accessRequirement);
		} else if (ObjectType.ACCESS_APPROVAL==objectType) {
			AccessApproval accessApproval = accessApprovalDAO.get(objectId);
			return canAdminAccessApproval(userInfo, accessApproval);
		} else {
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

	@Override
	public boolean canCreateAccessRequirement(UserInfo userInfo,
			AccessRequirement accessRequirement) throws NotFoundException {
		 Map<RestrictableObjectType, Collection<String>> sortedIds = 
			 RestricableODUtil.sortByType(accessRequirement.getSubjectIds());
		Collection<String> entityIds = sortedIds.get(RestrictableObjectType.ENTITY);
		if (entityIds!=null && entityIds.size()>0) {
			if (!isACTTeamMemberOrCanCreateOrEdit(userInfo, entityIds)) return false;
		}
		Collection<String> evaluationIds = sortedIds.get(RestrictableObjectType.EVALUATION);
		if (evaluationIds!=null && evaluationIds.size()>0) {
			if (!canAdministerEvaluation(userInfo, evaluationIds, evaluationDAO)) return false;
		}
		return true;
	}
	
	public boolean isACTTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) return true;
		UserGroup actTeam = userGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false);
		return userInfo.getGroups().contains(actTeam);
	}

	public boolean isACTTeamMemberOrCanCreateOrEdit(UserInfo userInfo, Collection<String> entityIds) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userInfo)) {
			return true;
		}
		if (entityIds.size()==0) return false;
		if (entityIds.size()>1) return false;
		String entityId = entityIds.iterator().next();
		if (!canAccess(userInfo, entityId, ACCESS_TYPE.CREATE) &&
				!canAccess(userInfo, entityId, ACCESS_TYPE.UPDATE)) return false;
		return true;
	}

	/**
	 * For Entities, check that user is an administrator or ACT member. 
	 * For Evaluations, check that user is an administrator or is the creator of the Evaluation
	 * @param userInfo
	 * @param accessRequirement
	 * @throws NotFoundException
	 */
	private boolean canAdminAccessRequirement(UserInfo userInfo, AccessRequirement accessRequirement) throws NotFoundException {
		Map<RestrictableObjectType, Collection<String>> sortedIds = 
			 RestricableODUtil.sortByType(accessRequirement.getSubjectIds());
		Collection<String> entityIds = sortedIds.get(RestrictableObjectType.ENTITY);
		if (entityIds!=null && !entityIds.isEmpty()) {
			if (!isACTTeamMemberOrAdmin(userInfo)) return false;
		}
		Collection<String> evaluationIds = sortedIds.get(RestrictableObjectType.EVALUATION);
		if (evaluationIds!=null && !evaluationIds.isEmpty()) {
			if (!canAdministerEvaluation(userInfo, evaluationIds, evaluationDAO)) return false;
		}	
		return true;
	}
	
	private boolean canAdminAccessApproval(UserInfo userInfo, AccessApproval accessApproval) throws NotFoundException {
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessApproval.getRequirementId().toString());
		return canAdminAccessRequirement(userInfo, accessRequirement);
	}

	/**
	 * check that user is an administrator or is the creator of the Evaluation
	 * @param userInfo
	 * @param evaluationIds
	 * @param evaluationDAO
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	private static boolean canAdministerEvaluation(
			UserInfo userInfo, 
			Collection<String> evaluationIds, 
			EvaluationDAO evaluationDAO) throws NotFoundException, UnauthorizedException {
		if (userInfo.isAdmin()) return true;
		for (String id : evaluationIds) {
			Evaluation evaluation = evaluationDAO.get(id);
			if (!EvaluationUtil.isEvalAdmin(userInfo, evaluation)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean canCreateAccessApproval(UserInfo userInfo,
			AccessApproval accessApproval) {
		if ((accessApproval instanceof ACTAccessApproval)) {
			return isACTTeamMemberOrAdmin(userInfo);
		} else if (accessApproval instanceof TermsOfUseAccessApproval) {
			return true;
		} else {
			throw new IllegalArgumentException("Unrecognized type: "+accessApproval.getEntityType());
		}
	}

	@Override
	public boolean canAccessAccessApprovalsForSubject(UserInfo userInfo,
			RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException {
		if (RestrictableObjectType.ENTITY.equals(subjectId.getType())) {
			if (!(isACTTeamMemberOrAdmin(userInfo))) return false;
		} else if (RestrictableObjectType.EVALUATION.equals(subjectId.getType())) {
			Evaluation evaluation = evaluationDAO.get(subjectId.getId());
			if (!EvaluationUtil.isEvalAdmin(userInfo, evaluation)) {
				return false;
			}
		} else {
			throw new NotFoundException("Unexpected object type: "+subjectId.getType());
		}
		return true;
	}
	
}
