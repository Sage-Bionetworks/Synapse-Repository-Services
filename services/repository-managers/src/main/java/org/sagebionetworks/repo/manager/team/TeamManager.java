package org.sagebionetworks.repo.manager.team;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamHeader;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
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
	public QueryResults<Team> get(long offset, long limit) throws DatastoreException;

	public Map<TeamHeader, List<UserGroupHeader>> getAllTeamsAndMembers() throws DatastoreException;
	/**
	 * Retrieve the Teams to which the given user belongs, paginated
	 * @param principalId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public QueryResults<Team> getByMember(String principalId, long offset, long limit) throws DatastoreException;
	
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
	 * Add a member to a Team
	 * @param userInfo
	 * @param teamId
	 * @param principalId
	 * @param isAdmin
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void addMember(UserInfo userInfo, String teamId, String principalId, boolean isAdmin) throws DatastoreException, UnauthorizedException, NotFoundException;
	
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
	 * return the URL for the icon of the given Team
	 * @param teamId
	 * @return
	 */
	public URL getIconURL(String teamId) throws NotFoundException;
}
