package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MembershipInvitationManager {
	
	/**
	 * Invite someone to join the team.  Note, the invitee list can include a group/team, as shorthand for all users in said group/team.
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
	 * Retrieve an invitation by its ID
	 * @param userInfo
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public MembershipInvtnSubmission get(UserInfo userInfo, String id) throws DatastoreException, NotFoundException;

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
	public QueryResults<MembershipInvitation> getOpenForUserInRange(String principalId, long offset, long limit) throws DatastoreException, NotFoundException;

	/**
	 * Get the Invitations for the given user, to join the given team
	 * @param principalId
	 * @param teamId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public QueryResults<MembershipInvitation> getOpenForUserAndTeamInRange(String principalId, String teamId, long offset, long limit) throws DatastoreException, NotFoundException;

}
