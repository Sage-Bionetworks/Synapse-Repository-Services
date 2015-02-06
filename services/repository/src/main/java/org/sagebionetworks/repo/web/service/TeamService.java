package org.sagebionetworks.repo.web.service;

import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface TeamService {
	/**
	 * 
	 * @param userId
	 * @param team
	 * @return
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public Team create(Long userId,
			Team team) throws UnauthorizedException, InvalidModelException, DatastoreException, NotFoundException;

	/**
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<Team> get(long limit, long offset)
			throws DatastoreException;
	
	/**
	 * 
	 * @param fragment
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public PaginatedResults<Team> get(String fragment, long limit, long offset)
			throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 */
	public PaginatedResults<Team> getByMember(String principalId, long limit, long offset)
			throws DatastoreException;
	
	/**
	 * 
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public Team get(String teamId) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public String getIconURL(String teamId) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param team
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 */
	public Team update(Long userId, Team team) throws  DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException;
	
	/**
	 * 
	 * @param userId
	 * @param teamId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public void delete(Long userId, String teamId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param teamId
	 * @param principalId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void addMember(Long userId, String teamId, String principalId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * 
	 * @param teamId
	 * @param fragment
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<TeamMember> getMembers(String teamId, String fragment, long limit, long offset) throws DatastoreException, NotFoundException;

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
	 * @param userId
	 * @param teamId
	 * @param principalId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void removeMember(Long userId, String teamId, String principalId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * 
	 * @return
	 */
	Long millisSinceLastCacheUpdate();

	/**
	 * For use by Quartz
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void refreshCache() throws DatastoreException, NotFoundException;
	
	/**
	 * For use by TeamController, requests from which must be authenticated
	 * @param userId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void refreshCache(Long userId) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param userId
	 * @param teamId
	 * @param principalId
	 * @param isAdmin
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void setPermissions(Long userId, String teamId, String principalId, boolean isAdmin) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userId
	 * @param teamId
	 * @param principalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public TeamMembershipStatus getTeamMembershipStatus(Long userId, String teamId, String principalId) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param ids
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	ListWrapper<Team> list(Set<Long> ids) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param teamId
	 * @param memberIds
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public ListWrapper<TeamMember> listTeamMembers(Long teamId, Set<Long> memberIds) throws DatastoreException, NotFoundException;
	
}
