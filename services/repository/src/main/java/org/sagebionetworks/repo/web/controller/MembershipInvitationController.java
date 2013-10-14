/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
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
 * @author brucehoff
 *
 */
@ControllerInfo(displayName="Membership Invitation Services", path="repo/v1")
@Controller
public class MembershipInvitationController extends BaseController {
	@Autowired
	ServiceProvider serviceProvider;
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION, method = RequestMethod.POST)
	public @ResponseBody
	MembershipInvtnSubmission createInvitation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody MembershipInvtnSubmission invitation
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().create(userId, invitation);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.OPEN_MEMBERSHIP_INVITATION, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<MembershipInvitation> getOpenInvitations(
			@PathVariable String id,
			@RequestParam(value = UrlHelpers.TEAM_ID_REQUEST_PARAMETER, required = false) String teamId,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().getOpenInvitations(id, teamId, limit, offset);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_ID, method = RequestMethod.GET)
	public @ResponseBody
	MembershipInvtnSubmission getInvitation(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId
			) throws NotFoundException {
		return serviceProvider.getMembershipInvitationService().get(userId, id);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.MEMBERSHIP_INVITATION_ID, method = RequestMethod.DELETE)
	public void deleteInvitation(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId
			) throws NotFoundException {
		serviceProvider.getMembershipInvitationService().delete(userId, id);
	}
}
