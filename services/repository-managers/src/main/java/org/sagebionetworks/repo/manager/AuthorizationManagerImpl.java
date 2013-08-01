package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
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
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
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
	private NodeQueryDao nodeQueryDao;	
	@Autowired
	private NodeDAO nodeDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private EvaluationDAO evaluationDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private FileHandleDao fileHandleDao;

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

	@Override
	public boolean canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType)
			throws DatastoreException, NotFoundException {
		// admins can do anything
		if (userInfo.isAdmin()) {
			return true;
		}		
		// anonymous can at most READ
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId())) {
			if (accessType != ACCESS_TYPE.READ) return false;
		}
		
		switch (objectType) {
			case ENTITY:				
				if (accessType == ACCESS_TYPE.DOWNLOAD) {
					return canDownload(userInfo, objectId);
				}
				// refer to ACL
				String permissionsBenefactor = nodeInheritanceDAO.getBenefactor(objectId);
				return accessControlListDAO.canAccess(userInfo.getGroups(), permissionsBenefactor, accessType);
			case EVALUATION:
				return evaluationPermissionsManager.hasAccess(userInfo, objectId, accessType);
			case ACCESS_REQUIREMENT:
				AccessRequirement accessRequirement = accessRequirementDAO.get(objectId);
				return canAdminAccessRequirement(userInfo, accessRequirement);
			case ACCESS_APPROVAL:
				AccessApproval accessApproval = accessApprovalDAO.get(objectId);
				return canAdminAccessApproval(userInfo, accessApproval);
			default:
				throw new IllegalArgumentException("Unknown ObjectType: "+objectType);
		}
	}

	private static boolean isEvalOwner(UserInfo userInfo, Evaluation evaluation) {
		return evaluation.getOwnerId().equals(userInfo.getIndividualGroup().getId());
	}

	@Override
	public boolean canAccess(UserInfo userInfo, final String nodeId, ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException {
		return canAccess(userInfo, nodeId, ObjectType.ENTITY, accessType);
	}

	@Override
	public boolean canCreate(UserInfo userInfo, final Node node) 
		throws NotFoundException, DatastoreException {
		String parentId = node.getParentId();
		if (parentId==null && !userInfo.isAdmin()) return false; // if not an admin, can't do it!
		return canAccess(userInfo, parentId, ACCESS_TYPE.CREATE);
	}

	private boolean canDownload(UserInfo userInfo, final String nodeId) throws DatastoreException, NotFoundException {
		if (userInfo.isAdmin()) return true;
		if (!agreesToTermsOfUse(userInfo)) return false;
		
		// if there are any unmet access requirements return false
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(nodeId);
		rod.setType(RestrictableObjectType.ENTITY);
		List<Long> accessRequirementIds = 
			AccessRequirementUtil.unmetAccessRequirementIds(userInfo, rod, nodeDAO, accessRequirementDAO);
		return accessRequirementIds.isEmpty();
	}

	/**
	 * @param n the number of items in the group-id list
	 * 
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	@Override
	public String authorizationSQL(int n) {
			return AuthorizationSqlUtil.authorizationSQL(n);
	}
	
	private static boolean agreesToTermsOfUse(UserInfo userInfo) {
		User user = userInfo.getUser();
		if (user==null) return false;
		// can't agree if you are anonymous
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(user.getUserId())) return false;
		return user.isAgreesToTermsOfUse();
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId)
			throws NotFoundException, DatastoreException {

		final String benefactor = nodeInheritanceDAO.getBenefactor(entityId);
		UserEntityPermissions permissions = new UserEntityPermissions();
		permissions.setCanAddChild(canAccess(userInfo, benefactor, ACCESS_TYPE.CREATE));
		permissions.setCanChangePermissions(canAccess(userInfo, benefactor, ACCESS_TYPE.CHANGE_PERMISSIONS));
		permissions.setCanDelete(canAccess(userInfo, benefactor, ACCESS_TYPE.DELETE));
		permissions.setCanEdit(canAccess(userInfo, benefactor, ACCESS_TYPE.UPDATE));
		permissions.setCanView(canAccess(userInfo, benefactor, ACCESS_TYPE.READ));
		permissions.setCanDownload(canDownload(userInfo, entityId));

		Node node = nodeDAO.getNode(entityId);
		permissions.setOwnerPrincipalId(node.getCreatedByPrincipalId());

		UserInfo anonymousUser = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		permissions.setCanPublicRead(canAccess(anonymousUser, benefactor, ACCESS_TYPE.READ));

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
			if (!isEvalOwner(userInfo, evaluation)) {
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
			if (!isEvalOwner(userInfo, evaluation)) {
				return false;
			}
		} else {
			throw new NotFoundException("Unexpected object type: "+subjectId.getType());
		}
		return true;
	}
	
}
