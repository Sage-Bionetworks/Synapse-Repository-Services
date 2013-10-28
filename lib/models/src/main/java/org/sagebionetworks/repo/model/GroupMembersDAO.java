package org.sagebionetworks.repo.model;

import java.util.List;

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
	 * Ensure the bootstrap users are in the correct bootstrap groups
	 */
	public void bootstrapGroups() throws NotFoundException;
	
	/**
	 * Checks and revises any cache(s) used to improve performance of nested queries
	 */
	public void validateCache(String principalId);
}
