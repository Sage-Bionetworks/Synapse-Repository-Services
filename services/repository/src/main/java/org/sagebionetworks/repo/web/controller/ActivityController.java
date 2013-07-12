package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
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
 * Provides generic access to all entities. The caller does not need to know the entity type for these methods.
 * New entity types should automatically be exposed with these methods.
 * 
 * @author John
 *
 */
@ControllerInfo(displayName="Activity Services", path="repo/v1")
@Controller
public class ActivityController extends BaseController{
	
	@Autowired
	ServiceProvider serviceProvider;
			
	/**
	 * Create a new activity with a POST.
	 * @param userId - The user that is doing the create.
	 * @param header - Used to get content type information.
	 * @param request - The body is extracted from the request.
	 * @return The new activity 
	 * @throws DatastoreException - Thrown when an there is a server failure.
	 * @throws InvalidModelException - Thrown if the passed object does not match the expected entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException - Thrown only for the case where the requesting user is not found.
	 * @throws IOException - Thrown if there is a failure to read the header.
	 * @throws JSONObjectAdapterException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.ACTIVITY
			}, method = RequestMethod.POST)
	public @ResponseBody
	Activity createActivity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody Activity activity,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {
		return serviceProvider.getActivityService().createActivity(userId, activity);
	}
	
	/**
	 * Get an existing activity with a GET.
	 * @param id - The ID of the activity to fetch.
	 * @param userId -The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException - Thrown if the requested activity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 * @throws UnauthorizedException - Thrown if specified user is unauthorized to access this activity.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ACTIVITY_ID
			}, method = RequestMethod.GET)
	public @ResponseBody
	Activity getActivity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getActivityService().getActivity(userId, id);
	}
	
	/**
	 * Update an activity.
	 * @param id - The id of the activity to update.
	 * @param userId - The user that is doing the update.
	 * @param header - Used to get content type information.
	 * @param etag - A valid etag must be provided for every update call.
	 * @param request - Used to read the contents.
	 * @return the updated activity
	 * @throws NotFoundException - Thrown if the given activity does not exist.
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not match the current etag of the activity.
	 * This will occur when an activity gets updated after getting the current etag.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws InvalidModelException - Thrown if the passed activity contents do not match the expected schema.
	 * @throws UnauthorizedException
	 * @throws IOException - There is a problem reading the contents.
	 * @throws JSONObjectAdapterException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ACTIVITY_ID
	}, method = RequestMethod.PUT)
	public @ResponseBody
	Activity updateActivity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody Activity activity,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException, JSONObjectAdapterException {
		return serviceProvider.getActivityService().updateActivity(userId, activity);
	}
	
	/**
	 * Called to delete an activity 
	 * @param id - The id of activity to delete.
	 * @param userId - The user that is deleting the activity.
	 * @param request 
	 * @throws NotFoundException - Thrown when the activity to delete does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException - Thrown when the user is not allowed to access or delete the specified activity.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 			
			UrlHelpers.ACTIVITY_ID
			}, method = RequestMethod.DELETE)
	public void deleteActivity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		serviceProvider.getActivityService().deleteActivity(userId, id);
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.ACTIVITY_GENERATED
	}, method = RequestMethod.GET) 
	public @ResponseBody
	PaginatedResults<Reference> getEntitiesGeneratedBy(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required=false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit) 
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getActivityService().getEntitiesGeneratedBy(userId, id, limit, offset);
	}

}
