package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProviderFactory;
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
	@RequestMapping(value = { UrlHelpers.PROJECT, UrlHelpers.LOCATION }, method = RequestMethod.POST)
	public @ResponseBody
	<T extends Base> T createEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException {
		// Determine the object type from the url.
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		@SuppressWarnings("unchecked")
		T entity = (T) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		// Validate the entity before we create it
		provider.validateEntity(entity);
		// Now create the entity
		T createdEntity = (T) entityController.createEntity(userId, entity, request);
		// Finally, add the type specific metadata.
		provider.addTypeSpecificMetadata(createdEntity, request);
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
	@RequestMapping(value = { UrlHelpers.PROJECT_ID, UrlHelpers.LOCATION_ID }, method = RequestMethod.GET)
	public @ResponseBody
	<T extends Base> T getEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the object type
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		// Get the entity.
		@SuppressWarnings("unchecked")
		T updatedEntity = (T) entityController.getEntity(userId, id, request, type.getClassForType());
		// Add any type specific metadata.
		provider.addTypeSpecificMetadata(updatedEntity, request);
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
	@RequestMapping(value = { UrlHelpers.PROJECT_ID, UrlHelpers.LOCATION_ID }, method = RequestMethod.PUT)
	public @ResponseBody
	<T extends Base> T updateEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		// Validate the object type
		// Determine the object type from the url.
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		@SuppressWarnings("unchecked")
		T entity = (T) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		// validate the entity
		provider.validateEntity(entity);
		entity = entityController.updateEntity(userId, id,	entity, request);
		provider.addTypeSpecificMetadata(entity, request);
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
	@RequestMapping(value = { UrlHelpers.PROJECT_ANNOTATIONS, UrlHelpers.LOCATION_ANNOTATIONS }, method = RequestMethod.GET)
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
	@RequestMapping(value = { UrlHelpers.PROJECT_ANNOTATIONS, UrlHelpers.LOCATION_ANNOTATIONS }, method = RequestMethod.PUT)
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
	@RequestMapping(value = { UrlHelpers.PROJECT_ID, UrlHelpers.LOCATION_ID }, method = RequestMethod.DELETE)
	public void deleteEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		entityController.deleteEntity(userId, id);
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
	@RequestMapping(value = { UrlHelpers.PROJECT, UrlHelpers.LOCATION }, method = RequestMethod.GET)
	public @ResponseBody
	<T extends Base> PaginatedResults<T> getEntities(
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
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		// Fetch the provider that will validate this entity.
		TypeSpecificMetadataProvider<T> provider = TypeSpecificMetadataProviderFactory.getProvider(type);
		@SuppressWarnings("unchecked")
		PaginatedResults<T> results = (PaginatedResults<T>) entityController.getEntities(
				userId, offset, limit, sort, ascending, request, type.getClassForType());

		for (T entity : results.getResults()) {
			provider.addTypeSpecificMetadata(entity, request);
		}
		return results;
	}

	/**
	 * Get the schema for a given entity.
	 * @param id
	 * @param request
	 * @return
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ={ UrlHelpers.PROJECT_ID + UrlHelpers.SCHEMA, UrlHelpers.LOCATION_ID + UrlHelpers.SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntitySchema(@PathVariable String id, HttpServletRequest request) throws DatastoreException {
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		return entityController.getEntitySchema(type.getClassForType());
	}
	

	/**
	 * Get the schema for an entity type.
	 * @param request
	 * @return
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.PROJECT + UrlHelpers.SCHEMA, UrlHelpers.LOCATION + UrlHelpers.SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntitiesSchema(HttpServletRequest request) throws DatastoreException {
		ObjectType type = ObjectType.getTypeForUrl(request.getRequestURI());
		return entityController.getEntitiesSchema(type.getClassForType());
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
@RequestMapping(value = { 
			UrlHelpers.DATASET_ACL,
			UrlHelpers.LAYER_ACL,
			UrlHelpers.PROJECT_ACL,
			UrlHelpers.LOCATION_ACL,
			UrlHelpers.LOCATIONS_ACL
			}, method = RequestMethod.POST)	
	public @ResponseBody
	AccessControlList createEntityAcl(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList newAcl,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException {
		// pass it along.
		return entityController.createEntityACL(userId, newAcl, request);
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
	@RequestMapping(value = { 
			UrlHelpers.DATASET_ACL,
			UrlHelpers.LAYER_ACL,
			UrlHelpers.PROJECT_ACL,
			UrlHelpers.LOCATION_ACL,
			UrlHelpers.LOCATIONS_ACL
			}, method = RequestMethod.GET)
	public @ResponseBody
	AccessControlList getEntityAcl(@PathVariable String id,
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException {
		// pass it along.
		return entityController.getEntityACL(id, userId);
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
	@RequestMapping(value = { 
			UrlHelpers.DATASET_ACL,
			UrlHelpers.LAYER_ACL,
			UrlHelpers.PROJECT_ACL,
			UrlHelpers.LOCATION_ACL,
			UrlHelpers.LOCATIONS_ACL
			}, method = RequestMethod.PUT)
	public @ResponseBody
	AccessControlList updateEntityAcl(@PathVariable String id,
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
			UrlHelpers.DATASET_ACL,
			UrlHelpers.LAYER_ACL,
			UrlHelpers.PROJECT_ACL,
			UrlHelpers.LOCATION_ACL,
			UrlHelpers.LOCATIONS_ACL
			}, method = RequestMethod.DELETE)
	public void deleteEntityACL(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		entityController.deleteEntityACL(userId, id);
	}

	
}
