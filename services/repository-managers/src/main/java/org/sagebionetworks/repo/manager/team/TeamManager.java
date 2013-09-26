package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
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
	 */
	public Team create(UserInfo userInfo, Team team) throws  DatastoreException, InvalidModelException, UnauthorizedException;

	/**
	 * Retrieve the Teams in the system, paginated
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public QueryResults<Team> get(long limit, long offset) throws DatastoreException;

	/**
	 * Retrieve the Teams whose names match the given nameFragment, paginated
	 * @param nameFragment
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException
	 */
	public QueryResults<Team> getByNameFragment(String nameFragment, long limit, long offset) throws DatastoreException;

	/**
	 * Retrieve the Teams to which the given user belongs, paginated
	 * @param principalId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DatastoreException

	 */
	public QueryResults<Team> getByMember(String principalId, long limit, long offset) throws DatastoreException;
	
	public Team get(String id) throws DatastoreException, NotFoundException;

	public Team put(UserInfo userInfo, Team team) throws InvalidModelException, DatastoreException, UnauthorizedException, NotFoundException;
	
	public void delete(UserInfo userInfo, String id) throws DatastoreException, UnauthorizedException, NotFoundException; 

	public void addMember(UserInfo userInfo, String teamId, String principalId) throws DatastoreException, UnauthorizedException, NotFoundException;
	
	public QueryResults<Team> getMembersByNameFragment(String nameFragment, long limit, long offset);
	
	
	
	
}
