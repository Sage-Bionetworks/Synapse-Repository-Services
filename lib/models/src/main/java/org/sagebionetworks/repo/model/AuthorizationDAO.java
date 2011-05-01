package org.sagebionetworks.repo.model;

import java.util.Collection;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AuthorizationDAO {
	
	/**
	 * @param groupId
	 * @param nodeId
	 * @param accessTypes
	 * @param userId the user making the request
	 * 
	 * sets the access that given group has for the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid; 
	 * UnauthorizedException if the given user doesn't have 'share'
	 * permission on the given node
	 * 
	 */
	public void setAccess(String groupId, String nodeId, Collection<String> accessTypes, String userId)
		throws NotFoundException, UnauthorizedException, DatastoreException;
	
	/**
	 * @param groupId
	 * @param nodeId
	 * @param userId the user making the request
	 * 
	 * removes the access to the given node from given group
	 * 
	 * @exception NotFoundException if the group or node is invalid; 
	 * UnauthorizedException if the given user doesn't have 'share'
	 * permission on the given node
	 * 
	 */
	public void removeAccess(String groupId, String nodeId, String userId) 
		throws NotFoundException, UnauthorizedException, DatastoreException;
	
	/**
	 * @param groupId
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff the given group has the given access to the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public boolean canAccess(String groupId, String nodeId, String accessType) 
		throws NotFoundException, DatastoreException;

	// at this level there is no representation of distinct resource types
	// either (1) have to add a 'type' attribute to a Node; or (2) have to
	// do the check at a higher level, prior to reaching this one
	// public void canCreate(user, resource-type)

}
