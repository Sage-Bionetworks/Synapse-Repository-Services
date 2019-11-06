package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;


public interface GroupMembersDAO {

	/**
	 * Retrieves the IDs of direct members of the given principalId
	 */
	List<UserGroup> getMembers(String principalId) 
			throws DatastoreException, NotFoundException;
	
	/**
	 * Return the subset of groups in which the given principal is a member
	 * @param principalId
	 * @param groupIds
	 * @return
	 */
	List<String> filterUserGroups(String principalId, List<String> groupIds);
	
	/**
	 * Adds the list of principal IDs to the group
	 */
	void addMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException, IllegalArgumentException;
	
	/**
	 * Removes the list of principal IDs from the group
	 */
	void removeMembers(String groupId, List<String> memberIds) 
			throws DatastoreException, NotFoundException;
	
	/**
	 * Retrieves the list of groups the given user belongs to
	 */
	List<UserGroup> getUsersGroups(String principalId) 
			throws DatastoreException, NotFoundException;

	/**
	 * Ensure the bootstrap users are in the correct bootstrap groups
	 */
	void bootstrapGroups() throws Exception;

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
	Set<String> getIndividuals(Set<String> principalIds, Long limit, Long offset);

	/**
	 * Returns the count of all individual userIds from the given set of principalIds.
	 * For a principalId in the set, if it's an individual, it counts as 1;
	 * if it's teamId, it counts all members once.
	 * 
	 * @param principalIds
	 * @return
	 */
	Long getIndividualCount(Set<String> principalIds);

	/**
	 * 
	 * @param userIds
	 * @return true if all users are members of groupId; false otherwise.
	 */
	boolean areMemberOf(String groupId, Set<String> userIds);
	
	/**
	 * Get the IDs of all members in this team.
	 * @param teamId
	 * @return
	 */
	Set<Long> getMemberIds(Long teamId);

	/**
	 * Get the IDs of all members in the giving team locking for update.
	 * @param teamId
	 * @return
	 */
	Set<Long> getMemberIdsForUpdate(Long teamId);
}
