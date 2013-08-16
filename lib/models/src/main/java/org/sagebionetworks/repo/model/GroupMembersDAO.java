package org.sagebionetworks.repo.model;

import java.util.List;


public interface GroupMembersDAO {

	/**
	 * Retrieves the IDs of members of the given principalId
	 * @param principalId
	 * @return
	 * @throws DatastoreException
	 */
	public GroupMembers getMembers(String principalId) throws DatastoreException;
	
	/**
	 * Adds the list of principal IDs to the group
	 * @param dto
	 * @throws DatastoreException
	 */
	public void addMembers(GroupMembers dto) throws DatastoreException;
	
	/**
	 * Removes the list of principal IDs from the group
	 * @param dto
	 * @throws DatastoreException
	 */
	public void removeMembers(GroupMembers dto) throws DatastoreException;
	
	/**
	 * Retrieves the list of groups the given user belongs to
	 * @param principalId
	 * @return
	 * @throws DatastoreException
	 */
	public List<UserGroup> getUsersGroups(String principalId) throws DatastoreException;

}
