/**
 * 
 */
package org.sagebionetworks.bridge.controller;

import java.io.IOException;

import org.sagebionetworks.bridge.BridgeUrlHelpers;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.service.BridgeServiceProvider;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.web.*;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 
 */
// @ControllerInfo(displayName = "Community Services", path = BridgeUrlHelpers.BASE_V1)
@Controller
public class CommunityController extends BridgeBaseController {

	@Autowired
	BridgeServiceProvider serviceProvider;

	/**
	 * 
	 * Create a new Community. The passed request body may contain the following fields:
	 * <ul>
	 * <li>name - Give your new Community a name. The name must be unique, not used by an existing Community (required).
	 * </li>
	 * <li>description - a short text description of the Community's purpose (optional).</li>
	 * </ul>
	 * <p>
	 * 
	 * @param userId
	 * @param community
	 * @return
	 * @throws NotFoundException
	 * @throws ACLInheritanceException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws NameConflictException
	 * @throws IOException
	 * @throws ServiceUnavailableException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = BridgeUrlHelpers.COMMUNITY, method = RequestMethod.POST)
	public @ResponseBody
	Community createCommunity(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody Community community) throws NotFoundException, NameConflictException, DatastoreException, InvalidModelException,
			UnauthorizedException, ACLInheritanceException, ServiceUnavailableException, IOException {
		return serviceProvider.getCommunityService().create(userId, community);
	}

	/**
	 * Retrieve the metadata for a specified community.
	 * 
	 * @param id the ID of the Community of interest
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.COMMUNITY_ID, method = RequestMethod.GET)
	public @ResponseBody
	Community getCommunity(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id) throws NotFoundException {
		return serviceProvider.getCommunityService().get(userId, id);
	}

	/**
	 * Update the Community metadata for the specified Community. Note: The client must be a Community administrator to
	 * make this request.
	 * 
	 * @param userId
	 * @param community the new metadata for the Community
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.COMMUNITY_ID, method = RequestMethod.PUT)
	public @ResponseBody
	Community updateCommunity(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody Community community) throws NotFoundException {
		return serviceProvider.getCommunityService().update(userId, community);
	}

	/**
	 * Delete the Community. Note: The client must be a Community administrator to make this request.
	 * 
	 * @param id the ID of the Community to delete.
	 * @param userId
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = BridgeUrlHelpers.COMMUNITY_ID, method = RequestMethod.DELETE)
	public void deleteCommunity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws NotFoundException {
		serviceProvider.getCommunityService().delete(userId, id);
	}

	/**
	 * Retrieve a paginated list of all Communities.
	 * 
	 * @param limit the maximum number of Communities to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.COMMUNITY, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Community> getCommunities(
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getCommunityService().getAll(userId, limit, offset);
	}

	/**
	 * Retrieve a paginated list of Communities to which the given user belongs.
	 * 
	 * @param id the principal ID of the user of interest.
	 * @param limit the maximum number of Communities to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.USER_COMMUNITIES, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Community> getCommunitiesByMember(
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getCommunityService().getForMember(userId, limit, offset);
	}

	/**
	 * Retrieve a paginated list of Communities to which the given user belongs.
	 * 
	 * @param id the principal ID of the user of interest.
	 * @param limit the maximum number of Communities to return (default 10)
	 * @param offset the starting index of the returned results (default 0)
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.COMMUNITY_MEMBERS, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<UserGroupHeader> getCommunityMembers(
			@PathVariable String id,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getCommunityService().getMembers(userId, id, limit, offset);
	}

	/**
	 * Join a Communities
	 * 
	 * @param id the ID of the Community to join
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.JOIN_COMMUNITY, method = RequestMethod.GET)
	public void joinCommunity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws DatastoreException,
			NotFoundException {
		serviceProvider.getCommunityService().joinCommunity(userId, id);
	}

	/**
	 * Join a Communities
	 * 
	 * @param id the ID of the Community to join
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.LEAVE_COMMUNITY, method = RequestMethod.GET)
	public void leaveCommunity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws DatastoreException,
			NotFoundException {
		serviceProvider.getCommunityService().leaveCommunity(userId, id);
	}
}
