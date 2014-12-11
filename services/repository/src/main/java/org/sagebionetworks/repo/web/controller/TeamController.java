/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
 * Teams are groups of users.  Teams can be granted access permissions to projects, 
 * folders and files, and share other resources by adding them to
 * <a href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
 * Lists (ACLs)</a>.  Any authenticated Synapse user
 * may create a Team, for which they become an administrator.  Team
 * administrators may:
 * <ul>
 * <li> invite other users to join the Team, 
 * <li> accept membership requests from users wishing to join the Team, 
 * <li> grant or revoke administrative control to Team members,
 * <li> remove a user from the Team.  
 * </ul>
 * <br>
 * Other Synapse users may:
 * <ul>
 * <li> issue membership requests to a Team,
 * <li> accept Team membership invitations (join the Team),
 * <li> unilaterally choose to leave a Team once added.
 * </ul>
 *
 */
@ControllerInfo(displayName="Team Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class TeamController extends BaseController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * 
	 * Create a new Team. The passed request body may contain the following fields:
	 * <ul>
	 * <li>name - Give your new Team a name.  The name must be unique, not used by an existing Team (required).</li>
	 * <li>description - a short text description of the Team's purpose (optional).</li>
	 * <li>icon - a fileHandle ID for an icon image file previously uploaded to Synapse (optional).</li>
	 * </ul>
	 * <p>
	 * To specify a Team icon, the icon file must first be uploaded to Synapse as 
	 * <a href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a> (see
	 * <a href="${org.sagebionetworks.file.controller.UploadController}">File
	 * Services</a>). The FileHandle ID can then be put into the Team's icon field.
	 * 
	 * 
	 * @param userId
	 * @param team
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TEAM, method = RequestMethod.POST)
	public @ResponseBody
	Team createTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Team team) throws NotFoundException {
		return serviceProvider.getTeamService().create(userId, team);
	}

	/**
	 * Retrieve a paginated list of Teams matching the supplied name fragment (optional), 
	 * in alphabetical order by Team name.
	 * <br>
	 * Note:  This service has JSONP support:  If the request parameter "callback=jsMethod" is included (where 
	 * 'jsMethod' is any function name you wish), then the response body will be wrapped in "jsMethod(...);".
	 * 
	 * @param fragment a prefix of the Team name, or a prefix of any sub-string in the name preceded by a space.  
	 * If omitted, all Teams are returned.  
	 * 
	 * @param limit the maximum number of Teams to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAMS, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Team> getTeamsByNameFragment(
			@RequestParam(value = UrlHelpers.NAME_FRAGMENT_FILTER, required = false) String fragment,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset
			) throws NotFoundException {
		return serviceProvider.getTeamService().get(fragment, limit, offset);
	}
	
	/**
	 * Retrieve a paginated list of Teams to which the given user belongs.
	 * 
	 * @param id the principal ID of the user of interest.
	 * @param limit the maximum number of Teams to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 */
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
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER_ID, method = RequestMethod.GET)
	public @ResponseBody
	TeamMember getTeamMember(
			@PathVariable String id,
			@PathVariable String principalId
			) throws NotFoundException {
		return  serviceProvider.getTeamService().getMember(id, principalId);
	}
	
	/**
	 * Retrieve the metadata for a specified Team.
	 * 
	 * @param id the ID of the Team of interest
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM_ID, method = RequestMethod.GET)
	public @ResponseBody
	Team getTeam(
			@PathVariable String id
			) throws NotFoundException {
		return serviceProvider.getTeamService().get(id);
	}
	
	/**
	 * Retrieve the download URL for the Team icon, or receive a redirect response to said URL.
	 * @param id the ID of the Team
	 * @param redirect if true or omitted, then redirect to the URL.  If false then simply return the URL.
	 * @param response
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.TEAM_ID_ICON, method = RequestMethod.GET)
	public
	void fileRedirectURLForTeamIcon(
			@PathVariable String id,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response
			) throws NotFoundException, IOException  {
		String redirectUrl = serviceProvider.getTeamService().getIconURL(id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Update the Team metadata for the specified Team.  
	 * Note: The client must be a Team administrator to make this request.
	 * @param userId
	 * @param team the new metadata for the Team
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM, method = RequestMethod.PUT)
	public @ResponseBody
	Team updateTeam(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Team team
			) throws NotFoundException {
		return serviceProvider.getTeamService().update(userId, team);
	}
	
	/**
	 * Delete the Team.
	 * Note: The client must be a Team administrator to make this request.
	 * @param id the ID of the Team to delete.
	 * @param userId
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_ID, method = RequestMethod.DELETE)
	public void deleteTeam(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		serviceProvider.getTeamService().delete(userId, id);
	}
	
	/**
	 * Add a member to the Team.
	 * If the one making the request is the user to be added, then the user must have
	 * an open invitation from the Team.  If the one making the request is an administrator
	 * on the Team, then there must be a pending request from the user to the Team, asking
	 * to be added.
	 * @param id the ID of the Team to which the user is to be added.
	 * @param principalId the ID of the user to be added to the Team.
	 * @param userId
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER_ID, method = RequestMethod.PUT)
	public void addTeamMember(
			@PathVariable String id,
			@PathVariable String principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		serviceProvider.getTeamService().addMember(userId, id, principalId);
	}
	
	/**
	 * Add or remove administrative permission for a Team member.
	 * Note: The client must be a Team administrator to make this request.
	 * @param id the Team ID
	 * @param principalId the user ID
	 * @param isAdmin if true, administrative permission is grant.  If false it is revoked.
	 * @param userId
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER_ID_PERMISSION, method = RequestMethod.PUT)
	public void setTeamAdmin(
			@PathVariable String id,
			@PathVariable String principalId,
			@RequestParam(value = UrlHelpers.TEAM_PERMISSION_REQUEST_PARAMETER, required = true) Boolean isAdmin,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		serviceProvider.getTeamService().setPermissions(userId, id, principalId, isAdmin);
	}
	
	/**
	 * Retrieve the Team Membership Status bundle for a team and user.  This says whether a user is
	 * a member of a Team, whether there are outstanding membership requests or invitations, and 
	 * whether the client making the request can add the given user to the given Team.
	 * @param id the Team ID
	 * @param principalId the user ID
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER_ID_MEMBERSHIP_STATUS, method = RequestMethod.GET)
	public @ResponseBody 
	TeamMembershipStatus getTeamMembershipStatus(
			@PathVariable String id,
			@PathVariable String principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		return serviceProvider.getTeamService().getTeamMembershipStatus(userId, id, principalId);
	}
		
	/**
	 * Retrieve a paginated list of Team members matching the supplied name prefix.  If the prefix 
	 * is omitted then all members are returned.
	 * <br>
	 * Note:  This service has JSONP support:  If the request parameter "callback=jsMethod" is included (where 
	 * 'jsMethod' is any function name you wish), then the response body will be wrapped in "jsMethod(...);".
	 * @param id the id of the Team of interest
	 * @param fragment a prefix of the user's first or last name or email address (optional)
	 * @param limit the maximum number of members to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.TEAM_MEMBERS_ID, method = RequestMethod.GET)
	public  @ResponseBody 
	PaginatedResults<TeamMember> getTeamMembers(
			@PathVariable String id,
			@RequestParam(value = UrlHelpers.NAME_FRAGMENT_FILTER, required = false) String fragment,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset
			) throws NotFoundException {
		return serviceProvider.getTeamService().getMembers(id, fragment, limit, offset);
	}
	
	/**
	 * Remove the given member from the specified Team.
	 * Note:  The client must either be a Team administrator or the member being removed.
	 * @param id the Team ID
	 * @param principalId the member's principal ID
	 * @param userId
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_ID_MEMBER_ID, method = RequestMethod.DELETE)
	public void removeTeamMember(
			@PathVariable String id,
			@PathVariable String principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException {
		serviceProvider.getTeamService().removeMember(userId, id, principalId);
	}	
	
	/**
	 * Refresh the cached prefix tree of Team and member names.  Note:  This is for testing only and
	 * may only be invoked by a Synapse administrator.
	 * @param userId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.TEAM_UPDATE_SEARCH_CACHE, method = RequestMethod.POST)
	public void refreshCache(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws NotFoundException, DatastoreException, UnauthorizedException {
		serviceProvider.getTeamService().refreshCache(userId);
	}	
}
