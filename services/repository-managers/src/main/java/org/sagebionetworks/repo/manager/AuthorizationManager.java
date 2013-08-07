package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ObjectType;
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
	 * Check user access to a Node (default type)
	 * 
	 * @param userInfo
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff the given user has the given access to the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public boolean canAccess(UserInfo userInfo, String nodeId, ACCESS_TYPE accessType) 	throws NotFoundException, DatastoreException;
	
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

	public boolean canAccessAccessApprovalsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException;
	
	
}
