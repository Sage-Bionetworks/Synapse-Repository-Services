package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	public MembershipInvitation create(UserInfo userInfo, MembershipInvitation mis) throws  DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * Send an invitation message addressed to an existing user
	 *
	 * @param user The user that is sending the invitation
	 * @param mis The invitation, the {@link MembershipInvitation#getInviteeId()} must be present
	 * @param acceptInvitationEndpoint
	 * @param notificationUnsubscribeEndpoint
	 * @return
	 * @throws NotFoundException
	 */
	void sendInvitationEmailToSynapseUser(UserInfo user, MembershipInvitation mis, String acceptInvitationEndpoint, String notificationUnsubscribeEndpoint) throws NotFoundException;

	/**
	 * Send an invitation message to an email address
	 *
	 * @param user The user that is sending the invitation
	 * @param mis The invitation, the {@link MembershipInvitation#getInviteeEmail()} must be present
	 * @param acceptInvitationEndpoint
	 * @return
	 * @throws NotFoundException
	 */
	void sendInvitationEmailToEmail(UserInfo user, MembershipInvitation mis, String acceptInvitationEndpoint) throws NotFoundException;

	/**
	 * Retrieve an invitation by its ID
	 *
	 * @param userInfo
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipInvitation get(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

	/**
	 * Retrieve an invitation by its ID using a signed token for authorization
	 *
	 * @param misId
	 * @param token
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipInvitation get(String misId, MembershipInvtnSignedToken token) throws DatastoreException, NotFoundException;

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
	public PaginatedResults<MembershipInvitation> getOpenSubmissionsForTeamInRange(
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
	public PaginatedResults<MembershipInvitation> getOpenSubmissionsForUserAndTeamInRange(
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
	 * Verify whether the inviteeEmail of the indicated MembershipInvitation is associated with the given user.
	 * Return an InviteeVerificationSignedToken if the verification succeeds.
	 * Throw UnauthorizedException if it fails.
	 *
	 * @param userId
	 * @param membershipInvitationId
	 * @param token
	 * @return
	 */
	public InviteeVerificationSignedToken getInviteeVerificationSignedToken(Long userId, String membershipInvitationId);

	/**
	 * Set the inviteeId of the indicated MembershipInvitation if the given token is valid.
	 * The indicated mis must have null inviteeId and a non null inviteeEmail.
	 *
	 * @param userId
	 * @param misId
	 * @param token
	 * @return
	 */
	public void updateInviteeId(Long userId, String misId, InviteeVerificationSignedToken token);
}
