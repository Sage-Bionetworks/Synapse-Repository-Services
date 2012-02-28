package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
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
 * Provides generic access to all entities. The caller does not need to know the entity type for these methods.
 * New entity types should automatically be exposed with these methods.
 * 
 * @author John
 *
 */
@Controller
public class BasicEntityController extends BaseController{
	
	@Autowired
	GenericEntityController entityController;
	
	/**
	 * Get an existing entity with a GET.
	 * @param userId -The user that is doing the get.
	 * @param id - The ID of the entity to fetch.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_ID
			}, method = RequestMethod.GET)
	public @ResponseBody
	Entity getEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the entity.
		Entity entity =  entityController.getEntity(userId, id, request);
		return entity;
	}
	
	/**
	 * Get the annotations for an entity.
	 * @param userId - The user that is doing the update.
	 * @param id - The id of the entity to update.
	 * @param request - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException - Thrown if the given entity does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_ANNOTATIONS
			}, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotations(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Pass it along
		return entityController.getEntityAnnotations(userId, id, request);
	}
	
	
	/**
	 * Update an entities annotations.
	 * @param userId - The user that is doing the update.
	 * @param id - The id of the entity to update.
	 * @param etag - A valid etag must be provided for every update call.
	 * @param updatedAnnotations - The updated annotations
	 * @param request
	 * @return the updated annotations
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not match the current etag of an entity.
	 * This will occur when an entity gets updated after getting the current etag.
	 * @throws NotFoundException - Thrown if the given entity does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws InvalidModelException - Thrown if the passed entity contents doe not match the expected schema.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.ENTITY_ANNOTATIONS
	}, method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateEntityAnnotations(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		// Pass it along
		return entityController.updateEntityAnnotations(userId, id, updatedAnnotations, request);
	}
	
	/**
	 * Create a new entity with a POST.
	 * @param userId - The user that is doing the create.
	 * @param header - Used to get content type information.
	 * @param request - The body is extracted from the request.
	 * @return The new entity with an etag, id, and type specific metadata.
	 * @throws DatastoreException - Thrown when an there is a server failure.
	 * @throws InvalidModelException - Thrown if the passed object does not match the expected entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException - Thrown only for the case where the entity is assigned a parent that does not exist.
	 * @throws IOException - Thrown if there is a failure to read the header.
	 * @throws JSONObjectAdapterException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY
			}, method = RequestMethod.POST)
	public @ResponseBody
	Entity createEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, JSONObjectAdapterException {

		// Read the entity from the body
		Entity entity =  JSONEntityHttpMessageConverter.readEntity(request.getReader());
		// Now create the entity
		Entity createdEntity = entityController.createEntity(userId, entity, request);
		// Finally, add the type specific metadata.
		return createdEntity;
	}
	
	/**
	 * Update an entity.
	 * @param userId - The user that is doing the update.
	 * @param header - Used to get content type information.
	 * @param id - The id of the entity to update.
	 * @param etag - A valid etag must be provided for every update call.
	 * @param request - Used to read the contents.
	 * @return the updated entity
	 * @throws NotFoundException - Thrown if the given entity does not exist.
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not match the current etag of an entity.
	 * This will occur when an entity gets updated after getting the current etag.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws InvalidModelException - Thrown if the passed entity contents doe not match the expected schema.
	 * @throws UnauthorizedException
	 * @throws IOException - There is a problem reading the contents.
	 * @throws JSONObjectAdapterException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_ID
	}, method = RequestMethod.PUT)
	public @ResponseBody
	Entity updateEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException, JSONObjectAdapterException {
		// Note that we auto-version for locationable entities whose md5 checksums have changed.
		// Read the entity from the body
		Entity entity =  JSONEntityHttpMessageConverter.readEntity(request.getReader());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		// validate the entity
		entity = entityController.updateEntity(userId, entity, false, request);
		// Return the result
		return entity;
	}
	
	/**
	 * Called to delete an entity. 
	 * @param userId - The user that is deleting the entity.
	 * @param id - The id of the user that is deleting the entity.
	 * @param request 
	 * @throws NotFoundException - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 			
			UrlHelpers.ENTITY_ID
			}, method = RequestMethod.DELETE)
	public void deleteEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Pass it along
		entityController.deleteEntity(userId, id);
	}
	
	/**
	 * Get an existing entity with a GET.
	 * @param userId -The user that is doing the get.
	 * @param id - The ID of the entity to fetch.
	 * @param versionNumber 
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_VERSION_NUMBER
			}, method = RequestMethod.GET)
	public @ResponseBody
	Entity getEntityForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the object type
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		// Get the entity.
		@SuppressWarnings("unchecked")
		Entity updatedEntity = entityController.getEntityForVersion(userId, id, versionNumber, request, type.getClassForType());
		// Return the results
		return updatedEntity;
	}
	
	/**
	 * Get an existing entity with a GET.
	 * @param userId -The user that is doing the get.
	 * @param id - The ID of the entity to fetch.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_ID_TYPE
			}, method = RequestMethod.GET)
	public @ResponseBody
	EntityHeader getEntityType(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the type of an entity by ID.
		return entityController.getEntityHeader(userId, id);
	}

	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value={UrlHelpers.ENTITY_ID+UrlHelpers.PERMISSIONS}, method=RequestMethod.GET)
	public @ResponseBody UserEntityPermissions getUserEntityPermissions(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		// pass it along.
		return entityController.getUserEntityPermissions(userId, id);
	}
	/**
	 * Get the headers for entities having references to an existing entity.
	 * @param userId -The user that is doing the get.
	 * @param id - The target entity's ID.
	 * @param request
	 * @return The headers of the entities having references to the given entity
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_ID+UrlHelpers.REFERENCED_BY
			}, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<EntityHeader> getEntityReferences(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		// Get the type of an entity by ID.
		return entityController.getEntityReferences(userId, id, null, offset, limit, request);
	}
	
	
	/**
	 * Get the headers for entities having references to an existing entity.
	 * @param userId -The user that is doing the get.
	 * @param id - The target entity's ID.
	 * @param request
	 * @return The headers of the entities having references to the given entity
	 * @throws NotFoundException - Thrown if the requested entity does not exist.
	 * @throws DatastoreException - Thrown when an there is a server failure. 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_ID+UrlHelpers.VERSION+UrlHelpers.VERSION_NUMBER+UrlHelpers.REFERENCED_BY
			}, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<EntityHeader> getEntityReferences(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@PathVariable String id, 
			@PathVariable int versionNumber, 
			HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		// Get the type of an entity by ID.
		return entityController.getEntityReferences(userId, id, versionNumber, offset, limit, request);
	}

}
