package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AuthorizationManager {
	
	/**
	 * Check user access to an object
	 * 
	 * @param userInfo
	 * @param objectId
	 * @param objectType
	 * @param accessType
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	AuthorizationStatus canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType) throws DatastoreException, NotFoundException;



	/**
     * Checks whether the given user can create the given node.
	 * 
	 * @param userInfo
	 * @param parentId
	 * @param nodeType
	 * @return true iff either (1) the user has 'add child' access to the parent or (2) parent is null
	 * and user is admin returns whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	AuthorizationStatus canCreate(UserInfo userInfo, String parentId, EntityType nodeType) throws NotFoundException, DatastoreException ;

	/**
	 * 
	 * @param userInfo UserInfo of the user in question
	 * @param activityId activity that generated the entities
	 * @return Returns true if the specified user can read at least one entity with the specified activity Id.  Returns whether access is granted and, if not, a String giving the reason why
	 */
	AuthorizationStatus canAccessActivity(UserInfo userInfo, String activityId) throws NotFoundException;
	
	/**
	 * The raw FileHandle can only be accessed by the user that created it.
	 * @param userInfo
	 * @param fileHandleId
	 * @param creator
	 * @return whether access is granted and, if not, a String giving the reason why
	 */
	AuthorizationStatus canAccessRawFileHandleByCreator(UserInfo userInfo, String fileHandleId, String creator);
	
	/**
	 * Is the user the creator or are they an admin
	 * @param userInfo
	 * @param creator
	 * @return
	 */
	boolean isUserCreatorOrAdmin(UserInfo userInfo, String creator);
	
	/**
	 * 
	 * @param userInfo
	 * @param fileHandleId
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException 
	 */
	AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException;
	
	/**
	 * Given a list of FileHandleIds, can the user download each file?
	 * <ul>
	 * <li>If a user is an admin then they will be authorized for all files.</li>
	 * <li>A user will be authorized to download each FileHandle that they
	 * created.</li>
	 * <li>For all other cases the user will be authorized as long as both of
	 * the following conditions are met:</li>
	 * <ol>
	 * <li>The FileHandle is actually associated with the object.</li>
	 * <li>The user is authorized to download the associated object.</li>
	 * </ol>
	 * </ul>
	 * 
	 * @param user
	 * @param associations
	 * @return Map key
	 */
	List<FileHandleAuthorizationStatus> canDownloadFile(UserInfo user, List<String> fileHandleId, String associatedObjectId, FileHandleAssociateType associationType);


	/**
	 * 
	 * @param userInfo
	 * @param subjectId
	 * @param accessType
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException
	 */
	AuthorizationStatus canAccessAccessApprovalsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException;
	
	/**
	 * Is this the AnonymousUser?
	 * @param userInfo
	 * @return
	 */
	boolean isAnonymousUser(UserInfo userInfo);
	
	/**
	 * Checks whether the parent (or other ancestors) are subject to access restrictions and whether 
	 * userInfo is a member of the ACT.
	 * 
	 * @param userInfo
	 * @param sourceParentId
	 * @param destParentId
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException 
	 */
	AuthorizationStatus canUserMoveRestrictedEntity(UserInfo userInfo, String sourceParentId, String destParentId) throws NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param objectId
	 * @param objectType
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AuthorizationStatus canCreateWiki(UserInfo userInfo, String objectId, ObjectType objectType) throws DatastoreException, NotFoundException;

	/**
	 *
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	boolean isACTTeamMemberOrAdmin(UserInfo userInfo)
			throws DatastoreException, UnauthorizedException;

	/**
	 *
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	boolean isReportTeamMemberOrAdmin(UserInfo userInfo)
			throws DatastoreException, UnauthorizedException;

	/**
	 * Get the intersection of the given benefactor ids and the benefactors the user can read.
	 * @param userInfo
	 * @param originalBenefactors
	 * @return
	 */
	Set<Long> getAccessibleBenefactors(UserInfo userInfo, ObjectType objectType, Set<Long> originalBenefactors);

	/**
	 * Check user access to an subscribable object
	 * 
	 * @param userInfo
	 * @param objectId
	 * @param objectType
	 * @param accessType
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	AuthorizationStatus canSubscribe(UserInfo userInfo, String objectId, SubscriptionObjectType objectType) throws DatastoreException, NotFoundException;

	/**
	 * Get the set of project IDs that that are visible to the passed set of
	 * principal IDs. 
	 * 
	 * @param principalIds
	 * @return
	 */
	Set<Long> getAccessibleProjectIds(Set<Long> principalIds);
	
	/**
	 * 
	 * @param userInfo
	 * @param oauthScopes
	 * @param service
	 * @param type
	 * @param name
	 * @param actionTypes
	 * @return the permitted actions for the given user on the given repository
	 */
	public Set<String> getPermittedDockerActions(UserInfo userInfo, List<OAuthScope> oauthScopes, String service, String type, String name, String actionTypes);

	/**
	 * Validate and throw exception for HasAccessorRequirement
	 * 
	 * @param req
	 * @param accessors
	 */
	void validateHasAccessorRequirement(HasAccessorRequirement req, Set<String> accessors);

	/**
	 * Check whether a user has access to a MembershipInvitation
	 *
	 * @param userInfo
	 * @param mis
	 * @param accessType
	 * @return whether access is granted and, if not, a String giving the reason why
	 */
	AuthorizationStatus canAccessMembershipInvitation(UserInfo userInfo, MembershipInvitation mis, ACCESS_TYPE accessType);

	/**
	 * Check whether the token is valid for the access_type
	 *
	 * @param token
	 * @param accessType
	 * @return
	 */
	AuthorizationStatus canAccessMembershipInvitation(MembershipInvtnSignedToken token, ACCESS_TYPE accessType);

	/**
	 * Check whether the token is valid for the access_type
	 *
	 * @param userId
	 * @param token
	 * @param accessType
	 * @return
	 */
	AuthorizationStatus canAccessMembershipInvitation(Long userId, InviteeVerificationSignedToken token, ACCESS_TYPE accessType);

	/**
	 * Check whether a user has access to a MembershipRequest
	 *
	 * @param userInfo
	 * @param mr
	 * @param accessType
	 * @return
	 */
	AuthorizationStatus canAccessMembershipRequest(UserInfo userInfo, MembershipRequest mr, ACCESS_TYPE accessType);
}
