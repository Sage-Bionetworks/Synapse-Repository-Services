package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
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
	public AuthorizationStatus canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType) throws DatastoreException, NotFoundException;

	/**
     * Checks whether the given user can create the given node.
     *
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff either (1) the user has 'add child' access to the parent or (2) parent is null
	 * and user is admin returns whether access is granted and, if not, a String giving the reason why
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public AuthorizationStatus canCreate(UserInfo userInfo, final Node node) throws NotFoundException, DatastoreException ;
	
	/**
	 * Checks whether the given user can create the given access requirement
	 * 
	 * @param userInfo
	 * @param accessRequirement
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException 
	 */
	public AuthorizationStatus canCreateAccessRequirement(UserInfo userInfo, AccessRequirement accessRequirement) throws NotFoundException;

	/**
	 * Checks whether the given user can create the given access approval
	 * 
	 * @param userInfo
	 * @param accessRequirement
	 * @return whether access is granted and, if not, a String giving the reason why
	 */
	public AuthorizationStatus canCreateAccessApproval(UserInfo userInfo, AccessApproval accessApproval);

	/**
	 * 
	 * @param userInfo UserInfo of the user in question
	 * @param activityId activity that generated the entities
	 * @return Returns true if the specified user can read at least one entity with the specified activity Id.  Returns whether access is granted and, if not, a String giving the reason why
	 */
	public AuthorizationStatus canAccessActivity(UserInfo userInfo, String activityId) throws NotFoundException;
	
	/**
	 * The raw FileHandle can only be accessed by the user that created it.
	 * @param userInfo
	 * @param fileHandleId
	 * @param creator
	 * @return whether access is granted and, if not, a String giving the reason why
	 */
	public AuthorizationStatus canAccessRawFileHandleByCreator(UserInfo userInfo, String fileHandleId, String creator);
	
	/**
	 * Is the user the creator or are they an admin
	 * @param userInfo
	 * @param creator
	 * @return
	 */
	public boolean isUserCreatorOrAdmin(UserInfo userInfo, String creator);
	
	/**
	 * 
	 * @param userInfo
	 * @param fileHandleId
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException 
	 */
	public AuthorizationStatus canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param fileHandleId
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException
	 */
	public void canAccessRawFileHandlesByIds(UserInfo userInfo, List<String> fileHandleId, Set<String> allowed, Set<String> disallowed)
			throws NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param subjectId
	 * @param accessType
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException
	 */
	public AuthorizationStatus canAccessAccessApprovalsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException;
	
	/**
	 * Is this the AnonymousUser?
	 * @param userInfo
	 * @return
	 */
	public boolean isAnonymousUser(UserInfo userInfo);
	
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
	public AuthorizationStatus canUserMoveRestrictedEntity(UserInfo userInfo, String sourceParentId, String destParentId) throws NotFoundException;

	/**
	 * Check if the user can start a given Asynchronous job
	 * 
	 * @param userInfo
	 * @param body
	 * @return whether access is granted and, if not, a String giving the reason why
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public AuthorizationStatus canUserStartJob(UserInfo userInfo, AsynchronousRequestBody body) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param objectId
	 * @param objectType
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AuthorizationStatus canCreateWiki(UserInfo userInfo, String objectId, ObjectType objectType) throws DatastoreException, NotFoundException;
}
