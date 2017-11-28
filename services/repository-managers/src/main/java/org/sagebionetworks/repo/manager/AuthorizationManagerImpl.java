package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.docker.RegistryEventAction.pull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class AuthorizationManagerImpl implements AuthorizationManager {

	private static final String REPOSITORY_TYPE = "repository";
	private static final String REGISTRY_TYPE = "registry";
	private static final String REGISTRY_CATALOG = "catalog";	
	public static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfiguration.getTrashFolderEntityIdStatic());

	private static final String FILE_HANDLE_UNAUTHORIZED_TEMPLATE = "Only the creator of a FileHandle can assign it to an Entity.  FileHandleId = '%1$s', UserId = '%2$s'";
	public static final String ANONYMOUS_ACCESS_DENIED_REASON = "Anonymous cannot perform this action. Please login and try again.";

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private ActivityDAO activityDAO;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private VerificationDAO verificationDao;
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private FileHandleAssociationManager fileHandleAssociationSwitch;
	@Autowired
	private V2WikiPageDao wikiPageDaoV2;
	@Autowired
	private org.sagebionetworks.repo.model.evaluation.SubmissionDAO submissionDAO;
	@Autowired
	private MessageManager messageManager;
	@Autowired
	private org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO dataAccessSubmissionDao;
	@Autowired
	private DockerNodeDao dockerNodeDao;
	@Autowired
	private GroupMembersDAO groupMembersDao;
	@Autowired
	private MembershipInvitationDAO membershipInvitationDAO;

	
	@Override
	public AuthorizationStatus canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType)
			throws DatastoreException, NotFoundException {
		switch (objectType) {
			case ENTITY:
				return entityPermissionsManager.hasAccess(objectId, accessType, userInfo);
			case EVALUATION:
				return evaluationPermissionsManager.hasAccess(userInfo, objectId, accessType);
			case ACCESS_REQUIREMENT:
				if (isACTTeamMemberOrAdmin(userInfo)) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				if (accessType==ACCESS_TYPE.READ || accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				return AuthorizationManagerUtil.accessDenied("Only ACT member can perform this action.");
			case ACCESS_APPROVAL:
				if (isACTTeamMemberOrAdmin(userInfo)) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				return AuthorizationManagerUtil.accessDenied("Only ACT member can perform this action.");
			case TEAM:
				if (userInfo.isAdmin()) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				// everyone should be able to download the Team's Icon, even anonymous.
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationManagerUtil.AUTHORIZED;
				}
				
				// just check the acl
				boolean teamAccessPermission = aclDAO.canAccess(userInfo.getGroups(), objectId, objectType, accessType);
				if (teamAccessPermission) {
					return AuthorizationManagerUtil.AUTHORIZED;
				} else {
					return AuthorizationManagerUtil.accessDenied("Unauthorized to access Team "+objectId+" for "+accessType);
				}
			case VERIFICATION_SUBMISSION:
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					if (isACTTeamMemberOrAdmin(userInfo) ||
							verificationDao.getVerificationSubmitter(Long.parseLong(objectId))==userInfo.getId()) {
						return AuthorizationManagerUtil.AUTHORIZED;
					} else {
					return AuthorizationManagerUtil.accessDenied(
							"You must be an ACT member or the owner of the Verification Submission to download its attachments.");
					}
				} else {
					return AuthorizationManagerUtil.accessDenied("Unexpected access type "+accessType);
				}
			case WIKI:{
				ACCESS_TYPE ownerAccessType = accessType;
				if(ACCESS_TYPE.DOWNLOAD == accessType){
					// Wiki download is checked against owner read.
					ownerAccessType = ACCESS_TYPE.READ;
				}
				WikiPageKey key = wikiPageDaoV2.lookupWikiKey(objectId);
				// check against the wiki owner
				return canAccess(userInfo, key.getOwnerObjectId(), key.getOwnerObjectType(), ownerAccessType);
			}
			case USER_PROFILE: {
				// everyone should be able to download userProfile picture, even anonymous.
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationManagerUtil.AUTHORIZED;
				} else {
					return AuthorizationManagerUtil.accessDenied("Unexpected access type "+accessType);
				}
			}
			case EVALUATION_SUBMISSIONS:
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					Submission submission = submissionDAO.get(objectId);
					return evaluationPermissionsManager.hasAccess(userInfo, submission.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
				} else {
					return AuthorizationManagerUtil.accessDenied("Unexpected access type "+accessType);
				}
			case MESSAGE: {
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					try {
						// if the user can get the message metadata, he/she can download the message
						messageManager.getMessage(userInfo, objectId);
						return AuthorizationManagerUtil.AUTHORIZED;
					} catch (UnauthorizedException e) {
						return AuthorizationManagerUtil.ACCESS_DENIED;
					}
				} else {
					return AuthorizationManagerUtil.accessDenied("Unexpected access type "+accessType);
				}
			}
			case DATA_ACCESS_REQUEST:
			case DATA_ACCESS_SUBMISSION: {
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					if (isACTTeamMemberOrAdmin(userInfo)) {
						return AuthorizationManagerUtil.AUTHORIZED;
					} else {
						return AuthorizationManagerUtil.ACCESS_DENIED;
					}
				} else {
					return AuthorizationManagerUtil.accessDenied("Unexpected access type "+accessType);
				}
			}
			default:
				throw new IllegalArgumentException("Unknown ObjectType: "+objectType);
		}
	}

	@Override
	public AuthorizationStatus canCreate(UserInfo userInfo, String parentId, EntityType nodeType) 
		throws NotFoundException, DatastoreException {
		return entityPermissionsManager.canCreate(parentId, nodeType, userInfo);
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

	@Deprecated
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
	public boolean isACTTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) return true;
		if(userInfo.getGroups().contains(TeamConstants.ACT_TEAM_ID)) return true;
		return false;
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
		if (isACTTeamMemberOrAdmin(userInfo)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		if (sourceParentId.equals(destParentId)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		List<String> sourceParentAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, sourceParentId, true);
		List<String> destParentAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, destParentId, true);

		List<String> missingRequirements = accessRequirementDAO.getAccessRequirementDiff(sourceParentAncestorIds, destParentAncestorIds, RestrictableObjectType.ENTITY);
		if (missingRequirements.isEmpty()) { // only OK if destParent has all the requirements that source parent has
			return AuthorizationManagerUtil.AUTHORIZED;
		} else {
			return AuthorizationManagerUtil.accessDenied("Cannot move restricted entity to a location having fewer access restrictions.");
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

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.AuthorizationManager#canDownloadFile(org.sagebionetworks.repo.model.UserInfo, java.util.List)
	 */
	@Override
	public List<FileHandleAuthorizationStatus> canDownloadFile(UserInfo user,
			List<String> fileHandleIds, String associatedObjectId,
			FileHandleAssociateType associationType) {
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null");
		}
		if (fileHandleIds == null) {
			throw new IllegalArgumentException("FileHandleIds cannot be null");
		}
		ObjectType assocatedObjectType = fileHandleAssociationSwitch.getAuthorizationObjectTypeForAssociatedObjectType(associationType);
		// Is the user authorized to download the associated object?
		AuthorizationStatus canUserDownloadAssociatedObject = canAccess(user,
				associatedObjectId, assocatedObjectType, ACCESS_TYPE.DOWNLOAD);
		List<FileHandleAuthorizationStatus> results = new ArrayList<FileHandleAuthorizationStatus>(fileHandleIds.size());
		// Validate all all filehandles are actually associated with the given
		// object.
		Set<String> associatedFileHandleIds = fileHandleAssociationSwitch.getFileHandleIdsAssociatedWithObject(fileHandleIds,
						associatedObjectId, associationType);
		// Which file handles did the user create.
		Set<String> fileHandlesCreatedByUser = fileHandleDao.getFileHandleIdsCreatedByUser(user.getId(), fileHandleIds);
		for (String fileHandleId : fileHandleIds) {
			
			if (fileHandlesCreatedByUser.contains(fileHandleId) || user.isAdmin()) {
				// The user is the creator of the file or and admin so they can
				// download it.
				results.add(new FileHandleAuthorizationStatus(fileHandleId,
						AuthorizationManagerUtil.AUTHORIZED));
			} else {
				/*
				 * The user is not an admin and they are not the creator of the
				 * file. Therefore they can only download the file if they have
				 * the download permission on the associated object and the
				 * fileHandle is actually associated with the object.
				 */
				if (associatedFileHandleIds.contains(fileHandleId)) {
					results.add(new FileHandleAuthorizationStatus(fileHandleId,
							canUserDownloadAssociatedObject));
				} else {
					// The fileHandle is not associated with the object.
					results.add(new FileHandleAuthorizationStatus(
							fileHandleId,
							AuthorizationManagerUtil.accessDeniedFileNotAssociatedWithObject(fileHandleId, associatedObjectId,
											associationType)));
				}
			}
		}
		return results;
	}


	@Override
	public Set<Long> getAccessibleBenefactors(UserInfo userInfo, Set<Long> benefactors) {
		Set<Long> results = null;
		if (userInfo.isAdmin()){
			// admin same as input
			results = Sets.newHashSet(benefactors);
		}else{
			// non-adim run a query
			results = this.aclDAO.getAccessibleBenefactors(userInfo.getGroups(), benefactors,
					ObjectType.ENTITY, ACCESS_TYPE.READ);
		}
		// The trash folder should not be in the results
		results.remove(TRASH_FOLDER_ID);
		return results;
	}

	@Override
	public AuthorizationStatus canSubscribe(UserInfo userInfo, String objectId,
			SubscriptionObjectType objectType)
			throws DatastoreException, NotFoundException {
		if (isAnonymousUser(userInfo)) {
			return AuthorizationManagerUtil.accessDenied(ANONYMOUS_ACCESS_DENIED_REASON);
		}
		switch (objectType) {
			case FORUM:
				Forum forum = forumDao.getForum(Long.parseLong(objectId));
				return canAccess(userInfo, forum.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
			case THREAD:
				String projectId = threadDao.getProjectId(objectId);
				return canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ);
			case DATA_ACCESS_SUBMISSION:
				if (isACTTeamMemberOrAdmin(userInfo)) {
					return AuthorizationManagerUtil.AUTHORIZED;
				} else {
					return AuthorizationManagerUtil.accessDenied("Only ACT member can follow this topic.");
				}
			case DATA_ACCESS_SUBMISSION_STATUS:
				if (dataAccessSubmissionDao.isAccessor(objectId, userInfo.getId().toString())) {
					return AuthorizationManagerUtil.AUTHORIZED;
				} else {
					return AuthorizationManagerUtil.accessDenied("Only accessors can follow this topic.");
				}
		}
		return AuthorizationManagerUtil.accessDenied("The objectType is unsubscribable.");
	}

	@Override
	public Set<Long> getAccessibleProjectIds(Set<Long> principalIds) {
		ValidateArgument.required(principalIds, "principalIds");
		if(principalIds.isEmpty()){
			return new HashSet<>(0);
		}
		return this.aclDAO.getAccessibleProjectIds(principalIds, ACCESS_TYPE.READ);
	}
	
	/*
	 * Given a docker repository path, return a valid parent Id, 
	 * a project which has been verified to exist. 
	 * If there is no such valid parent then return null
	 */
	public String validDockerRepositoryParentId(String dockerRepositoryPath) {
		// check that 'repositoryPath' is a valid path
		try {
			DockerNameUtil.validateName(dockerRepositoryPath);
		} catch (IllegalArgumentException e) {
			return null;
		}
		// check that 'repopath' starts with a synapse ID (synID) and synID is a project or folder
		String parentId;
		try {
			parentId = DockerNameUtil.getParentIdFromRepositoryPath(dockerRepositoryPath);
		} catch (IllegalArgumentException e) {
			return null;
		}
		
		if (parentId==null) throw new IllegalArgumentException("parentId is required.");

		try {
			EntityType parentType = nodeDao.getNodeTypeById(parentId);
			if (parentType!=EntityType.project) {
				return null;
			}
		} catch (NotFoundException e) {
			return null;
		}

		return parentId;
	}

	@Override
	public Set<String> getPermittedDockerActions(UserInfo userInfo, String service, String type, String name, String actionTypes) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(service, "service");
		ValidateArgument.required(type, "type");
		ValidateArgument.required(name, "name");
		ValidateArgument.required(actionTypes, "actionTypes");
		
		String[] actionArray = actionTypes.split(",");
		if (REGISTRY_TYPE.equalsIgnoreCase(type)) {
			return getPermittedDockerRegistryActions(userInfo, service, name, actionArray);
		} else if (REPOSITORY_TYPE.equalsIgnoreCase(type)) {
			Set<RegistryEventAction> approvedActions = getPermittedDockerRepositoryActions(userInfo, service, name, actionArray);
			Set<String> result = new HashSet<String>();
			for (RegistryEventAction a : approvedActions) result.add(a.name());
			return result;
		} else {
			throw new IllegalArgumentException("Unexpected type "+type);
		}
	}

	private Set<String> getPermittedDockerRegistryActions(UserInfo userInfo, String service, String name, String[] actionTypes) {
		if (name.equalsIgnoreCase(REGISTRY_CATALOG)) {
			// OK, it's a request to list the catalog
			if (userInfo.isAdmin()) { 
				// an admin can do *anything*
				return new HashSet<String>(Arrays.asList(actionTypes));
			} else {
				// non-admins cannot list the catalog
				return Collections.emptySet();
			}
		} else {
			// unrecognized name
			return Collections.emptySet();
		}
	}

	private Set<RegistryEventAction> getPermittedDockerRepositoryActions(UserInfo userInfo, String service, String repositoryPath, String[] actionTypes) {		Set<RegistryEventAction> permittedActions = new HashSet<RegistryEventAction>();

		String repositoryName = service+DockerNameUtil.REPO_NAME_PATH_SEP+repositoryPath;

		String existingDockerRepoId = dockerNodeDao.getEntityIdForRepositoryName(repositoryName);
		
		boolean isInTrash = false;
		if (existingDockerRepoId!=null) {
			String benefactor = nodeDao.getBenefactor(existingDockerRepoId);
			isInTrash = TRASH_FOLDER_ID.equals(KeyFactory.stringToKey(benefactor));
		}
		for (String requestedActionString : actionTypes) {
			RegistryEventAction requestedAction = RegistryEventAction.valueOf(requestedActionString);
			switch (requestedAction) {
			case push:
				// check CREATE or UPDATE permission and add to permittedActions
				AuthorizationStatus as = null;
				if (existingDockerRepoId==null) {
					String parentId = validDockerRepositoryParentId(repositoryPath);
					if (parentId==null) {
						// can't push to a non-existent parent
						as = AuthorizationManagerUtil.ACCESS_DENIED;
					} else {
						// check for create permission on parent
						as = canCreate(userInfo, parentId, EntityType.dockerrepo);
					}
				} else {
					if (!isInTrash) {
						// check update permission on this entity
						as = canAccess(userInfo, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
					}
				}
				if (as!=null && as.getAuthorized()) {
					permittedActions.add(requestedAction);
					if (existingDockerRepoId==null) permittedActions.add(pull);
				}
				break;
			case pull:
				if (
					// check DOWNLOAD permission and add to permittedActions
					(existingDockerRepoId!=null && !isInTrash && canAccess(
							userInfo, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).getAuthorized()) ||
					// If Docker repository was submitted to an Evaluation and if the requester
					// has administrative access to the queue, then DOWNLOAD permission is granted
					evaluationPermissionsManager.isDockerRepoNameInEvaluationWithAccess(repositoryName, 
							userInfo.getGroups(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION)) {
						permittedActions.add(requestedAction);
				}
				break;
			default:
				throw new RuntimeException("Unexpected action type: " + requestedAction);
			}
		}
		return permittedActions;
	}

	@Override
	public void validateHasAccessorRequirement(HasAccessorRequirement req, Set<String> accessors) {
		if (req.getIsCertifiedUserRequired()) {
			ValidateArgument.requirement(groupMembersDao.areMemberOf(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
					accessors),
					"Accessors must be Synapse Certified Users.");
		}
		if (req.getIsValidatedProfileRequired()) {
			ValidateArgument.requirement(verificationDao.haveValidatedProfiles(accessors),
					"Accessors must have validated profiles.");
		}
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(UserInfo userInfo, MembershipInvitation mi, ACCESS_TYPE accessType) {
		if (mi.getInviteeId() != null) {
			// The invitee should be able to read or delete the invitation
			boolean userIsInvitee = Long.parseLong(mi.getInviteeId()) == userInfo.getId();
			if (userIsInvitee && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE)) {
				return AuthorizationManagerUtil.AUTHORIZED;
			}
		}
		// An admin of the team should be able to create, read or delete the invitation
		boolean userIsTeamAdmin = aclDAO.canAccess(userInfo.getGroups(), mi.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		if (userIsTeamAdmin && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE || accessType == ACCESS_TYPE.CREATE)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		// A Synapse admin should have access of any type
		if (userInfo.isAdmin()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		return AuthorizationManagerUtil.accessDenied("Unauthorized to " + accessType +  " membership invitation " + mi.getId());
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(MembershipInvtnSignedToken token, ACCESS_TYPE accessType) {
		String miId = token.getMembershipInvitationId();
		try {
			SignedTokenUtil.validateToken(token);
		} catch (IllegalArgumentException e) {
			return AuthorizationManagerUtil.accessDenied("Unauthorized to access membership invitation " + miId + "(" + e.getMessage() + ")");
		}
		if (accessType == ACCESS_TYPE.READ) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		return AuthorizationManagerUtil.accessDenied("Unauthorized to " + accessType +  " membership invitation " + miId);
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(Long userId, InviteeVerificationSignedToken token, ACCESS_TYPE accessType) {
		String miId = token.getMembershipInvitationId();
		try {
			SignedTokenUtil.validateToken(token);
		} catch (IllegalArgumentException e) {
			return AuthorizationManagerUtil.accessDenied("Unauthorized to access membership invitation " + miId + "(" + e.getMessage() + ")");
		}
		if (token.getInviteeId().equals(userId.toString()) && token.getMembershipInvitationId().equals(miId) && accessType == ACCESS_TYPE.UPDATE) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		return AuthorizationManagerUtil.accessDenied("Unauthorized to " + accessType +  " membership invitation " + miId);
	}

	@Override
	public AuthorizationStatus canAccessMembershipRequest(UserInfo userInfo, MembershipRequest mr, ACCESS_TYPE accessType) {
		if (userInfo.isAdmin()) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		// An admin of the team should be able to read or delete the request
		// The requester should also be able to read or delete the request
		boolean userIsTeamAdmin = aclDAO.canAccess(userInfo.getGroups(), mr.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		boolean userIsRequester = Long.parseLong(mr.getUserId()) == userInfo.getId();
		if ((userIsTeamAdmin || userIsRequester) && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE)) {
			return AuthorizationManagerUtil.AUTHORIZED;
		}
		return AuthorizationManagerUtil.accessDenied("Unauthorized to " + accessType + " membership request " + mr.getId());
	}
}
