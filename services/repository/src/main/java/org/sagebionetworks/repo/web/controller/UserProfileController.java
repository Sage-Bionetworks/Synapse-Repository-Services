package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Every Synapse user has an associated <a href="${org.sagebionetworks.repo.model.UserProfile}">UserProfile</a>.
 */
@ControllerInfo(displayName="User Profile Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class UserProfileController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Get the profile of the caller (my profile).
	 * <p><b>Note:</b> Private user profile fields will be returned.</p>
	 * @param userId
	 *             The user that is making the request.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.GET)
	public @ResponseBody
	UserProfile getMyOwnUserProfile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getUserProfileService().getMyOwnUserProfile(userId);
	}

	/**
	 * Get the profile of a specified user.
	 * <p><b>Note:</b> Private fields (e.g. "rStudioUrl") are omitted unless the requester is the profile owner or an administrator.</p>
	 * @param userId
	 *            The user that is making the request.
	 * @param profileId 
	 *            The target profile owner ID (the "id" field returned in the "/user" request).
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE_ID, method = RequestMethod.GET)
	public @ResponseBody
	UserProfile getUserProfileByOwnerId(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String profileId,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getUserProfileService().getUserProfileByOwnerId(userId, profileId);
	}

	/**
	 * Get all publicly available <a href="${org.sagebionetworks.repo.model.UserProfile}">UserProfile</a> data in the system
	 * @param offset
	 *        The offset index determines where this page will start from. An index of 0 is the first item. <p><i>Default is 0</i></p>
	 * @param limit
	 *        Limits the number of items that will be fetched for this page. <p><i>Default is 100</i></p>
	 * @param sort
	 *        Used to indicate upon which field(s) to sort. <p><i>Default is NONE</i></p>
	 * @param ascending
	 *        Used to indicate whether the sort direction is ascending or not.  <p><i>Default is true</i></p>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<UserProfile> getUserProfilesPaginated(HttpServletRequest request,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending
			) throws DatastoreException, UnauthorizedException, NotFoundException {		
		return serviceProvider.getUserProfileService().getUserProfilesPaginated(request, userId, offset, limit, sort, ascending);
	}

	/**
	 * Update your own profile
	 * <p><b>Note: </b> The user associated with the UserProfile "ownerId" must match the identity of the caller, 
	 * otherwise an Unauthorized response will occur.</p>
	 * @param userId
	 * 		The user that is making the request.
	 * @return The updated <a href="${org.sagebionetworks.repo.model.UserProfile}">UserProfile</a>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.PUT)
	public @ResponseBody
	UserProfile updateUserProfile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		return serviceProvider.getUserProfileService().updateUserProfile(userId, header, request);
	}
	
	/**
	 * Batch get UserGroupHeaders.
	 * This fetches information about a collection of users or groups, specified by Synapse IDs.
	 * 
	 * @param ids IDs are specified as request parameters at the end of the URL, separated by commas.  <p>For example: <pre class="prettyprint">ids=1001,819</pre></p>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_GROUP_HEADERS_BATCH, method = RequestMethod.GET)
	public @ResponseBody
	UserGroupHeaderResponsePage getUserGroupHeadersByIds(
			@RequestHeader HttpHeaders header,
			@RequestParam(value = UrlHelpers.IDS_PATH_VARIABLE, required = true) String ids,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException {

		String[] idsArray = ids.split(",");
		List<String> idsList = new ArrayList<String>(Arrays.asList(idsArray));
		List<Long> longList = new LinkedList<Long>();
		for(String stringId: idsList){
			longList.add(Long.parseLong(stringId));
		}
		// convert to a list of longs
		return serviceProvider.getUserProfileService().getUserGroupHeadersByIds(userId, longList);
	}

	/**
	 * Batch get UserGroupHeaders.
	 * This fetches information about a collection of users or groups, specified by Synapse IDs.
	 *
	 * @param ids IDs are specified as request parameters at the end of the URL, separated by commas. <p>For example: <pre class="prettyprint">ids=1001,819</pre></p>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.POST)
	public @ResponseBody
	ListWrapper<UserProfile> listUserProfiles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody IdList ids) throws DatastoreException, NotFoundException {
		return serviceProvider.getUserProfileService().listUserProfiles(userId, ids);
	}

	/**
	 * Get Users and Groups by name.
	 * 
	 * @param prefixFilter The name to search for.
	 * @param offset
	 *         The offset index determines where this page will start from. An index of 0 is the first item. <p><i>Default is 0</i></p> 
	 * @param limit
	 * 			Limits the number of items that will be fetched for this page. <p><i>Default is 10</i></p>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_GROUP_HEADERS, method = RequestMethod.GET)
	public @ResponseBody
	UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(
			@RequestParam(value = UrlHelpers.PREFIX_FILTER, required = false) String prefixFilter,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getUserProfileService().getUserGroupHeadersByPrefix(prefixFilter, offset, limit, header, request);
	}
	
	/**
	 * Get the actual URL of the image file associated with a user's profile.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.USER_PROFILE_IMAGE, method = RequestMethod.GET)
	public @ResponseBody
	void imageRedirectURLForUser(
			@PathVariable String profileId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getUserProfileService().getUserProfileImage(profileId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Get the actual URL of the image file associated with a user's profile.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.USER_PROFILE_IMAGE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void imagePreviewRedirectURLForUser(
			@PathVariable String profileId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getUserProfileService().getUserProfileImagePreview(profileId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Add an <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a> as a <a href="${org.sagebionetworks.repo.model.Favorite}">Favorite</a> of the caller.
	 * @param id
	 *        Entity ID of the favorite <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a>
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.FAVORITE_ID
			}, method = RequestMethod.POST)
	public @ResponseBody
	EntityHeader addFavorite(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {
		return serviceProvider.getUserProfileService().addFavorite(userId, id);
	}

	/**
	 * Remove an <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a> as a <a href="${org.sagebionetworks.repo.model.Favorite}">Favorite</a> of the caller.
	 * @param id
	 *       Entity ID of the <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a> that should be removed as a favorite
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 
			UrlHelpers.FAVORITE_ID
			}, method = RequestMethod.DELETE)
	public @ResponseBody
	void removeFavorite(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {
		serviceProvider.getUserProfileService().removeFavorite(userId, id);
	}

	/**
	 * Get a paginated result that contains the 
	 * caller's <a href="${org.sagebionetworks.repo.model.Favorite}">Favorites</a> 
	 * @param offset
	 * 			The offset index determines where this page will start from. An index of 0 is the first item. <p><i>Default is 0</i></p>
	 * @param limit
	 *          Limits the number of items that will be fetched for this page. <p><i>Default is 10</i></p>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.FAVORITE
	}, method = RequestMethod.GET) 
	public @ResponseBody
	PaginatedResults<EntityHeader> getFavorites(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit) 
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getFavorites(userId, limit, offset);
	}

	/**
	 * Get a paginated result that contains the caller's <a
	 * href="${org.sagebionetworks.repo.model.Project}">projects</a>. The list is ordered by most recent interacted with
	 * project first
	 * 
	 * @param offset The offset index determines where this page will start from. An index of 0 is the first item.
	 *        <i>Default is 0</i>
	 * @param limit Limits the number of items that will be fetched for this page. <i>Default is 10</i>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.MY_PROJECTS }, method = RequestMethod.GET)
	@Deprecated
	public @ResponseBody
	PaginatedResults<ProjectHeader> getMyProjects(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, null, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, limit, offset);
	}

	/**
	 * Get a paginated result that contains the <a href="${org.sagebionetworks.repo.model.Project}">projects</a> from a
	 * user. The list is ordered by most recent interacted with project first
	 * 
	 * @param offset The offset index determines where this page will start from. An index of 0 is the first item.
	 *        <i>Default is 0</i>
	 * @param limit Limits the number of items that will be fetched for this page. <i>Default is 10</i>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_FOR_USER }, method = RequestMethod.GET)
	@Deprecated
	public @ResponseBody
	PaginatedResults<ProjectHeader> getProjectsForUser(
			@PathVariable Long principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, principalId, null, ProjectListType.OTHER_USER_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, limit, offset);
	}

	/**
	 * Get a paginated result that contains the <a href="${org.sagebionetworks.repo.model.Project}">projects</a> from a
	 * user. The list is ordered by most recent interacted with project first
	 * 
	 * @param offset The offset index determines where this page will start from. An index of 0 is the first item.
	 *        <i>Default is 0</i>
	 * @param limit Limits the number of items that will be fetched for this page. <i>Default is 10</i>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_FOR_TEAM }, method = RequestMethod.GET)
	public @ResponseBody
	@Deprecated
	PaginatedResults<ProjectHeader> getProjectsForTeam(
			@PathVariable Long teamId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, null, teamId, ProjectListType.TEAM_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, limit, offset);
	}

	/**
	 * Get a paginated result that contains the <a href="${org.sagebionetworks.repo.model.ProjectHeader}">project
	 * headers</a> from a user. The list is ordered by most recent interacted with project first
	 * 
	 * @param type The type of project list
	 * @param sortColumn The optional column to sort on. <i>Default sort by last activity</i>
	 * @param sortDirection The optional sort direction. <i>Default sort descending</i>
	 * @param offset The offset index determines where this page will start from. An index of 0 is the first item.
	 *        <i>Default is 0</i>
	 * @param limit Limits the number of items that will be fetched for this page. <i>Default is 10</i>
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ProjectHeader> getProjects(
			@PathVariable ProjectListType type,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, null, null, type, sortColumn, sortDirection, limit, offset);
	}

	/**
	 * Same as getProjects, but has team parameter
	 * 
	 * @param teamId The team ID to list projects for, when showing ProjectListType.TEAM_PROJECTS
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_TEAM }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ProjectHeader> getProjectsTeam(
			@PathVariable ProjectListType type,
			@PathVariable Long teamId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, null, teamId, type, sortColumn, sortDirection, limit, offset);
	}

	/**
	 * Same as getProjects, but has other user id parameter
	 * 
	 * @param principalId The user ID to list projects for, when showing ProjectListType.OTHER_USER_PROJECTS
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_USER }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ProjectHeader> getProjectsUser(
			@PathVariable ProjectListType type,
			@PathVariable Long principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, principalId, null, type, sortColumn, sortDirection, limit, offset);
	}
}
