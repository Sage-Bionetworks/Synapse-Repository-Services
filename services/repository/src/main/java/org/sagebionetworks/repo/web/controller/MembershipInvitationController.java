/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The Membership Invitation Services create, retrieve and delete 
 * membership invitations.  A membership invitation is created by a Team administrator
 * to invite a Synapse user to join the Team.  Without the invitation it is not possible
 * for an outside user to join.  For more on Teams, see
 * <a href="#org.sagebionetworks.repo.web.controller.TeamController">Team Services</a>.
 *
 */
@ControllerInfo(displayName="Membership Invitation Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class MembershipInvitationController extends BaseController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Create a membership invitation.  The Team and invitee must be specified.  Optionally,
	 * the creator may include an invitation message and/or expiration date for the invitation.
	 * If no expiration date is specified then the invitation never expires.
	 * Note:  The client must be an administrator of the specified Team to make this request.
	 * @param userId
	 * @param invitation
	 * @param acceptInvitationEndpoint the portal end-point for one-click acceptance of the membership
	 * invitation.  A signed, serialized token is appended to create the complete URL:
	 * <ahref="${org.sagebionetworks.repo.model.JoinTeamSignedToken}">JoinTeamSignedToken</a>
	 * @param notificationUnsubscribeEndpoint the portal prefix for one-click email unsubscription.  
	 * A signed, serialized token is appended to create the complete URL: 
	 * <ahref="${org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken}">NotificationSettingsSignedToken</a>

	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION, method = RequestMethod.POST)
	public @ResponseBody
	MembershipInvtnSubmission createInvitation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.ACCEPT_INVITATION_ENDPOINT_PARAM, required = false) String acceptInvitationEndpoint,
			@RequestParam(value = AuthorizationConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM, required = false) String notificationUnsubscribeEndpoint,
			@RequestBody MembershipInvtnSubmission invitation
			) throws NotFoundException {
		return serviceProvider.
				getMembershipInvitationService().
				create(userId, invitation, 
						acceptInvitationEndpoint, 
						notificationUnsubscribeEndpoint);
	}

	/**
	 * Retrieve the open invitations to a user, optionally filtering by the Team of origin.
	 * An invitation is only open if it has not expired and if the user has not joined the Team.
	 * @param id the ID of the Synapse user to which invitations have been extended.
	 * @param teamId the ID of the Team extending the invitations (optional)
	 * @param limit the maximum number of invitations to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_INVITATION_BY_USER, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MembershipInvitation> getOpenInvitationsByUser(
			@PathVariable String id,
			@RequestParam(value = UrlHelpers.TEAM_ID_REQUEST_PARAMETER, required = false) String teamId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().getOpenInvitations(null, id, teamId, limit, offset);
	}

	/**
	 * Retrieve the open invitations from a Team, optionally filtering by the invitee.
	 * An invitation is only open if it has not expired and if the user has not joined the Team.
	 * @param userId the ID of the user making the request
	 * @param id the ID of the Team extending the invitations  
	 * @param inviteeId the ID of the Synapse user to which invitations have been extended (optional)
	 * @param limit the maximum number of invitations to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_INVITATION_BY_TEAM, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MembershipInvtnSubmission> getOpenInvitationsByTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@RequestParam(value = UrlHelpers.INVITEE_ID_REQUEST_PARAMETER, required = false) String inviteeId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().getOpenInvitationSubmissions(userId, inviteeId, id, limit, offset);
	}

	/**
	 * Retrieve an invitation by ID
	 * Note:  The client must be an administrator of the specified Team to make this request.
	 * @param id the ID of the invitation
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_ID, method = RequestMethod.GET)
	public @ResponseBody
	MembershipInvtnSubmission getInvitation(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().get(userId, id);
	}

	/**
	 * Delete an invitation
	 * Note:  The client must be an administrator of the Team referenced by the invitation to make this request.
	 * @param id the ID of the invitation to be deleted
	 * @param userId
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_ID, method = RequestMethod.DELETE)
	public void deleteInvitation(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		serviceProvider.getMembershipInvitationService().delete(userId, id);
	}

	/**
	 * Retrieve the number of pending Membership Invitations
	 * @param userId
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_INVITATION_COUNT, method = RequestMethod.GET)
	public @ResponseBody Count getOpenInvitationCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) {
		return serviceProvider.getMembershipInvitationService().getOpenInvitationCount(userId);
	}
}
