/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
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
 * Teams are groups of users.  Teams can be granted access permissions to projects, folders and files, and share other resources.
 * 
 * @author brucehoff
 *
 */
@ControllerInfo(displayName="Team Services", path="repo/v1")
@Controller
public class TeamController extends BaseController {
	@Autowired
	ServiceProvider serviceProvider;
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TEAM, method = RequestMethod.POST)
	public @ResponseBody
	Team createTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody Team team) throws NotFoundException {
		return serviceProvider.getTeamService().create(userId, team);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Team> getTeamsByNameFragment(
			@RequestParam(value = UrlHelpers.NAME_FRAGMENT_FILTER, required = false) String fragment,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset
			) {
		return serviceProvider.getTeamService().get(fragment, limit, offset);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_TEAM, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Team> getTeamsByMember(
			@PathVariable String id,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset
			) {
		return serviceProvider.getTeamService().getByMember(id, limit, offset);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM_ID, method = RequestMethod.GET)
	public @ResponseBody
	Team getTeam(
			@PathVariable String id
			) throws NotFoundException {
		return serviceProvider.getTeamService().get(id);
	}
	
	@RequestMapping(value = UrlHelpers.TEAM_ID_ICON, method = RequestMethod.GET)
	public
	void fileRedirectURLForTeamIcon(
			@PathVariable String id,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response
			) throws NotFoundException, IOException  {
		URL redirectUrl = serviceProvider.getTeamService().getIconURL(id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM, method = RequestMethod.PUT)
	public @ResponseBody
	Team updateTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody Team team
			) throws NotFoundException {
		return serviceProvider.getTeamService().update(userId, team);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_ID, method = RequestMethod.DELETE)
	public void deleteTeam(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId
			) throws NotFoundException {
		serviceProvider.getTeamService().delete(userId, id);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER_ID, method = RequestMethod.PUT)
	public void addTeamMember(
			@PathVariable String id,
			@PathVariable String principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId
			) throws NotFoundException {
		serviceProvider.getTeamService().addMember(userId, id, principalId);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER, method = RequestMethod.GET)
	public  @ResponseBody 
	PaginatedResults<TeamMember> getTeamMembers(
			@PathVariable String id,
			@RequestParam(value = UrlHelpers.NAME_FRAGMENT_FILTER, required = false) String fragment,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset
			) throws NotFoundException {
		return serviceProvider.getTeamService().getMembers(id, fragment, limit, offset);
	}
	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER_ID, method = RequestMethod.DELETE)
	public void removeTeamMember(
			@PathVariable String id,
			@PathVariable String principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId
			) throws NotFoundException {
		serviceProvider.getTeamService().removeMember(userId, id, principalId);
	}	
}
