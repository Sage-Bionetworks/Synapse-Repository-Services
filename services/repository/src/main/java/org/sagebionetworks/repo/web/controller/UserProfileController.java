package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONException;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
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
 * 
 *
 */
@ControllerInfo(displayName="User Profile Services", path="repo/v1")
@Controller
public class UserProfileController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Get the profile of the caller (my profile).
	 * <p><b>Note:</b> Private user profile fields will be returned.</p>
	 * @param userId
	 *             The user that is making the request.
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.GET)
	public @ResponseBody
	UserProfile getMyOwnUserProfile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
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
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE_ID, method = RequestMethod.GET)
	public @ResponseBody
	UserProfile getUserProfileByOwnerId(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String profileId,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException, NotFoundException {
		return serviceProvider.getUserProfileService().getUserProfileByOwnerId(userId, profileId);
	}

	/**
	 * Get all publicly available <a href="${org.sagebionetworks.repo.model.UserProfile}">UserProfile</a> data in the system
	 * @param request
	 * @param userId
	 * @param offset
	 *        The offset index determines where this page will start from. An index of 0 is the first item. <p><i>Default is 0</i></p>
	 * @param limit
	 *        Limits the number of items that will be fetched for this page. <p><i>Default is 100</i></p>
	 * @param sort
	 *        Used to indicate upon which field(s) to sort. <p><i>Default is NONE</i></p>
	 * @param ascending
	 *        Used to indicate whether the sort direction is ascending or not.  <p><i>Default is true</i></p>
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<UserProfile> getUserProfilesPaginated(HttpServletRequest request,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false)  String userId,
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
	 * @param header
	 * @param request
	 * @return The updated <a href="${org.sagebionetworks.repo.model.UserProfile}">UserProfile</a>
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException Thrown when there is a server-side problem.
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws IOException
	 * @throws AuthenticationException
	 * @throws XPathExpressionException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_PROFILE, method = RequestMethod.PUT)
	public @ResponseBody
	UserProfile updateUserProfile(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException, AuthenticationException, XPathExpressionException {
		return serviceProvider.getUserProfileService().updateUserProfile(userId, header, request);
	}

	/**
	 * Create a filled-in <a href="${org.sagebionetworks.repo.model.attachment.S3AttachmentToken}">S3AttachmentToken</a> for use with a particular
	 * locationable user profile picture to be stored in AWS S3.
	 * 
	 * @param userId
	 * @param id
	 * @param etag
	 * @param s3Token
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.USER_PROFILE_S3_ATTACHMENT_TOKEN }, method = RequestMethod.POST)
	public @ResponseBody
	S3AttachmentToken createUserProfileS3AttachmentToken(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String profileId, @RequestBody S3AttachmentToken token,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		return serviceProvider.getUserProfileService().createUserProfileS3AttachmentToken(userId, profileId, token, request);
	}
	/**
	 * Create a new PresignedUrl for a profile picture attachment.
	 * 
	 * @param userId
	 * @param id
	 * @param token
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.USER_PROFILE_ATTACHMENT_URL }, method = RequestMethod.POST)
	public @ResponseBody
	PresignedUrl getUserProfileAttachmentUrl(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String profileId,
			@RequestBody PresignedUrl url,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		return serviceProvider.getUserProfileService().getUserProfileAttachmentUrl(userId, profileId, url, request);
	}
	
	/**
	 * Batch get UserGroupHeaders.
	 * This fetches information about a collection of users or groups, specified by Synapse IDs.
	 * 
	 * @param header
	 * @param ids IDs are specified as request parameters at the end of the URL, separated by commas.  <p>For example: <pre class="prettyprint">ids=1001,819</pre></p>
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws JSONException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_GROUP_HEADERS_BATCH, method = RequestMethod.GET)
	public @ResponseBody
	UserGroupHeaderResponsePage getUserGroupHeadersByIds(
			@RequestHeader HttpHeaders header,
			@RequestParam(value = UrlHelpers.IDS_PATH_VARIABLE, required = true) String ids,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException {

		String[] idsArray = ids.split(",");
		List<String> idsList = new ArrayList<String>(Arrays.asList(idsArray));
		return serviceProvider.getUserProfileService().getUserGroupHeadersByIds(userId, idsList);
	}

	/**
	 * Get Users and Groups by name or email.
	 * 
	 * @param prefixFilter The name or email to search for.
	 * @param offset
	 *         The offset index determines where this page will start from. An index of 0 is the first item. <p><i>Default is 0</i></p> 
	 * @param limit
	 * 			Limits the number of items that will be fetched for this page. <p><i>Default is 10</i></p>
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
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
	 * Add an <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a> as a <a href="${org.sagebionetworks.repo.model.Favorite}">Favorite</a> of the caller.
	 * @param id
	 *        Entity ID of the favorite <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a>
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.FAVORITE_ID
			}, method = RequestMethod.POST)
	public @ResponseBody
	EntityHeader addFavorite(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {
		return serviceProvider.getUserProfileService().addFavorite(userId, id);
	}

	/**
	 * Remove an <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a> as a <a href="${org.sagebionetworks.repo.model.Favorite}">Favorite</a> of the caller.
	 * @param id
	 *       Entity ID of the <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a> that should be removed as a favorite
	 * @param userId
	 * @param request
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 
			UrlHelpers.FAVORITE_ID
			}, method = RequestMethod.DELETE)
	public @ResponseBody
	void removeFavorite(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {
		serviceProvider.getUserProfileService().removeFavorite(userId, id);
	}

	/**
	 * Get a paginated result that contains the 
	 * caller's <a href="${org.sagebionetworks.repo.model.Favorite}">Favorites</a> 
	 * @param userId
	 * @param offset
	 * 			The offset index determines where this page will start from. An index of 0 is the first item. <p><i>Default is 0</i></p>
	 * @param limit
	 *          Limits the number of items that will be fetched for this page. <p><i>Default is 10</i></p>
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.FAVORITE
	}, method = RequestMethod.GET) 
	public @ResponseBody
	PaginatedResults<EntityHeader> getFavorites(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required=false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit) 
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getUserProfileService().getFavorites(userId, limit, offset);
	}

}
