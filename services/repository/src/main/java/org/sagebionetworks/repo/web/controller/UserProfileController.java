package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONException;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.authutil.User;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Favorite;
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

@ControllerInfo(displayName="User Profile Services", path="repo/v1")
@Controller
public class UserProfileController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Get a user profile
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
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
	 * Get a user profile specifying the individual group id for the user of interest
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
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
	 * Get all the UserProfiles in the system (paginated
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserProfiles 
	 * @throws DatastoreException - Thrown when there is a server-side problem.
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
	 * Get a user profile
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The updated UserProfile
	 * @throws DatastoreException - Thrown when there is a server-side problem.
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
	 * Create a security token for use for a particular with a particular
	 * locationable user profile picture to be stored in AWS S3
	 * 
	 * @param userId
	 * @param id
	 * @param etag
	 * @param s3Token
	 * @param request
	 * @return a filled-in S3Token
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
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
	 * Create a token used to upload an attachment.
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
	 * Batch get headers for users/groups matching a list of Synapse IDs.
	 * 
	 * @param header
	 * @param ids
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
	 * Get the users and groups whose names have a given prefix.
	 * 
	 * @param prefixFilter
	 * @param offset
	 * @param limit
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
