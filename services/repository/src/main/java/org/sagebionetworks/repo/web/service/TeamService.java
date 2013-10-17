package org.sagebionetworks.repo.web.service;

import java.net.URL;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	public Team create(String userId,
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
	public URL getIconURL(String teamId) throws DatastoreException, NotFoundException;

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
	public Team update(String userId, Team team) throws  DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException;
	
	/**
	 * 
	 * @param userId
	 * @param teamId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public void delete(String userId, String teamId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * 
	 * @param userId
	 * @param teamId
	 * @param principalId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void addMember(String userId, String teamId, String principalId) throws DatastoreException, UnauthorizedException, NotFoundException;

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
	 * @param userId
	 * @param teamId
	 * @param principalId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void removeMember(String userId, String teamId, String principalId) throws DatastoreException, UnauthorizedException, NotFoundException;

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
	void refreshCache(String userId) throws DatastoreException, NotFoundException;
	
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
	public void setPermissions(String userId, String teamId, String principalId, boolean isAdmin) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userId
	 * @param teamId
	 * @param principalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public TeamMembershipStatus getTeamMembershipStatus(String userId, String teamId, String principalId) throws DatastoreException, NotFoundException;
	
}
