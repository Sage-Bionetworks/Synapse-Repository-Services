package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TeamDAO {
	/**
	 * @param dto object to be created
	 * @return the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public Team create(Team dto) throws DatastoreException, InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Team get(String id) throws DatastoreException, NotFoundException;


	/**
	 * Validates that team exists, if the team does not exist, throws a NotFoundException
	 *
	 * @param teamId
	 * @return
	 * @throws NotFoundException
	 */
	void validateTeamExists(String teamId) throws NotFoundException;

	/**
	 * 
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public ListWrapper<Team> list(List<Long> ids) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the Teams in the system
	 * 
	 * @param offset
	 * @param limit
	 * 
	 */
	public List<Team> getInRange(long limit, long offset) throws DatastoreException;
	
	/**
	 * 
	 * @return the number of teams in the system
	 * @throws DatastoreException
	 */
	public long getCount() throws DatastoreException;
	
	/**
	 * 
	 * @param teamId
	 * @param principalId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public TeamMember getMember(String teamId, String principalId) throws NotFoundException, DatastoreException;
	
	/**
	 * 
	 * @param teamIds
	 * @param principalIds
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public ListWrapper<TeamMember> listMembers(List<Long> teamIds, List<Long> principalIds) throws NotFoundException, DatastoreException;

	/**
	 * Note: this method does not fill in the field {@link org.sagebionetworks.repo.model.TeamMember#getIsAdmin }
	 * @param teamId The team which the returned members belong to.
	 * @param include The set of principal IDs to explicitly include in search. All members with principal IDs that do not match
	 *             IDs in this set will be filtered out. If null, a filter is not applied.
	 * @param exclude The set of principal IDs to explicitly exclude in search. All members with principal IDs that do match
	 *             IDs in this set will not be included in results. If null, a filter is not applied.
	 * @param limit
	 * @param offset
	 * @return A paginated list of members for that team
	 * @throws DatastoreException
	 */
	public List<TeamMember> getMembersInRange(String teamId, Set<Long> include, Set<Long> exclude, long limit, long offset) throws DatastoreException;

	/**
	 * 
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 */
	public long getMembersCount(String teamId) throws DatastoreException;
	
	/**
	 * Get the ids of all the admins in the team
	 * @param teamId
	 * @return
	 * @throws NotFoundException
	 */
	public List<String> getAdminTeamMemberIds(String teamId) throws NotFoundException;

	/**
	 * This is used to build up the team and member prefix caches
	 * @return
	 * @throws DatastoreException
	 */
	public Map<Team, Collection<TeamMember>> getAllTeamsAndMembers() throws DatastoreException;
	
	/**
	 * Get the Teams a member belongs to
	 * @param principalId the team member
	 * @param offset
	 * @param limit
	 * @return the Teams this principal belongs to
	 * @throws DatastoreException 
	 */
	public List<Team> getForMemberInRange(String principalId, long limit, long offset) throws DatastoreException;

	/**
	 * Get the IDs of the Teams a member belongs to
	 * @param teamMemberId
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @param ascending
	 * @return
	 */
	List<String> getIdsForMember(String teamMemberId, long limit, long offset, TeamSortOrder sortBy, Boolean ascending);

	/**
	 * 
	 * @param principalId
	 * @return the number of teams the given member belongs to
	 * @throws DatastoreException
	 */
	public long getCountForMember(String principalId) throws DatastoreException;
	
	/**
	 * Updates the 'shallow' properties of an object.
	 * Note:  leaving createdBy and createdOn null in the dto tells the DAO to use the currently stored values
	 *
	 * @param team
	 * @throws DatastoreException
	 */
	public Team update(Team team) throws InvalidModelException,
			NotFoundException, ConflictingUpdateException, DatastoreException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	/**
	 * returns the number of admin members in a Team
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 */
	long getAdminMemberCount(String teamId) throws DatastoreException;

	/**
	 * retrieve all teams that a user is an admin
	 * 
	 * @param userId
	 * @return
	 */
	public List<String> getAllTeamsUserIsAdmin(String userId);
}
