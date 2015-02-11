package org.sagebionetworks.repo.manager.team;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface TeamManager {

	/**
	 * Create a new Team
	 * @param userInfo
	 * @param team
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public Team create(UserInfo userInfo, Team team) throws  DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;

	/**
	 * Retrieve the Teams in the system, paginated
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<Team> list(long limit, long offset) throws DatastoreException;
	
	/**
	 * 
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public ListWrapper<Team> list(Set<Long> ids) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<TeamMember> listMembers(String teamId, long limit, long offset) throws DatastoreException;
	
	/**
	 * 
	 * @param teamId
	 * @param memberIds
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public ListWrapper<TeamMember> listMembers(Long teamId, Set<Long> memberIds) throws DatastoreException, NotFoundException;
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
	 * @return
	 * @throws DatastoreException
	 */
	public Map<Team, Collection<TeamMember>> listAllTeamsAndMembers() throws DatastoreException;
	
	/**
	 * Retrieve the Teams to which the given user belongs, paginated
	 * @param principalId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<Team> listByMember(String principalId, long limit, long offset) throws DatastoreException;
	
	/**
	 * Get a Team by its ID
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Team get(String id) throws DatastoreException, NotFoundException;

	/**
	 * Update a Team
	 * 
	 * @param userInfo
	 * @param team
	 * @return
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public Team put(UserInfo userInfo, Team team) throws InvalidModelException, DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * Delete a Team by its ID
	 * @param userInfo
	 * @param id
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void delete(UserInfo userInfo, String id) throws DatastoreException, UnauthorizedException, NotFoundException; 

	/**
	 * Add a member to a Team, removing applicable membership requests and invitations.
	 * @param userInfo
	 * @param teamId
	 * @param principalUserInfo
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void addMember(UserInfo userInfo, String teamId, UserInfo principalUserInfo) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * Remove a member from a Team
	 * @param userInfo
	 * @param teamId
	 * @param principalId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void removeMember(UserInfo userInfo, String teamId, String principalId) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * Get the ACL for a Team
	 * 
	 * @param userInfo
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public AccessControlList getACL(UserInfo userInfo, String teamId) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * Update the ACL for a Team
	 * @param userInfo
	 * @param acl
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void updateACL(UserInfo userInfo, AccessControlList acl) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param teamId
	 * @param principalId
	 * @param isAdmin
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void setPermissions(UserInfo userInfo, String teamId, String principalId, boolean isAdmin) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo the user info of the requestor
	 * @param teamId
	 * @param principalUserInfo the user info of the one whose status we're asking about
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public TeamMembershipStatus getTeamMembershipStatus(UserInfo userInfo, String teamId, UserInfo principalUserInfo) throws DatastoreException, NotFoundException;
	
	/**
	 * return the URL for the icon of the given Team
	 * @param teamId
	 * @return
	 */
	public String getIconURL(String teamId) throws NotFoundException;
}
