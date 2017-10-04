package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipInvitationManager {
	
	/**
	 * Invite someone to join the team.  Note, the invitee list can include a group/team, as shorthand for all users in said group/team.
	 *
	 * @param userInfo
	 * @param mis
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public MembershipInvtnSubmission create(UserInfo userInfo, MembershipInvtnSubmission mis) throws  DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * Create an invitation message addressed to an existing user
	 *
	 * @param mis
	 * @param acceptInvitationEndpoint
	 * @param notificationUnsubscribeEndpoint
	 * @return
	 * @throws NotFoundException
	 */
	MessageToUserAndBody createInvitationToUser(MembershipInvtnSubmission mis, String acceptInvitationEndpoint, String notificationUnsubscribeEndpoint) throws NotFoundException;

	/**
	 * Send an invitation message to an email address
	 *
	 * @param mis
	 * @param acceptInvitationEndpoint
	 * @param notificationUnsubscribeEndpoint
	 * @return
	 * @throws NotFoundException
	 */
	void sendInvitationToEmail(MembershipInvtnSubmission mis, String acceptInvitationEndpoint, String notificationUnsubscribeEndpoint) throws NotFoundException;

	/**
	 * Retrieve an invitation by its ID
	 *
	 * @param userInfo
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipInvtnSubmission get(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	/**
	 * Retrieve an invitation by its ID using a signed token for authorization
	 *
	 * @param misId
	 * @param token
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipInvtnSubmission get(String misId, MembershipInvtnSignedToken token) throws DatastoreException, NotFoundException;

	/**
	 * Delete an invitation
	 * 
	 * @param userInfo
	 * @param id
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void delete(UserInfo userInfo, String id) throws DatastoreException, UnauthorizedException, NotFoundException; 
	
	/**
	 * Get the Invitations for the given user
	 * 
	 * @param principalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<MembershipInvitation> getOpenForUserInRange(String principalId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * Get the Invitations for the given user, to join the given team
	 *
	 * @param principalId
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public PaginatedResults<MembershipInvitation> getOpenForUserAndTeamInRange(String principalId, String teamId, long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 */
	public PaginatedResults<MembershipInvtnSubmission> getOpenSubmissionsForTeamInRange(
			UserInfo userInfo, String teamId, long limit, long offset) throws NotFoundException;

	/**
	 * 
	 * @param userInfo
	 * @param inviteeId
	 * @param teamId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 */
	public PaginatedResults<MembershipInvtnSubmission> getOpenSubmissionsForUserAndTeamInRange(
			UserInfo userInfo, String inviteeId, String teamId, long limit,
			long offset)  throws NotFoundException;

	/**
	 * Retrieve the number of open invitation for a user
	 * 
	 * @param principalId
	 * @return
	 */
	public Count getOpenInvitationCountForUser(String principalId);

	/**
	 * Verify whether the inviteeEmail of the indicated MembershipInvtnSubmission is associated with the given user.
	 * Return an InviteeVerificationSignedToken if the verification succeeds.
	 * Throw UnauthorizedException if it fails.
	 *
	 * @param userId
	 * @param membershipInvitationId
	 * @param token
	 * @return
	 */
	public InviteeVerificationSignedToken verifyInvitee(Long userId, String membershipInvitationId, MembershipInvtnSignedToken token);

	/**
	 * Set the inviteeId of the indicated MembershipInvtnSubmission if the given token is valid.
	 * The indicated mis must have null inviteeId and a non null inviteeEmail.
	 *
	 * @param userId
	 * @param misId
	 * @param token
	 * @return
	 */
	public void updateInviteeId(Long userId, String misId, InviteeVerificationSignedToken token);
}
