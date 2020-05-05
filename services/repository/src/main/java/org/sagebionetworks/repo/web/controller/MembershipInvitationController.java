/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
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
public class MembershipInvitationController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * <p>Create a membership invitation and send an email notification to the invitee. The team must be specified.
	 * Also, either an inviteeId or an inviteeEmail must be specified. Optionally, the creator may include an
	 * invitation message and/or expiration date for the invitation. If no expiration date is specified then the
	 * invitation never expires.</p>
	 *
	 * <p>Note: The client must be an administrator of the specified Team to make this request.</p>
	 *
	 * @param userId
	 * @param invitation
	 * @param acceptInvitationEndpoint The portal endpoint prefix for one-click acceptance of the membership invitation.
	 * A signed, serialized token is appended to create the complete URL:
	 * <a href="${org.sagebionetworks.repo.model.JoinTeamSignedToken}">JoinTeamSignedToken</a>
	 * if an inviteeId is specified, or
	 * <a href="${org.sagebionetworks.repo.model.MembershipInvtnSignedToken}">MembershipInvtnSignedToken</a>
	 * if an inviteeEmail is specified.
	 * In normal operation, this parameter should be omitted.
	 * @param notificationUnsubscribeEndpoint The portal endpoint prefix for one-click email unsubscription.
	 * A signed, serialized token is appended to create the complete URL:
	 * <a href="${org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken}">NotificationSettingsSignedToken</a>.
	 * In normal operation, this parameter should be omitted.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION, method = RequestMethod.POST)
	public @ResponseBody
	MembershipInvitation createInvitation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.ACCEPT_INVITATION_ENDPOINT_PARAM, required = false) String acceptInvitationEndpoint,
			@RequestParam(value = AuthorizationConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM, defaultValue = ServiceConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT) String notificationUnsubscribeEndpoint,
			@RequestBody MembershipInvitation invitation
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
	 *
	 * Note: certain fields may be omitted when returned if the field value is null
	 * @param id the ID of the Synapse user to which invitations have been extended.
	 * @param teamId the ID of the Team extending the invitations (optional)
	 * @param limit the maximum number of invitations to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
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
	 *
	 * Note: certain fields may be omitted when returned if the field value is null
	 * @param userId the ID of the user making the request
	 * @param id the ID of the Team extending the invitations  
	 * @param inviteeId the ID of the Synapse user to which invitations have been extended (optional)
	 * @param limit the maximum number of invitations to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_INVITATION_BY_TEAM, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MembershipInvitation> getOpenInvitationsByTeam(
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
	 * Note:  The client must be an administrator of the Team referenced by the invitation or the invitee to make this request.
	 * @param id the ID of the invitation
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_ID, method = RequestMethod.GET)
	public @ResponseBody
	MembershipInvitation getInvitation(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().get(userId, id);
	}

	/**
	 * Retrieve an invitation by ID using a MembershipInvtnSignedToken for authorization
	 * @param id
	 * @param token
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_ID, method = RequestMethod.POST)
	public @ResponseBody
	MembershipInvitation getInvitation(
			@PathVariable String id,
			@RequestBody MembershipInvtnSignedToken token
	) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().get(id, token);
	}

	/**
	 * Delete an invitation
	 * Note:  The client must be an administrator of the Team referenced by the invitation or the invitee to make this request.
	 * @param id the ID of the invitation to be deleted
	 * @param userId
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
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
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_INVITATION_COUNT, method = RequestMethod.GET)
	public @ResponseBody Count getOpenInvitationCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) {
		return serviceProvider.getMembershipInvitationService().getOpenInvitationCount(userId);
	}

	/**
	 * Verify whether the inviteeEmail of the indicated MembershipInvitation is associated with the authenticated user.
	 * If it is, the response body will contain an InviteeVerificationSignedToken.
	 * If it is not, a response status 403 Forbidden will be returned.
	 * InviteeVerificationSignedTokens generated by this service expire 24 hours from creation.
	 * See https://sagebionetworks.jira.com/wiki/spaces/PLFM/pages/143628166/Invite+a+new+user+to+join+a+team for more information.
	 * @param membershipInvitationId
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_VERIFY_INVITEE, method = RequestMethod.GET)
	public @ResponseBody InviteeVerificationSignedToken getInviteeVerificationSignedToken(
			@PathVariable("id") String membershipInvitationId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().getInviteeVerificationSignedToken(userId, membershipInvitationId);
	}

	/**
	 * Set the inviteeId of a MembershipInvitation.
	 * A valid InviteeVerificationSignedToken must have an inviteeId equal to the id of
	 * the authenticated user and a membershipInvitationId equal to the id in the URI.
	 * This call will only succeed if the indicated MembershipInvitation has a
	 * null inviteeId and a non null inviteeEmail.
	 * See https://sagebionetworks.jira.com/wiki/spaces/PLFM/pages/143628166/Invite+a+new+user+to+join+a+team for more information.
	 * @param membershipInvitationId
	 * @param userId
	 * @param token
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_UPDATE_INVITEE_ID, method = RequestMethod.PUT)
	public void updateInviteeId(
			@PathVariable("id") String membershipInvitationId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody InviteeVerificationSignedToken token) {
		serviceProvider.getMembershipInvitationService().updateInviteeId(userId, membershipInvitationId, token);
	}
}
