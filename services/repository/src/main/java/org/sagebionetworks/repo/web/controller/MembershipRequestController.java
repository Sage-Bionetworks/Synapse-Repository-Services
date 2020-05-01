package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.MembershipRequest;
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
 * The Membership Request Services create, retrieve and delete 
 * membership requests.  A membership request is created by a Synapse user
 * to request admission to a Team.  Without the request it is not possible
 * for a Team to admit the user.  For more on Teams, see
 * <a href="#org.sagebionetworks.repo.web.controller.TeamController">Team Services</a>.
 *
 */
@ControllerInfo(displayName="Membership Request Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class MembershipRequestController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * <p>Create a membership request and send an email notification to the administrators of the team.
	 * The Team must be specified. Optionally, the creator may include a message and/or expiration date for the request.
	 * If no expiration date is specified then the request never expires.</p>
	 *
	 * @param userId
	 * @param request
	 * @param acceptRequestEndpoint
	 * @param acceptRequestEndpoint The portal end-point for one-click acceptance of the membership
	 * request.  A signed, serialized token is appended to create the complete:
	 * <a href="${org.sagebionetworks.repo.model.JoinTeamSignedToken}">JoinTeamSignedToken</a>
	 * In normal operation, this parameter should be omitted.
	 * @param notificationUnsubscribeEndpoint The portal prefix for one-click email unsubscription.
	 * A signed, serialized token is appended to create the complete:
	 * <a href="${org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken}">NotificationSettingsSignedToken</a>.
	 * In normal operation, this parameter should be omitted.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_REQUEST, method = RequestMethod.POST)
	public @ResponseBody
	MembershipRequest createRequest(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.ACCEPT_REQUEST_ENDPOINT_PARAM, defaultValue = ServiceConstants.ACCEPT_REQUEST_ENDPOINT) String acceptRequestEndpoint,
			@RequestParam(value = AuthorizationConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM, defaultValue = ServiceConstants.NOTIFICATION_UNSUBSCRIBE_ENDPOINT) String notificationUnsubscribeEndpoint,
			@RequestBody MembershipRequest request
			) throws NotFoundException {
		return serviceProvider.getMembershipRequestService().create(userId, request, acceptRequestEndpoint, notificationUnsubscribeEndpoint);
	}

	/**
	 * Retrieve the open requests submitted to a Team, optionally filtering by the requester.
	 * An request is only open if it has not expired and if the requester has not been added the Team.
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum frequency this method can be called</td>
	 * <td>40 calls per minute</td>
	 * </tr>
	 * </table>
	 * </p>
	 * @param id Team ID
	 * @param userId
	 * @param requestorId the ID of the user requesting admission to the Team
	 * @param limit the maximum number of requests to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_REQUEST_FOR_TEAM, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MembershipRequest> getOpenRequestsByTeam(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.REQUESTOR_ID_REQUEST_PARAMETER, required = false) String requestorId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset
			) throws NotFoundException {
		return serviceProvider.getMembershipRequestService().getOpenRequests(userId, requestorId, id, limit, offset);
	}

	/**
	 * Retrieve the open requests submitted by a user, optionally filtering by the Team.
	 * An request is only open if it has not expired and if the requester has not been added the Team.
	 * Note:  The 'id' in the URI must be the same ID as that of the authenticated user issuing the request.
	 * 
	 * @param id User ID
	 * @param userId
	 * @param teamId

	 * @param limit the maximum number of requests to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_REQUEST_FOR_USER, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MembershipRequest> getOpenRequestsByUser(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.TEAM_ID_PARAM, required = false) String teamId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset
			) throws NotFoundException {
		return serviceProvider.getMembershipRequestService().getOpenRequestSubmissions(userId, id, teamId, limit, offset);
	}

	/**
	 * Retrieve an request by ID
	 * Note:  The client must be the creator of the membership request to make this request.
	 * @param id the Team ID
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_REQUEST_ID, method = RequestMethod.GET)
	public @ResponseBody
	MembershipRequest getRequest(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		return serviceProvider.getMembershipRequestService().get(userId, id);
	}

	/**
	 * Delete a request
	 * Note:  The client must be the creator of the membership request to make this request.
	 * 
	 * @param id
	 * @param userId
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_REQUEST_ID, method = RequestMethod.DELETE)
	public void deleteRequest(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		serviceProvider.getMembershipRequestService().delete(userId, id);
	}

	/**
	 * Retrieve the number of pending Membership Requests for teams that user is admin
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_REQUEST_COUNT, method = RequestMethod.GET)
	public @ResponseBody Count getOpenMembershipRequestCount(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) {
		return serviceProvider.getMembershipRequestService().getOpenMembershipRequestCount(userId);
	}
}
