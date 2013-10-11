package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipRequestManager {
	
	/**
	 * Request to join the team.
	 * @param userInfo
	 * @param mrs
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 */
	public MembershipRqstSubmission create(UserInfo userInfo, MembershipRqstSubmission mrs) throws  DatastoreException, InvalidModelException, UnauthorizedException;
	
	/**
	 * Retrieve an request by its ID
	 * @param userInfo
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipRqstSubmission get(UserInfo userInfo, String id) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Delete a request
	 * 
	 * @param userInfo
	 * @param id
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void delete(UserInfo userInfo, String id) throws DatastoreException, UnauthorizedException, NotFoundException; 
	
	/**
	 * Get the Requests for the given user
	 * 
	 * @param userInfo
	 * @param principalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<MembershipRequest> getOpenByTeamInRange(UserInfo userInfo, String teamId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * Get the Requests for the given user, to join the given team
	 * @param userInfo
	 * @param principalId
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<MembershipRequest> getOpenByTeamAndRequestorInRange(UserInfo userInfo, String teamId, String requestorId, long limit, long offset) throws DatastoreException, NotFoundException;

}
