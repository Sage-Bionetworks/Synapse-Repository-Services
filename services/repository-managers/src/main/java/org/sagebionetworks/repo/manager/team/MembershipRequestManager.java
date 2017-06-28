package org.sagebionetworks.repo.manager.team;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipRequestManager {
	
	/**
	 * Request to join the team.
	 * 
	 * @param userInfo
	 * @param mrs
	 * @param acceptInvitationEndpoint
	 * @param notificationUnsubscribeEndpoint
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
	public PaginatedResults<MembershipRequest> getOpenByTeamAndRequesterInRange(UserInfo userInfo, String teamId, String requesterId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param requesterId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public PaginatedResults<MembershipRqstSubmission> getOpenSubmissionsByRequesterInRange(
			UserInfo userInfo, String requesterId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param teamId
	 * @param requesterId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public PaginatedResults<MembershipRqstSubmission> getOpenSubmissionsByTeamAndRequesterInRange(
			UserInfo userInfo, String teamId, String requesterId, long limit,
			long offset) throws DatastoreException, NotFoundException;

	/**
	 * Create the notification content
	 * 
	 * @param mrs
	 * @return the message metadata and the message content, one for each team administrator
	 */
	public List<MessageToUserAndBody> createMembershipRequestNotification(MembershipRqstSubmission mrs,
			String acceptRequestEndpoint, String notificationUnsubscribeEndpoint);

	/**
	 * Retrieve all open request submissions for teams of which user is admin
	 * 
	 * @param userInfo
	 * @return
	 */
	public Count getOpenSubmissionsCountForTeamAdmin(UserInfo userInfo);

}
