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
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public boolean canAccess(UserInfo userInfo, String objectId, ObjectType objectType, ACCESS_TYPE accessType) throws DatastoreException, NotFoundException;

	/**
     * Checks whether the given user can create the given node.
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
	public boolean canCreate(UserInfo userInfo, final Node node) throws NotFoundException, DatastoreException ;
	
	/**
	 * Checks whether the given user can create the given access requirement
	 * 
	 * @param userInfo
	 * @param accessRequirement
	 * @return
	 * @throws NotFoundException 
	 */
	public boolean canCreateAccessRequirement(UserInfo userInfo, AccessRequirement accessRequirement) throws NotFoundException;

	/**
	 * Checks whether the given user can create the given access approval
	 * 
	 * @param userInfo
	 * @param accessRequirement
	 * @return
	 */
	public boolean canCreateAccessApproval(UserInfo userInfo, AccessApproval accessApproval);

	/**
	 * 
	 * @param userInfo UserInfo of the user in question
	 * @param activityId activity that generated the entities
	 * @return Returns true if the specified user can read at least one entity with the specified activity Id
	 */
	public boolean canAccessActivity(UserInfo userInfo, String activityId);
	
	/**
	 * The raw FileHandle can only be accessed by the user that created it.
	 * @param userInfo
	 * @param creator
	 * @return
	 */
	public boolean canAccessRawFileHandleByCreator(UserInfo userInfo, String creator);
	
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
	 * @return
	 * @throws NotFoundException 
	 */
	public boolean canAccessRawFileHandleById(UserInfo userInfo, String fileHandleId) throws NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param fileHandleId
	 * @return
	 * @throws NotFoundException
	 */
	public void canAccessRawFileHandlesByIds(UserInfo userInfo, List<String> fileHandleId, Set<String> allowed, Set<String> disallowed)
			throws NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param subjectId
	 * @param accessType
	 * @return
	 * @throws NotFoundException
	 */
	public boolean canAccessAccessApprovalsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException;
	
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
	 * @return
	 * @throws NotFoundException 
	 */
	public boolean canUserMoveRestrictedEntity(UserInfo userInfo, String sourceParentId, String destParentId) throws NotFoundException;

	/**
	 * Check if the user can start a given Asynchronous job
	 * 
	 * @param userInfo
	 * @param body
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public boolean canUserStartJob(UserInfo userInfo, AsynchronousRequestBody body) throws DatastoreException, NotFoundException;

	/**
	 * returns true iff the user is a certified user
	 * @param userInfo
	 * @return
	 */
	public boolean isCertifiedUser(UserInfo userInfo);
}
