package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.PaginatedParameters;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
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
 * REST controller for CRUD operations on any object
 * 
 * @author jhill
 */
@Controller
public class DefaultController extends BaseController {

	@Autowired
	GenericEntityController entityController;
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	/**
	 * Create a new entity with a POST.
	 * @param <T>
	 * @param userId - The user that is doing the create.
	 * @param header - Used to get content type information.
	 * @param request - The body is extracted from the request.
	 * @return The new entity with an etag, id, and type specific metadata.
	 * @throws DatastoreException - Thrown when an there is a server failure.
	 * @throws InvalidModelException - Thrown if the passed object does not match the expected entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException - Thrown only for the case where the entity is assigned a parent that does not exist.
	 * @throws IOException - Thrown if there is a failure to read the header.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { 
			UrlHelpers.DATASET,
			UrlHelpers.LAYER,
			UrlHelpers.PREVIEW,
			UrlHelpers.LOCATION,
			UrlHelpers.PROJECT
			}, method = RequestMethod.POST)
	public @ResponseBody
	<T extends Nodeable> T createEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException {

		// Determine the object type from the url.
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());
		@SuppressWarnings("unchecked")
		T entity = (T) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		// Now create the entity
		T createdEntity = (T) entityController.createEntity(userId, entity, request);
		// Finally, add the type specific metadata.
		return createdEntity;
	}
	
	
	
	/**
	 * Get an existing entity with a GET.
	 * @param <T>
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
			UrlHelpers.DATASET_ID,
			UrlHelpers.LAYER_ID,
			UrlHelpers.PREVIEW_ID,
			UrlHelpers.LOCATION_ID,
			UrlHelpers.PROJECT_ID
			}, method = RequestMethod.GET)
	public @ResponseBody
	<T extends Nodeable> T getEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the object type
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());
		// Get the entity.
		@SuppressWarnings("unchecked")
		T updatedEntity = (T) entityController.getEntity(userId, id, request, type.getClassForType());
		// Return the results
		return updatedEntity;
	}

	/**
	 * Update an entity.
	 * @param <T>
	 * @param userId - The user that is doing the update.
	 * @param header - Used to get content type information.
	 * @param id - The id of the entity to update.
	 * @param etag - A valid etag must be provided for every update call.
	 * @param request - Used to read the contents.
	 * @return
	 * @throws NotFoundException - Thrown if the given entity does not exist.
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not match the current etag of an entity.
	 * This will occur when an entity gets updated after getting the current etag.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws InvalidModelException - Thrown if the passed entity contents doe not match the expected schema.
	 * @throws UnauthorizedException
	 * @throws IOException - There is a problem reading the contents.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.DATASET_ID,
			UrlHelpers.LAYER_ID,
			UrlHelpers.PREVIEW_ID,
			UrlHelpers.LOCATION_ID,
			UrlHelpers.PROJECT_ID
	}, method = RequestMethod.PUT)
	public @ResponseBody
	<T extends Nodeable> T updateEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		// Determine the object type from the url.
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());
		@SuppressWarnings("unchecked")
		T entity = (T) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		// validate the entity
		entity = entityController.updateEntity(userId, entity, request);
		// Return the result
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
			UrlHelpers.DATASET_ANNOTATIONS,
			UrlHelpers.LAYER_ANNOTATIONS,
			UrlHelpers.PREVIEW_ANNOTATIONS,
			UrlHelpers.LOCATION_ANNOTATIONS,
			UrlHelpers.PROJECT_ANNOTATIONS
			}, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotations(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
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
	 * @return
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not match the current etag of an entity.
	 * This will occur when an entity gets updated after getting the current etag.
	 * @throws NotFoundException - Thrown if the given entity does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws InvalidModelException - Thrown if the passed entity contents doe not match the expected schema.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.DATASET_ANNOTATIONS,
			UrlHelpers.LAYER_ANNOTATIONS,
			UrlHelpers.PREVIEW_ANNOTATIONS,
			UrlHelpers.LOCATION_ANNOTATIONS,
			UrlHelpers.PROJECT_ANNOTATIONS
	}, method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateEntityAnnotations(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		// Pass it along
		return entityController.updateEntityAnnotations(userId, id, updatedAnnotations, request);
	}

	/**
	 * Called to delete an entity. 
	 * @param userId - The user that is deleting the entity.
	 * @param id - The id of the user that is deleting the entity.
	 * @throws NotFoundException - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 			
			UrlHelpers.DATASET_ID,
			UrlHelpers.LAYER_ID,
			UrlHelpers.PREVIEW_ID,
			UrlHelpers.LOCATION_ID,
			UrlHelpers.PROJECT_ID 
			}, method = RequestMethod.DELETE)
	public void deleteEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());
		entityController.deleteEntity(userId, id, type.getClassForType());
		return;
	}

	/**
	 * Fetch all of the entities of a given type in a paginated form.
	 * @param <T>
	 * @param userId - The id of the user doing the fetch.
	 * @param offset - The offset index determines where this page will start from.  An index of 1 is the first entity. When null it will default to 1.
	 * @param limit - Limits the number of entities that will be fetched for this page. When null it will default to 10.
	 * @param sort - Determines the order that the entities will be returned. When null, the entities will be sorted by their natural order (i.e. by ID).
	 * @param ascending - Should the sort be ascending (true) or descending (false).
	 * @param request
	 * @return A paginated list of results.
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.DATASET,
			UrlHelpers.LAYER,
			UrlHelpers.PREVIEW,
			UrlHelpers.LOCATION,
			UrlHelpers.PROJECT
		}, method = RequestMethod.GET)
	public @ResponseBody
	<T extends Nodeable> PaginatedResults<T> getEntities(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
			HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException {

		// Null is used for the default.
		if(ServiceConstants.DEFAULT_SORT_BY_PARAM.equals(sort)){
			sort = null;
		}
		// Determine the object type from the url.
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());
		@SuppressWarnings("unchecked")
		PaginatedResults<T> results = (PaginatedResults<T>) entityController.getEntities(
				userId, new PaginatedParameters(offset, limit, sort, ascending), request, type.getClassForType());
		// Return the result
		return results;
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.DATASET_CHILDREN,
			UrlHelpers.LAYER_CHILDREN,
			UrlHelpers.PREVIEW_CHILDREN,
			UrlHelpers.LOCATION_CHILDREN,
			UrlHelpers.PROJECT_CHILDREN
		}, method = RequestMethod.GET)
	public @ResponseBody
	<T extends BaseChild> PaginatedResults<T> getEntityChildren(
			@PathVariable String parentType,
			@PathVariable String parentId,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
			HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException {

		// Null is used for the default.
		if(ServiceConstants.DEFAULT_SORT_BY_PARAM.equals(sort)){
			sort = null;
		}
		PaginatedParameters paging = new PaginatedParameters(offset, limit, sort, ascending);
		// Determine the object type from the url.
		ObjectType type = ObjectType.getLastTypeInUrl(request.getRequestURI());
		Class<? extends T> clazz = (Class<? extends T>) type.getClassForType();
		PaginatedResults<T> results = (PaginatedResults<T>) entityController.getEntityChildrenOfTypePaginated(userId, parentId, clazz, paging, request);
		// Return the results
		return results;
	}	

	/**
	 * Get the schema for an entity type.
	 * @param request
	 * @return
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.DATASET_SCHEMA,
			UrlHelpers.LAYER_SCHEMA,
			UrlHelpers.PREVIEW_SCHEMA,
			UrlHelpers.LOCATION_SCHEMA,
			UrlHelpers.PROJECT_SCHEMA
	}, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntitiesSchema(HttpServletRequest request) throws DatastoreException {
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());
		return entityController.getEntitySchema(type.getClassForType());
	}
	
	/**
	 * Get the schema for Annotations
	 * 
	 * @return the schema
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ={UrlHelpers.ANNOTATIONS + UrlHelpers.SCHEMA}, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getAnnotationsSchema() throws DatastoreException {
		return entityController.getEntityAnnotationsSchema();
	}

	/**
	 * Create a new ACL, overriding inheritance.
	 * @param <T>
	 * @param userId - The user that is doing the create.
	 * @param request - The body is extracted from the request.
	 * @return The new ACL, which includes the id of the affected entity
	 * @throws DatastoreException - Thrown when an there is a server failure.
	 * @throws InvalidModelException - Thrown if the passed object does not match the expected entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException - Thrown only for the case where the entity is assigned a parent that does not exist.
	 * @throws IOException - Thrown if there is a failure to read the header.
	 */
	@ResponseStatus(HttpStatus.CREATED)
@RequestMapping(value = { UrlHelpers.OBJECT_TYPE_ID_ACL }, method = RequestMethod.POST)	
	public @ResponseBody
	AccessControlList createEntityAcl(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList newAcl,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException {
		// pass it along.
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());

		return entityController.createEntityACL(userId, newAcl, request, type.getClassForType());
	}
	
	

