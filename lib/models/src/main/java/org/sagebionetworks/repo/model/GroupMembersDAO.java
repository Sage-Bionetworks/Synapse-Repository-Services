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
	public void bootstrapGroups() throws Exception;

	/**
	 * Returns all individual userIds from the given set of principalIds. For a
	 * principalId in the set, if it's an individual, returns that userId; if it's
	 * teamId, returns all users in that team.
	 * 
	 * @param principalIds
	 * @param limit
	 * @param offset
	 * @return
	 */
	public Set<String> getIndividuals(Set<String> principalIds, Long limit, Long offset);

	/**
	 * Returns the count of all individual userIds from the given set of principalIds.
	 * For a principalId in the set, if it's an individual, it counts as 1;
	 * if it's teamId, it counts all members once.
	 * 
	 * @param principalIds
	 * @return
	 */
	public Long getIndividualCount(Set<String> principalIds);
}
