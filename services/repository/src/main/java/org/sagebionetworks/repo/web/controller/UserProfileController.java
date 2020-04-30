package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ProjectListTypeDeprecated;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.principal.AliasList;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.principal.UserGroupHeaderResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
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
public class UserProfileController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Get the profile of the caller (my profile).
	 * <p><b>Note:</b> Private user profile fields will be returned.</p>
	 * @param userId
	 *             The user who is making the request.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.GET)
	public @ResponseBody
	UserProfile getMyOwnUserProfile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getUserProfileService().getMyOwnUserProfile(userId);
	}

	/**
	 * Get the user bundle of the caller (my own bundle).
	 * <p><b>Note:</b> Private fields will be returned.</p>
	 * @param userId The user who is making the request.
	 * @param mask integer flag defining which components to include in the bundle
	 * 
	 * <p> This integer is used as a bit-string of flags to specify which parts to include 
	 *  in the UserBundle. The mask is defined as follows:
	 * <ul>
	 * <li>	UserProfile  = 0x1 </li>
	 * <li> ORCID  = 0x2 </li>
	 * <li> VerificationSubmission  = 0x4 </li>
	 * <li> IsCertified = 0x8 </li>
	 * <li> Is Verified  = 0x10 </li>
	 * <li> Is ACT Member = 0x20 </li>
	 * </ul>
	 * </p>
	 *
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_BUNDLE, method = RequestMethod.GET)
	public @ResponseBody
	UserBundle getMyOwnUserBundle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam int mask
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getUserProfileService().getMyOwnUserBundle(userId, mask);
	}

	/**
	 * Get the profile of a specified user.
	 * <p><b>Note:</b> Private fields (e.g. "rStudioUrl") are omitted unless the requester is the profile owner or an administrator.</p>
	 * @param userId The user who is making the request.
	 * @param profileId The target profile owner ID (the "id" field returned in the "/user" request).
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE_ID, method = RequestMethod.GET)
	public @ResponseBody
	UserProfile getUserProfileByOwnerId(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String profileId) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getUserProfileService().getUserProfileByOwnerId(userId, profileId);
	}
	
	/**
	 * Get the user bundle of a specified user.
	 * <p><b>Note:</b> Private fields (e.g. "rStudioUrl") are omitted unless the requester is the profile owner or an administrator.</p>
	 * @param userId The user who is making the request.
	 * @param profileId  The target profile owner ID
	 * @param mask integer flag defining which components to include in the bundle
	 * 
	 * <p> This integer is used as a bit-string of flags to specify which parts to include 
	 *  in the UserBundle. The mask is defined as follows:
	 * <ul>
	 * <li>	UserProfile  = 0x1 </li>
	 * <li> ORCID  = 0x2 </li>
	 * <li> VerificationSubmission  = 0x4 </li>
	 * <li> IsCertified = 0x8 </li>
	 * <li> Is Verified  = 0x10 </li>
	 * <li> Is ACT Member = 0x20 </li>
	 * </ul>
	 * </p>
	 *
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_BUNDLE_ID, method = RequestMethod.GET)
	public @ResponseBody
	UserBundle getUserBundleByOwnerId(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam int mask,
			@PathVariable String id
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getUserProfileService().getUserBundleByOwnerId(userId, id, mask);
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
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<UserProfile> getUserProfilesPaginated(HttpServletRequest request,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
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
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.PUT)
	public @ResponseBody
	UserProfile updateUserProfile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody UserProfile userProfile)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		return serviceProvider.getUserProfileService().updateUserProfile(userId, userProfile);
	}
	
	/**
	 * Update email notification settings.  Note: The request is authenticated by a hash
	 * message authentication code, generated by Synapse.  The intended use of this
	 * service is by the portal, completing a round trip with a 'one-click unsubscribe'
	 * link provided by Synapse via email.
	 * 
	 * @param notificationSettingsSignedToken
	 * 	
	 * @return A success message, if successful.
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.NOTIFICATION_SETTINGS, method = RequestMethod.PUT)
	public @ResponseBody
	ResponseMessage updateNotification(
			@RequestBody NotificationSettingsSignedToken notificationSettingsSignedToken)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		return serviceProvider.getUserProfileService().updateNotificationSettings(notificationSettingsSignedToken);
	}
	
	/**
	 * Batch get UserGroupHeaders.
	 * This fetches information about a collection of users or groups, specified by Synapse IDs.
	 * 
	 * @param ids IDs are specified as request parameters at the end of the URL, separated by commas.  <p>For example: <pre class="prettyprint">ids=1001,819</pre></p>
	 */
	@RequiredScope({view})
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
		return serviceProvider.getUserProfileService().getUserGroupHeadersByIds(longList);
	}

	/**
	 * Batch get UserGroupHeaders.
	 * This fetches information about a collection of users or groups, specified by Synapse IDs.
	 *
	 * @param ids IDs are specified as request parameters at the end of the URL, separated by commas. <p>For example: <pre class="prettyprint">ids=1001,819</pre></p>
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.POST)
	public @ResponseBody
	ListWrapper<UserProfile> listUserProfiles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody IdList ids) throws DatastoreException, NotFoundException {
		return serviceProvider.getUserProfileService().listUserProfiles(userId, ids);
	}

	/**
	 * Get Users and Groups that match the given prefix.
	 * 
	 * @param prefixFilter
	 *            The name to search for.
	 * @param typeFilter
	 *            Restrict the results to a type of principal. 
	 *            Available options: <a href="${org.sagebionetworks.repo.model.principal.TypeFilter}">TypeFilter</a>.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 0 is the first item.
	 *            <p>
	 *            <i>Default is 0</i>
	 *            </p>
	 * @param limit
	 *            Limits the number of items that will be fetched for this page.
	 *            <p>
	 *            <i>Default is 10</i>
	 *            </p>
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_GROUP_HEADERS, method = RequestMethod.GET)
	public @ResponseBody
	UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(
			@RequestParam(value = UrlHelpers.PREFIX_FILTER, required = false) String prefixFilter,
			@RequestParam(required = false) TypeFilter typeFilter,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit) throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getUserProfileService().getUserGroupHeadersByPrefix(prefixFilter, typeFilter, offset, limit);
	}
	
	/**
	 * Get Users and Groups that match the given list of aliases.
	 * 
	 * @param request
	 *            The list of principal aliases to lookup. Each alias must be
	 *            either a user name or team name. The maximum number of aliases per request is 100.
	 * @return The list UserGroupHeaders that match the requested Aliases. The order
	 *         of the request is preserved in the response. If a requested alias
	 *         does not match an existing user name or team name then no header
	 *         will be returned.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_GROUP_HEADERS_BY_ALIASES, method = RequestMethod.POST)
	public @ResponseBody
	UserGroupHeaderResponse getUserGroupHeadersByAliases(
			@RequestBody AliasList request) {
		return serviceProvider.getUserProfileService()
				.getUserGroupHeadersByAlias(request);
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
	@RequiredScope({download})
	@RequestMapping(value = UrlHelpers.USER_PROFILE_IMAGE, method = RequestMethod.GET)
	public @ResponseBody
	void imageRedirectURLForUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String profileId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getUserProfileService().getUserProfileImage(userId, profileId);
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
	@RequiredScope({download})
	@RequestMapping(value = UrlHelpers.USER_PROFILE_IMAGE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void imagePreviewRedirectURLForUser(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String profileId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getUserProfileService().getUserProfileImagePreview(userId, profileId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	
	/**
	 * Add an <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a> as a <a href="${org.sagebionetworks.repo.model.Favorite}">Favorite</a> of the caller.
	 * @param id
	 *        Entity ID of the favorite <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a>
	 */
	@RequiredScope({view,modify})
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
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 
			UrlHelpers.FAVORITE_ID
			}, method = RequestMethod.DELETE)
	public void removeFavorite(
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
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.FAVORITE
	}, method = RequestMethod.GET) 
	public @ResponseBody
	PaginatedResults<EntityHeader> getFavorites(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit) 
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getFavorites(userId, limit, offset);
	}

	public static ProjectListType getProjectListTypeForProjectListType(ProjectListTypeDeprecated deprecatedType) {
		ProjectListType projectListFilter = null;
		switch (deprecatedType) {
		case MY_PROJECTS:
		case OTHER_USER_PROJECTS:
			projectListFilter=ProjectListType.ALL;
			break;
		case MY_CREATED_PROJECTS:
			projectListFilter=ProjectListType.CREATED;
			break;
		case MY_PARTICIPATED_PROJECTS:
			projectListFilter=ProjectListType.PARTICIPATED;
			break;
		case MY_TEAM_PROJECTS:
		case TEAM_PROJECTS:
			projectListFilter=ProjectListType.TEAM;
			break;
		default:
			throw new IllegalArgumentException("Unrecognized "+deprecatedType);
		}
		return projectListFilter;
	}

	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_DEPRECATED }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ProjectHeader> getOwnProjectsDeprecated(
			@PathVariable ProjectListTypeDeprecated deprecatedType,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Long limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		
		ProjectListType projectListFilter = getProjectListTypeForProjectListType(deprecatedType);		
		String nextPageToken = (new NextPageToken(limit, offset)).toToken();
		ProjectHeaderList list = serviceProvider.getUserProfileService().getProjects(userId, userId, null, projectListFilter, sortColumn, sortDirection, nextPageToken);
		PaginatedResults<ProjectHeader> result = new PaginatedResults<ProjectHeader>();
		result.setResults(list.getResults());
		result.setTotalNumberOfResults(result.calculateTotalWithLimitAndOffset(list.getResults().size(), limit, offset));
		return result;
	}
	
	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_TEAM_DEPRECATED }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ProjectHeader> getProjectsTeamDeprecated(
			@PathVariable Long teamId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Long limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ProjectListType projectListFilter = ProjectListType.TEAM;	
		String nextPageToken = (new NextPageToken(limit, offset)).toToken();
		ProjectHeaderList list = serviceProvider.getUserProfileService().getProjects(userId, userId, teamId, projectListFilter, sortColumn, sortDirection, nextPageToken);
		PaginatedResults<ProjectHeader> result = new PaginatedResults<ProjectHeader>();
		result.setResults(list.getResults());
		result.setTotalNumberOfResults(result.calculateTotalWithLimitAndOffset(list.getResults().size(), limit, offset));
		return result;
	}

	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_USER_DEPRECATED }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ProjectHeader> getProjectsUserDeprecated(
			@PathVariable ProjectListTypeDeprecated deprecatedType,
			@PathVariable Long principalId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Long offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Long limit)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		ProjectListType projectListFilter = getProjectListTypeForProjectListType(deprecatedType);		
		String nextPageToken = (new NextPageToken(limit, offset)).toToken();
		ProjectHeaderList list = serviceProvider.getUserProfileService().getProjects(userId, principalId, null, projectListFilter, sortColumn, sortDirection, nextPageToken);
		PaginatedResults<ProjectHeader> result = new PaginatedResults<ProjectHeader>();
		result.setResults(list.getResults());
		result.setTotalNumberOfResults(result.calculateTotalWithLimitAndOffset(list.getResults().size(), limit, offset));
		return result;
	}
	
	
	/**
	 * Get a paginated result that contains <a href="${org.sagebionetworks.repo.model.ProjectHeader}">project
	 * headers</a> and user activity (last access date) of the caller. The project list includes only
	 * those for which the user or a team they are on is explicitly granted access in the project's share
	 * settings.  Other projects, including those visible by virtue of being Public, are not included.  
	 * The list of projects is further filtered as follows:
	 * <br/>
	 * If <i>filter</i> is ALL (the default): the projects that the caller has READ access to by virtue of being 
	 * included in the project's share settings personally or via a team in which they are a member, as described above.
	 * <br/>
	 * If <i>filter</i> is CREATED: only projects that the caller has created.
	 * <br/>
	 * If <i>filter</i> is PARTICIPATED: only projects that the caller has <em>not</em> created.
	 * <br/>
	 * If <i>filter</i> is TEAM: the projects that the caller has READ access by virtue of being included in
	 * the project's share settings via the team given by 'teamId' or, if no team ID is specified, then by any team 
	 * which they are a member of.
	 * <br/>
	 * 
	 * @param userId The ID of the user making the request
	 * @param projectFilter The <a href="${org.sagebionetworks.repo.model.ProjectListType}">criterion</a> for including a project in the list (see above).
	 * @param teamId If the projectFilter is 'TEAM' then this is the ID of the team through which the returned projects are shared with the user.
	 * @param filter see above
	 * @param sortColumn The optional <a href="${org.sagebionetworks.repo.model.ProjectListSortColumn}">column</a> to sort on. 
	 * 			<i>Default sort by last activity</i>
	 * @param sortDirection The optional <a href="${org.sagebionetworks.repo.model.entity.query.SortDirection}">sort direction</a>. 
	 * 			<i>Default sort descending</i>
	 * @param nextPageToken a token returned with the previous page of results
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS }, method = RequestMethod.GET)
	public @ResponseBody
	ProjectHeaderList getProjects(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.TEAM_ID_PARAM, required = false) Long teamId,
			@RequestParam(value = AuthorizationConstants.PROJECT_FILTER_PARAM, required = false) ProjectListType filter,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM, required=false) String nextPageToken)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, userId, teamId, filter, sortColumn, sortDirection, nextPageToken);
	}

	/**
	 * Get a paginated result that contains <a href="${org.sagebionetworks.repo.model.ProjectHeader}">project
	 * headers</a> and user activity (last access date) of the user specified by 'principalId'. The project list includes only
	 * those for which the user or a team they are on is explicitly granted access in the project's share
	 * settings.  Other projects, including those visible by virtue of being Public, are not included.  
	 * The list of projects is further filtered as follows:
	 * <br/>
	 * If <i>filter</i> is ALL (the default): the projects that the user has READ access to by virtue of being 
	 * included in the project's share settings personally or via a team in which they are a member, as described above.
	 * <br/>
	 * If <i>filter</i> is CREATED: only projects that the user has created.
	 * <br/>
	 * If <i>filter</i> is PARTICIPATED: only projects that the user has <em>not</em> created.
	 * <br/>
	 * If <i>filter</i> is TEAM: the projects that the user has READ access by virtue of being included in
	 * the project's share settings via the team given by 'teamId' or, if no team ID is specified, then by any team 
	 * which they are a member of.
	 * <br/>
	 * 
	 * @param userId The ID of the user making the request
	 * @param principalId The ID of the user to list projects for
	 * @param projectFilter The <a href="${org.sagebionetworks.repo.model.ProjectListType}">criterion</a> for including a project in the list (see above).
	 * @param teamId If the projectFilter is 'TEAM' then this is the ID of the team through which the returned projects are shared with 'principalId'.
	 * @param filter see above
	 * @param sortColumn The optional <a href="${org.sagebionetworks.repo.model.ProjectListSortColumn}">column</a> to sort on. 
	 * 			<i>Default sort by last activity</i>
	 * @param sortDirection The optional <a href="${org.sagebionetworks.repo.model.entity.query.SortDirection}">sort direction</a>. 
	 * 			<i>Default sort descending</i>
	 * @param nextPageToken a token returned with the previous page of results
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECTS_USER }, method = RequestMethod.GET)
	public @ResponseBody
	ProjectHeaderList getOtherUsersProjects(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long principalId,
			@RequestParam(value = AuthorizationConstants.TEAM_ID_PARAM, required = false) Long teamId,
			@RequestParam(value = AuthorizationConstants.PROJECT_FILTER_PARAM, required = false) ProjectListType filter,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_PARAM, required = false) ProjectListSortColumn sortColumn,
			@RequestParam(value = UrlHelpers.PROJECTS_SORT_DIRECTION_PARAM, required = false) SortDirection sortDirection,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM, required=false) String nextPageToken)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getProjects(userId, principalId, teamId, filter, sortColumn, sortDirection, nextPageToken);
	}

}