	/**
	 * Get the Access Control List (ACL) for a given entity.
	 * @param id - The ID of the entity to get the ACL for.
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The entity ACL.
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 * @throws NotFoundException - Thrown if the entity does not exist.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.OBJECT_TYPE_ID_ACL	}, method = RequestMethod.GET)
	public @ResponseBody
	AccessControlList getEntityAcl(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		// pass it along.
		ObjectType type = ObjectType.getFirstTypeInUrl(request.getRequestURI());
		return entityController.getEntityACL(id, userId, request, type.getClassForType());
	}
	
	/**
	 * Update an entity's ACL.
	 * @param id
	 * @param userId
	 * @param updatedACL
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.OBJECT_TYPE_ID_ACL	}, method = RequestMethod.PUT)
	public @ResponseBody
	AccessControlList updateEntityAcl(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList updatedACL,
			HttpServletRequest request) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException {
		// pass it along.
		return entityController.updateEntityACL(userId, updatedACL);
	}
	

	/**
	 * Called to restore inheritance (vs. defining ones own ACL)
	 * @param userId - The user that is deleting the entity.
	 * @param id - The entity whose inheritance is to be restored
	 * @throws NotFoundException - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 
			UrlHelpers.OBJECT_TYPE_ID_ACL
			}, method = RequestMethod.DELETE)
	public void deleteEntityACL(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		entityController.deleteEntityACL(userId, id);
	}
	
	/**
	 * Get the schema for an ACL
	 * @param id
	 * @param request
	 * @return
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ={UrlHelpers.ACL + UrlHelpers.SCHEMA}, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getAclSchema() throws DatastoreException {
		return entityController.getAclSchema();
	}
}
