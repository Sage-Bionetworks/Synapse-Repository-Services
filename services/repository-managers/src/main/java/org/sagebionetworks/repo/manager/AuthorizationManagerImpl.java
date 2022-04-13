package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.docker.RegistryEventAction.pull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.file.FileAssociateObject;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.manager.form.FormManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AuthorizationManagerImpl implements AuthorizationManager {

	private static final String REPOSITORY_TYPE = "repository";
	private static final String REGISTRY_TYPE = "registry";
	private static final String REGISTRY_CATALOG = "catalog";	
	public static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	public static final String ANONYMOUS_ACCESS_DENIED_REASON = "Anonymous cannot perform this action. Please login and try again.";
	private static final String FILE_HANDLE_ID_IS_NOT_ASSOCIATED_TEMPLATE = "FileHandleId: %1s is not associated with objectId: %2s of type: %3s";


	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private EntityAuthorizationManager entityAuthorizationManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private AccessControlListManager aclManager;
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
	private org.sagebionetworks.evaluation.dao.SubmissionDAO submissionDAO;
	@Autowired
	private MessageManager messageManager;
	@Autowired
	private org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO dataAccessSubmissionDao;
	@Autowired
	private DockerNodeDao dockerNodeDao;
	@Autowired
	private TokenGenerator tokenGenerator;
	@Autowired
	private FormManager formManager;
	@Autowired
	private FileHandleAuthorizationManager fileHandleAuthorizationManager;
	@Autowired
	private DataAccessAuthorizationManager dataAccessAuthorizationManager;
	
	@Override
	public AuthorizationStatus canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType)
			throws DatastoreException, NotFoundException {
		switch (objectType) {
			case ENTITY:
				return entityAuthorizationManager.hasAccess(userInfo, objectId, accessType);
			case EVALUATION:
				return evaluationPermissionsManager.hasAccess(userInfo, objectId, accessType);
			case ACCESS_REQUIREMENT:
				if (isACTTeamMemberOrAdmin(userInfo)) {
					return AuthorizationStatus.authorized();
				}
				if (accessType==ACCESS_TYPE.READ || accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationStatus.authorized();
				}
				return AuthorizationStatus.accessDenied("Only ACT member can perform this action.");
			case ACCESS_APPROVAL:
				if (isACTTeamMemberOrAdmin(userInfo)) {
					return AuthorizationStatus.authorized();
				}
				return AuthorizationStatus.accessDenied("Only ACT member can perform this action.");
			case TEAM:
				if (userInfo.isAdmin()) {
					return AuthorizationStatus.authorized();
				}
				// everyone should be able to download the Team's Icon, even anonymous.
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationStatus.authorized();
				}
				
				// just check the acl
				boolean teamAccessPermission = aclManager.canAccess(userInfo.getGroups(), objectId, objectType, accessType);
				if (teamAccessPermission) {
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Unauthorized to access Team "+objectId+" for "+accessType);
				}
			case VERIFICATION_SUBMISSION:
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					if (isACTTeamMemberOrAdmin(userInfo) ||
							verificationDao.getVerificationSubmitter(Long.parseLong(objectId))==userInfo.getId()) {
						return AuthorizationStatus.authorized();
					} else {
					return AuthorizationStatus.accessDenied(
							"You must be an ACT member or the owner of the Verification Submission to download its attachments.");
					}
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
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
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			case EVALUATION_SUBMISSIONS:
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					Submission submission = submissionDAO.get(objectId);
					return evaluationPermissionsManager.hasAccess(userInfo, submission.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			case MESSAGE: {
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					try {
						// if the user can get the message metadata, he/she can download the message
						messageManager.getMessage(userInfo, objectId);
						return AuthorizationStatus.authorized();
					} catch (UnauthorizedException e) {
						return AuthorizationStatus.accessDenied(e);
					}
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			case DATA_ACCESS_REQUEST:
				if (accessType == ACCESS_TYPE.DOWNLOAD) {
					return dataAccessAuthorizationManager.canDownloadRequestFiles(userInfo, objectId);
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			case DATA_ACCESS_SUBMISSION: {
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return dataAccessAuthorizationManager.canDownloadSubmissionFiles(userInfo, objectId);
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			case FORM_DATA: {
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return formManager.canUserDownloadFormData(userInfo, objectId);
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			default:
				throw new IllegalArgumentException("Unknown ObjectType: "+objectType);
		}
	}

	@Override
	public AuthorizationStatus canCreate(UserInfo userInfo, String parentId, EntityType nodeType) 
		throws NotFoundException, DatastoreException {
		return entityAuthorizationManager.canCreate(parentId, nodeType, userInfo);
	}
	
	@Override
	public AuthorizationStatus canAccessRawFileHandleByCreator(UserInfo userInfo, String fileHandleId, String creator) {
		return AuthorizationUtils.canAccessRawFileHandleByCreator(userInfo, fileHandleId, creator);
	}


	@Override
	public AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException {
		return fileHandleAuthorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId);
	}

	@Override
	public boolean isACTTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		return AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo);
	}

	@Override
	public boolean isReportTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		return AuthorizationUtils.isReportTeamMemberOrAdmin(userInfo);
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
			return AuthorizationStatus.authorized();
		}
		if (sourceParentId.equals(destParentId)) {
			return AuthorizationStatus.authorized();
		}
		List<Long> sourceParentAncestorIds = nodeDao.getEntityPathIds(sourceParentId);
		List<Long> destParentAncestorIds = nodeDao.getEntityPathIds(destParentId);

		List<String> missingRequirements = accessRequirementDAO.getAccessRequirementDiff(sourceParentAncestorIds, destParentAncestorIds, RestrictableObjectType.ENTITY);
		if (missingRequirements.isEmpty()) { // only OK if destParent has all the requirements that source parent has
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus.accessDenied("Cannot move restricted entity to a location having fewer access restrictions.");
		}
	}

	@Override
	public AuthorizationStatus canAccessAccessApprovalsForSubject(UserInfo userInfo,
			RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userInfo)) {
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus.accessDenied("You are not allowed to retrieve access approvals for this subject.");
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
			return entityAuthorizationManager.canCreateWiki(objectId, userInfo);
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
						AuthorizationStatus.authorized()));
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
							accessDeniedFileNotAssociatedWithObject(fileHandleId, associatedObjectId,
											associationType)));
				}
			}
		}
		return results;
	}

	/**
	 * Create an access denied status for a file handle not associated with the requested object.
	 * @param fileHandleId
	 * @param associatedObjectId
	 * @param associateType
	 * @return
	 */
	public static AuthorizationStatus accessDeniedFileNotAssociatedWithObject(String fileHandleId, String associatedObjectId, FileHandleAssociateType associateType){
		return AuthorizationStatus.accessDenied(String
				.format(FILE_HANDLE_ID_IS_NOT_ASSOCIATED_TEMPLATE,
						fileHandleId, associatedObjectId,
						associateType));
	}


	@Override
	public Set<Long> getAccessibleBenefactors(UserInfo userInfo, ObjectType objectType, Set<Long> benefactors) {
		return aclManager.getAccessibleBenefactors(userInfo, objectType, benefactors);
	}

	@Override
	public AuthorizationStatus canSubscribe(UserInfo userInfo, String objectId,
			SubscriptionObjectType objectType)
			throws DatastoreException, NotFoundException {
		if (isAnonymousUser(userInfo)) {
			return AuthorizationStatus.accessDenied(ANONYMOUS_ACCESS_DENIED_REASON);
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
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Only ACT member can follow this topic.");
				}
			case DATA_ACCESS_SUBMISSION_STATUS:
				if (dataAccessSubmissionDao.isAccessor(objectId, userInfo.getId().toString())) {
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Only accessors can follow this topic.");
				}
		}
		return AuthorizationStatus.accessDenied("The objectType is unsubscribable.");
	}

	@Override
	public Set<Long> getAccessibleProjectIds(Set<Long> principalIds) {
		return this.aclManager.getAccessibleProjectIds(principalIds);
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
	public Set<String> getPermittedDockerActions(UserInfo userInfo, List<OAuthScope> oauthScopes, String service, String type, String name, String actionTypes) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(oauthScopes, "oauthScopes");
		ValidateArgument.required(service, "service");
		ValidateArgument.required(type, "type");
		ValidateArgument.required(name, "name");
		ValidateArgument.required(actionTypes, "actionTypes");
		
		String[] actionArray = actionTypes.split(",");
		if (REGISTRY_TYPE.equalsIgnoreCase(type)) {
			return getPermittedDockerRegistryActions(userInfo, oauthScopes, service, name, actionArray);
		} else if (REPOSITORY_TYPE.equalsIgnoreCase(type)) {
			Set<RegistryEventAction> approvedActions = getPermittedDockerRepositoryActions(userInfo, oauthScopes, service, name, actionArray);
			Set<String> result = new HashSet<String>();
			for (RegistryEventAction a : approvedActions) result.add(a.name());
			return result;
		} else {
			throw new IllegalArgumentException("Unexpected type "+type);
		}
	}

	private Set<String> getPermittedDockerRegistryActions(UserInfo userInfo, List<OAuthScope> oauthScopes, String service, String name, String[] actionTypes) {
		if (name.equalsIgnoreCase(REGISTRY_CATALOG)) {
			// OK, it's a request to list the catalog
			if (userInfo.isAdmin() && oauthScopes.contains(OAuthScope.view)) { 
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

	private Set<RegistryEventAction> getPermittedDockerRepositoryActions(UserInfo userInfo, List<OAuthScope> oauthScopes, String service, String repositoryPath, String[] actionTypes) {		Set<RegistryEventAction> permittedActions = new HashSet<RegistryEventAction>();

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
				if (!oauthScopes.contains(OAuthScope.modify)) {
					break; // No need to check specific permissions
				}
				// check CREATE or UPDATE permission and add to permittedActions
				AuthorizationStatus as = null;
				if (existingDockerRepoId==null) {
					String parentId = validDockerRepositoryParentId(repositoryPath);
					if (parentId==null) {
						// can't push to a non-existent parent
						as = AuthorizationStatus.accessDenied(""); //TODO: more informative message?
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
				if (as!=null && as.isAuthorized()) {
					permittedActions.add(requestedAction);
					if (existingDockerRepoId==null) permittedActions.add(pull);
				}
				break;
			case pull:
				if (!oauthScopes.contains(OAuthScope.download)) {
					break; // No need to check specific permissions
				}
				if (
					// check DOWNLOAD permission and add to permittedActions
					(existingDockerRepoId!=null && !isInTrash && canAccess(
							userInfo, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).isAuthorized()) ||
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
	public AuthorizationStatus canAccessMembershipInvitation(UserInfo userInfo, MembershipInvitation mi, ACCESS_TYPE accessType) {
		if (mi.getInviteeId() != null) {
			// The invitee should be able to read or delete the invitation
			boolean userIsInvitee = Long.parseLong(mi.getInviteeId()) == userInfo.getId();
			if (userIsInvitee && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE)) {
				return AuthorizationStatus.authorized();
			}
		}
		// An admin of the team should be able to create, read or delete the invitation
		boolean userIsTeamAdmin = aclManager.canAccess(userInfo.getGroups(), mi.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		if (userIsTeamAdmin && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE || accessType == ACCESS_TYPE.CREATE)) {
			return AuthorizationStatus.authorized();
		}
		// A Synapse admin should have access of any type
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType +  " membership invitation " + mi.getId());
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(MembershipInvtnSignedToken token, ACCESS_TYPE accessType) {
		String miId = token.getMembershipInvitationId();
		try {
			tokenGenerator.validateToken(token);
		} catch (UnauthorizedException e) {
			return AuthorizationStatus.accessDenied("Unauthorized to access membership invitation " + miId + " (" + e.getMessage() + ")");
		}
		if (accessType == ACCESS_TYPE.READ) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType +  " membership invitation " + miId);
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(Long userId, InviteeVerificationSignedToken token, ACCESS_TYPE accessType) {
		String miId = token.getMembershipInvitationId();
		try {
			tokenGenerator.validateToken(token);
		} catch (UnauthorizedException e) {
			return AuthorizationStatus.accessDenied("Unauthorized to access membership invitation " + miId + " (" + e.getMessage() + ")");
		}
		if (token.getInviteeId().equals(userId.toString()) && token.getMembershipInvitationId().equals(miId) && accessType == ACCESS_TYPE.UPDATE) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType +  " membership invitation " + miId);
	}

	@Override
	public AuthorizationStatus canAccessMembershipRequest(UserInfo userInfo, MembershipRequest mr, ACCESS_TYPE accessType) {
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		// An admin of the team should be able to read or delete the request
		// The requester should also be able to read or delete the request
		boolean userIsTeamAdmin = aclManager.canAccess(userInfo.getGroups(), mr.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		boolean userIsRequester = Long.parseLong(mr.getUserId()) == userInfo.getId();
		if ((userIsTeamAdmin || userIsRequester) && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE)) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType + " membership request " + mr.getId());
	}

	@Override
	public List<FileHandleAssociationAuthorizationStatus> canDownLoadFile(UserInfo user,
			List<FileHandleAssociation> associations) {
		if(user == null){
			throw new IllegalArgumentException("User cannot be null");
		}
		// validate the input
		validate(associations);
		/* A separate authorization check must be made for each associated object so the
		 * first step is to group by the associated object.
		 */
		Map<FileAssociateObject, List<String>> objectGroups =  Maps.newHashMap();
		for(FileHandleAssociation fha: associations){
			FileAssociateObject key = new FileAssociateObject(fha.getAssociateObjectId(), fha.getAssociateObjectType());
			List<String> fileHandleIds = objectGroups.get(key);
			if(fileHandleIds == null){
				fileHandleIds = Lists.newLinkedList();
				objectGroups.put(key, fileHandleIds);
			}
			fileHandleIds.add(fha.getFileHandleId());
		}
		// used to track the results.
		Map<FileHandleAssociation, FileHandleAssociationAuthorizationStatus> resultMap = Maps.newHashMap();
		// execute a canDownloadFile() check for each object group.
		for(FileAssociateObject object: objectGroups.keySet()){
			List<String> fileHandleIds = objectGroups.get(object);
			// The authorization check for this group.
			List<FileHandleAuthorizationStatus> groupResults = canDownloadFile(user, fileHandleIds, object.getObjectId(), object.getObjectType());
			// Build the results for this group
			for(FileHandleAuthorizationStatus fileStatus: groupResults){
				FileHandleAssociation fileAssociation = new FileHandleAssociation();
				fileAssociation.setFileHandleId(fileStatus.getFileHandleId());
				fileAssociation.setAssociateObjectId(object.getObjectId());
				fileAssociation.setAssociateObjectType(object.getObjectType());
				FileHandleAssociationAuthorizationStatus result = new FileHandleAssociationAuthorizationStatus(fileAssociation, fileStatus.getStatus());
				resultMap.put(fileAssociation, result);
			}
		}
		
		// put the results in the same order as the request
		List<FileHandleAssociationAuthorizationStatus> results = Lists.newLinkedList();
		for(FileHandleAssociation association: associations){
			results.add(resultMap.get(association));
		}
		return results;
	}
	
	public void validate(List<FileHandleAssociation> associations){
		if(associations == null){
			throw new IllegalArgumentException("FileHandleAssociations cannot be null");
		}
		if(associations.isEmpty()){
			throw new IllegalArgumentException("FileHandleAssociations is empty.  Must include at least one FileHandleAssociation");
		}
		for(FileHandleAssociation fha: associations){
			validate(fha);
		}
	}
	
	public void validate(FileHandleAssociation fha){
		if(fha == null){
			throw new IllegalArgumentException("FileHandleAssociation cannot be null");
		}
		if(fha.getFileHandleId() == null){
			throw new IllegalArgumentException("FileHandleAssociation.fileHandleId cannot be null");
		}
		if(fha.getAssociateObjectId() == null){
			throw new IllegalArgumentException("FileHandleAssociation.associateObjectId cannot be null");
		}
		if(fha.getAssociateObjectType() == null){
			throw new IllegalArgumentException("FileHandleAssociation.associateObjectType cannot be null");
		}
	}
}
