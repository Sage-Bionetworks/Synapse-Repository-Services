package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;


public interface GroupMembersDAO {

	/**
	 * Retrieves the IDs of direct members of the given principalId
	 */
	public List<UserGroup> getMembers(String principalId) 
			throws DatastoreException, NotFoundException;

	/**
	 * Retrieves the IDs of members of the given principalId
	 * @param nested Whether the members should be direct or nested
	 */
	public List<UserGroup> getMembers(String principalId, boolean nested)
			throws DatastoreException, NotFoundException;
	
	/**
	 * Adds the list of principal IDs to the group
	 */
	public void addMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException, IllegalArgumentException;
	
	/**
	 * Removes the list of principal IDs from the group
	 */
	public void removeMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException;
	
	/**
	 * Retrieves the list of groups the given user belongs to
	 */
	public List<UserGroup> getUsersGroups(String principalId) 
			throws DatastoreException, NotFoundException;
	
	/**
	 * Changes the etag of the parent group (for migration purposes)
	 * and nullifies cached parents of all children and their children
	 * @return All IDs where the cache has been cleared
	 */
	public Set<String> markAsUpdated(String groupId, List<String> memberIds)
			throws DatastoreException;

}
