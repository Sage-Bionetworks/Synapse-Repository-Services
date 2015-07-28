package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessApproval;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Multimap;

public class AuthorizationManagerImpl implements AuthorizationManager {

	private static final String FILE_HANDLE_UNAUTHORIZED_TEMPLATE = "Only the creator of a FileHandle can assign it to an Entity.  FileHandleId = '%1$s', UserId = '%2$s'";

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private AccessApprovalDAO  accessApprovalDAO;
	@Autowired
	private ActivityDAO activityDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private EvaluationDAO evaluationDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private AccessControlListDAO aclDAO;

	@Override
	public AuthorizationStatus canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType)
			throws DatastoreException, NotFoundException {

		// anonymous can at most READ
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			if (accessType != ACCESS_TYPE.READ) return AuthorizationManagerUtil.accessDenied("Anonymous users are unauthorized for all but public read operations.");
		}

		switch (objectType) {
			case ENTITY:
				return entityPermissionsManager.hasAccess(objectId, accessType, userInfo);
			case EVALUATION:
				return evaluationPermissionsManager.hasAccess(userInfo, objectId, accessType);
			case ACCESS_REQUIREMENT:
				if (userInfo.isAdmin()) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				if (accessType==ACCESS_TYPE.READ) {
					return AuthorizationManagerUtil.AUTHORIZED;					
				}
				AccessRequirement accessRequirement = accessRequirementDAO.get(objectId);
				return canAdminAccessRequirement(userInfo, accessRequirement);
			case ACCESS_APPROVAL:
				if (userInfo.isAdmin()) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				AccessApproval accessApproval = accessApprovalDAO.get(objectId);
				return canAdminAccessApproval(userInfo, accessApproval);
			case TEAM:
				if (userInfo.isAdmin()) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				// just check the acl
				boolean teamAccessPermission = aclDAO.canAccess(userInfo.getGroups(), objectId, ObjectType.TEAM, accessType);
				if (teamAccessPermission) {
					return AuthorizationManagerUtil.AUTHORIZED;
				} else {
					return AuthorizationManagerUtil.accessDenied("Unauthorized to access Team "+objectId+" for "+accessType);
				}
			default:
				throw new IllegalArgumentException("Unknown ObjectType: "+objectType);
		}
	}

	@Override
	public AuthorizationStatus canCreate(UserInfo userInfo, final Node node) 
		throws NotFoundException, DatastoreException {
		return entityPermissionsManager.canCreate(node, userInfo);
	}

	@Override
	public AuthorizationStatus canChangeSettings(UserInfo userInfo, Node node) throws NotFoundException, DatastoreException {
		return entityPermissionsManager.canChangeSettings(node, userInfo);
	}

	@Override
	public AuthorizationStatus canAccessActivity(UserInfo userInfo, String activityId) throws DatastoreException, NotFoundException {
		if(userInfo.isAdmin()) return AuthorizationManagerUtil.AUTHORIZED;
		
		// check if owner
		Activity act = activityDAO.get(activityId);
		if(act.getCreatedBy().equals(userInfo.getId().toString()))
				return AuthorizationManagerUtil.AUTHORIZED;
		
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
					if(canAccess(userInfo, nodeId, ObjectType. ENTITY, ACCESS_TYPE.READ).getAuthorized()) {
						return AuthorizationManagerUtil.AUTHORIZED;
					}
				} catch (Exception e) {
					// do nothing, same as false
				}
			}
			offset += limit; 
		}
		// no access found to generated entities, no access
		return AuthorizationManagerUtil.accessDenied("User lacks permission to access Activity "+activityId);
	}
	
	@Override
	public boolean isUserCreatorOrAdmin(UserInfo userInfo, String creator) {
		// Admins can see anything.
		if (userInfo.isAdmin()) return true;
		// Only the creator can see the raw file handle
		return userInfo.getId().toString().equals(creator);
	}

	@Override
	public AuthorizationStatus canAccessRawFileHandleByCreator(UserInfo userInfo, String fileHandleId, String creator) {
		if( isUserCreatorOrAdmin(userInfo, creator)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil.accessDenied(createFileHandleUnauthorizedMessage(fileHandleId, userInfo));
		}
	}
	
	/**
	 * Create an unauthorized message for file handles.
	 * 
	 * @param fileHandleId
	 * @param userInfo
	 * @return
	 */
	private String createFileHandleUnauthorizedMessage(String fileHandleId,	UserInfo userInfo) {
		return String.format(FILE_HANDLE_UNAUTHORIZED_TEMPLATE, fileHandleId, userInfo.getId().toString());
	}
	


	@Override
	public AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException {
		// Admins can do anything
		if(userInfo.isAdmin()) return AuthorizationManagerUtil.AUTHORIZED;
		// Lookup the creator by
		String creator  = fileHandleDao.getHandleCreator(fileHandleId);
		// Call the other methods
		return canAccessRawFileHandleByCreator(userInfo, fileHandleId, creator);
	}

	@Override
	public void canAccessRawFileHandlesByIds(UserInfo userInfo, List<String> fileHandleIds, Set<String> allowed, Set<String> disallowed)
			throws NotFoundException {
		// no file handles, nothing to do
		if (fileHandleIds.isEmpty()) {
			return;
		}

		// Admins can do anything
		if (userInfo.isAdmin()) {
			allowed.addAll(fileHandleIds);
			return;
		}

		// Lookup the creators
		Multimap<String, String> creatorMap = fileHandleDao.getHandleCreators(fileHandleIds);
		for (Entry<String, Collection<String>> entry : creatorMap.asMap().entrySet()) {
			String creator = entry.getKey();
			if (canAccessRawFileHandleByCreator(userInfo, "", creator).getAuthorized()) {
				allowed.addAll(entry.getValue());
			} else {
				disallowed.addAll(entry.getValue());
			}
		}
	}

	@Override
	public AuthorizationStatus canCreateAccessRequirement(UserInfo userInfo,
			AccessRequirement accessRequirement) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userInfo)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil.accessDenied("Access Requirements may only be created by a member of the Synapse Access and Compliance Team.");
		}
	}
	
	public boolean isACTTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) return true;
		if(userInfo.getGroups().contains(TeamConstants.ACT_TEAM_ID)) return true;
		return false;
	}

	/**
	 * Check that user is an administrator or ACT member. 
	 * @param userInfo
	 * @param accessRequirement
	 * @throws NotFoundException
	 */
	private AuthorizationStatus canAdminAccessRequirement(UserInfo userInfo, AccessRequirement accessRequirement) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userInfo)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil.accessDenied("Only ACT member may create or modify Access Restrictions.");
		}
	}
	
	/**
	 * Checks whether the parent (or other ancestors) are subject to access restrictions and, if so, whether 
	 * userInfo is a member of the ACT.
	 * 
	 * @param userInfo
	 * @param sourceParentId
	 * @param destParentId
	 * @return
	 */
	@Override
	public AuthorizationStatus canUserMoveRestrictedEntity(UserInfo userInfo, String sourceParentId, String destParentId) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userInfo)) return AuthorizationManagerUtil.AUTHORIZED;
		if (sourceParentId.equals(destParentId)) return AuthorizationManagerUtil.AUTHORIZED;
		List<String> sourceParentAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, sourceParentId, true);
		List<AccessRequirement> allRequirementsForSourceParent = accessRequirementDAO.getForSubject(sourceParentAncestorIds, RestrictableObjectType.ENTITY);
		List<String> destParentAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, destParentId, true);
		List<AccessRequirement> allRequirementsForDestParent = accessRequirementDAO.getForSubject(destParentAncestorIds, RestrictableObjectType.ENTITY);
		Set<AccessRequirement> diff = new HashSet<AccessRequirement>(allRequirementsForSourceParent);
		diff.removeAll(allRequirementsForDestParent);
		if (diff.isEmpty()) { // only OK if destParent has all the requirements that source parent has
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil.accessDenied("Cannot move restricted entity to a location having fewer access restrictions.");
		}
	}
	
	private AuthorizationStatus canAdminAccessApproval(UserInfo userInfo, AccessApproval accessApproval) throws NotFoundException {
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessApproval.getRequirementId().toString());
		return canAdminAccessRequirement(userInfo, accessRequirement);
	}

	@Override
	public AuthorizationStatus canCreateAccessApproval(UserInfo userInfo,
			AccessApproval accessApproval) {
		if (accessApproval instanceof ACTAccessApproval) {
			if (isACTTeamMemberOrAdmin(userInfo)) {
				return AuthorizationManagerUtil.AUTHORIZED;
			} else {
				return new AuthorizationStatus(false, "User is not an ACT Member.");
			}
		} else if (accessApproval instanceof SelfSignAccessApproval) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else if (accessApproval instanceof TermsOfUseAccessApproval) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else if (accessApproval instanceof PostMessageContentAccessApproval) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			throw new IllegalArgumentException("Unrecognized type: "+accessApproval.getClass().getName());
		}
	}

	@Override
	public AuthorizationStatus canAccessAccessApprovalsForSubject(UserInfo userInfo,
			RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userInfo)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return new AuthorizationStatus(false,"You are not allowed to retrieve access approvals for this subject.");
		}
	}

	@Override
	public boolean isAnonymousUser(UserInfo userInfo) {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		return AuthorizationUtils.isUserAnonymous(userInfo);
	}

	@Override
	public AuthorizationStatus canCreateWiki(UserInfo userInfo, String objectId, ObjectType objectType) throws DatastoreException, NotFoundException {
		if (objectType==ObjectType.ENTITY) {
			return entityPermissionsManager.canCreateWiki(objectId, userInfo);
		} else {
			return canAccess(userInfo, objectId, objectType, ACCESS_TYPE.CREATE);
		}
	}
	
}
