package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.manager.SchemaCache;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.PaginatedParameters;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.ObjectSchema;
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
			UrlHelpers.PROJECT,
			UrlHelpers.FOLDER,
			UrlHelpers.EULA,
			UrlHelpers.AGREEMENT,
			UrlHelpers.ANALYSIS,
			UrlHelpers.STEP,
			UrlHelpers.CODE
			}, method = RequestMethod.POST)
	public @ResponseBody
	Entity createEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException {

		// Determine the object type from the url.
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		@SuppressWarnings("unchecked")
		Entity entity =  (Entity) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		// Now create the entity
		Entity createdEntity = entityController.createEntity(userId, entity, request);
		// Finally, add the type specific metadata.
		return createdEntity;
	}
	
	/**
	 * @param userId
	 * @param header
	 * @param etag
	 * @param request
	 * @return the newly created versionable entity
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.CODE_VERSION,
			UrlHelpers.LAYER_VERSION,
			UrlHelpers.DATASET_VERSION,
			UrlHelpers.LOCATION_VERSION
			}, method = RequestMethod.PUT)
	public @ResponseBody
	Versionable createNewVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {

		// This is simply an update with a new version created.
		return (Versionable) updateEntityImpl(userId, header, etag, true, request);
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
			UrlHelpers.DATASET_ID,
			UrlHelpers.LAYER_ID,
			UrlHelpers.PREVIEW_ID,
			UrlHelpers.LOCATION_ID,
			UrlHelpers.PROJECT_ID,
			UrlHelpers.FOLDER_ID,
			UrlHelpers.EULA_ID,
			UrlHelpers.AGREEMENT_ID,
			UrlHelpers.ANALYSIS_ID,
			UrlHelpers.STEP_ID,
			UrlHelpers.CODE_ID
			}, method = RequestMethod.GET)
	public @ResponseBody
	Entity getEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the object type
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		// Get the entity.
		@SuppressWarnings("unchecked")
		Entity updatedEntity = entityController.getEntity(userId, id, request, type.getClassForType());
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
			UrlHelpers.CODE_VERSION_NUMBER,
			UrlHelpers.LAYER_VERSION_NUMBER,
			UrlHelpers.DATASET_VERSION_NUMBER,
			UrlHelpers.LOCATION_VERSION_NUMBER
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.DATASET_ID,
			UrlHelpers.LAYER_ID,
			UrlHelpers.PREVIEW_ID,
			UrlHelpers.LOCATION_ID,
			UrlHelpers.PROJECT_ID,
			UrlHelpers.FOLDER_ID,
			UrlHelpers.EULA_ID,
			UrlHelpers.AGREEMENT_ID,
			UrlHelpers.ANALYSIS_ID,
			UrlHelpers.STEP_ID,
			UrlHelpers.CODE_ID
	}, method = RequestMethod.PUT)
	public @ResponseBody
	Entity updateEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		// Note that we auto-version for locationable entities whose md5 checksums have changed.
		return updateEntityImpl(userId, header, etag, false, request);
	}

	/**
	 * Does the actually entity update.
	 * @param userId
	 * @param header
	 * @param etag
	 * @param newVersion - Should a new version be created to do this update?
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 */
	private Entity updateEntityImpl(String userId, HttpHeaders header,
			String etag, boolean newVersion, HttpServletRequest request) throws IOException,
			NotFoundException, ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException {
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		@SuppressWarnings("unchecked")
		Entity entity = (Entity) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		// validate the entity
		entity = entityController.updateEntity(userId, entity, newVersion, request);
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
			UrlHelpers.PROJECT_ANNOTATIONS,
			UrlHelpers.FOLDER_ANNOTATIONS,
			UrlHelpers.EULA_ANNOTATIONS,
			UrlHelpers.AGREEMENT_ANNOTATIONS,
			UrlHelpers.ANALYSIS_ANNOTATIONS,
			UrlHelpers.STEP_ANNOTATIONS,
			UrlHelpers.CODE_ANNOTATIONS
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
			UrlHelpers.DATASET_PATH,
			UrlHelpers.LAYER_PATH,
			UrlHelpers.PREVIEW_PATH,
			UrlHelpers.LOCATION_PATH,
			UrlHelpers.PROJECT_PATH,
			UrlHelpers.FOLDER_PATH,
			UrlHelpers.EULA_PATH,
			UrlHelpers.AGREEMENT_PATH,
			UrlHelpers.ANALYSIS_PATH,
			UrlHelpers.STEP_PATH,
			UrlHelpers.CODE_PATH
			}, method = RequestMethod.GET)
	public @ResponseBody
	EntityPath getEntityPath(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Wrap it up and pass it along
		List<EntityHeader> paths = entityController.getEntityPath(userId, id);
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(paths);
		return entityPath;
	}
	
	/**
	 * Get the annotations for a given version of an entity.
	 * @param userId - The user that is doing the update.
	 * @param id - The id of the entity to update.
	 * @param versionNumber 
	 * @param request - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException - Thrown if the given entity does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.CODE_VERSION_ANNOTATIONS,
			UrlHelpers.LAYER_VERSION_ANNOTATIONS,
			UrlHelpers.DATASET_VERSION_ANNOTATIONS,
			UrlHelpers.LOCATION_VERSION_ANNOTATIONS
			}, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotationsForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Pass it along
		return entityController.getEntityAnnotationsForVersion(userId, id, versionNumber, request);
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
			UrlHelpers.DATASET_ANNOTATIONS,
			UrlHelpers.LAYER_ANNOTATIONS,
			UrlHelpers.PREVIEW_ANNOTATIONS,
			UrlHelpers.LOCATION_ANNOTATIONS,
			UrlHelpers.PROJECT_ANNOTATIONS,
			UrlHelpers.FOLDER_ANNOTATIONS,
			UrlHelpers.EULA_ANNOTATIONS,
			UrlHelpers.AGREEMENT_ANNOTATIONS,
			UrlHelpers.ANALYSIS_ANNOTATIONS,
			UrlHelpers.STEP_ANNOTATIONS,
			UrlHelpers.CODE_ANNOTATIONS
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
			UrlHelpers.DATASET_ID,
			UrlHelpers.LAYER_ID,
			UrlHelpers.PREVIEW_ID,
			UrlHelpers.LOCATION_ID,
			UrlHelpers.PROJECT_ID,
			UrlHelpers.FOLDER_ID,
			UrlHelpers.EULA_ID,
			UrlHelpers.AGREEMENT_ID,
			UrlHelpers.ANALYSIS_ID,
			UrlHelpers.STEP_ID,
			UrlHelpers.CODE_ID
			}, method = RequestMethod.DELETE)
	public void deleteEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Determine the object type from the url.
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		entityController.deleteEntity(userId, id, type.getClassForType());
		return;
	}

	/**
	 * Called to delete an entity. 
	 * @param userId - The user that is deleting the entity.
	 * @param id - The id of the user that is deleting the entity.
	 * @param versionNumber 
	 * @param request 
	 * @throws NotFoundException - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 	
			UrlHelpers.CODE_VERSION_NUMBER,
			UrlHelpers.LAYER_VERSION_NUMBER,
			UrlHelpers.DATASET_VERSION_NUMBER,
			UrlHelpers.LOCATION_VERSION_NUMBER
			}, method = RequestMethod.DELETE)
	public void deleteEntityVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		entityController.deleteEntityVersion(userId, id, versionNumber,type.getClassForType());
	}
	/**
	 * Fetch all of the entities of a given type in a paginated form.
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
			UrlHelpers.PROJECT,
			UrlHelpers.FOLDER,
			UrlHelpers.EULA,
			UrlHelpers.AGREEMENT,
			UrlHelpers.ANALYSIS,
			UrlHelpers.STEP,
			UrlHelpers.CODE
		}, method = RequestMethod.GET)
	public @ResponseBody
    PaginatedResults<Entity> getEntities(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
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
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		@SuppressWarnings("unchecked")
		PaginatedResults<Entity> results = (PaginatedResults<Entity>) entityController.getEntities(
				userId, new PaginatedParameters(offset, limit, sort, ascending), request, type.getClassForType());
		// Return the result
		return results;
	}
	
	/**
	 * Fetch all of the entities of a given type in a paginated form.
	 * @param id 
	 * @param userId - The id of the user doing the fetch.
	 * @param offset - The offset index determines where this page will start from.  An index of 1 is the first entity. When null it will default to 1.
	 * @param limit - Limits the number of entities that will be fetched for this page. When null it will default to 10.
	 * @param request
	 * @return A paginated list of results.
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.CODE_VERSION,
			UrlHelpers.LAYER_VERSION,
			UrlHelpers.DATASET_VERSION,
			UrlHelpers.LOCATION_VERSION
		}, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Versionable> getAllVersionsOfEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		
		if(limit == null){
			limit = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM_INT;
		}

		// Determine the object type from the url.
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		Class<? extends Versionable> clazz = (Class<? extends Versionable>) type.getClassForType();
		@SuppressWarnings("unchecked")
		PaginatedResults<Versionable> results = entityController.getAllVerionsOfEntity(userId, offset, limit, id, request, clazz);
		// Return the result
		return results;
	}
	
	/**
	 * @param parentType
	 * @param parentId
	 * @param userId
	 * @param offset
	 * @param limit
	 * @param sort
	 * @param ascending
	 * @param request
	 * @return paginated results
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.DATASET_CHILDREN,
			UrlHelpers.LAYER_CHILDREN,
			UrlHelpers.PREVIEW_CHILDREN,
			UrlHelpers.LOCATION_CHILDREN,
			UrlHelpers.PROJECT_CHILDREN,
			UrlHelpers.FOLDER_CHILDREN,
			UrlHelpers.EULA_CHILDREN,
			UrlHelpers.AGREEMENT_CHILDREN,
			UrlHelpers.ANALYSIS_CHILDREN,
			UrlHelpers.STEP_CHILDREN,
			UrlHelpers.CODE_CHILDREN
		}, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Entity> getEntityChildren(
			@PathVariable String parentType,
			@PathVariable String parentId,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
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
		if(limit == null){
			limit = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM_INT;
		}
		PaginatedParameters paging = new PaginatedParameters(offset, limit, sort, ascending);
		// Determine the object type from the url.
		EntityType type = EntityType.getLastTypeInUrl(request.getRequestURI());
		Class<? extends Entity> clazz = (Class<? extends Entity>) type.getClassForType();
		PaginatedResults<Entity> results = entityController.getEntityChildrenOfTypePaginated(userId, parentId, clazz, paging, request);
		// Return the results
		return results;
	}	

	/**
	 * Get the schema for an entity type.
	 * @param request
	 * @return the schema for the designated entity type
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {
			UrlHelpers.DATASET_SCHEMA,
			UrlHelpers.LAYER_SCHEMA,
			UrlHelpers.PREVIEW_SCHEMA,
			UrlHelpers.LOCATION_SCHEMA,
			UrlHelpers.PROJECT_SCHEMA,
			UrlHelpers.FOLDER_SCHEMA,
			UrlHelpers.EULA_SCHEMA,
			UrlHelpers.AGREEMENT_SCHEMA,
			UrlHelpers.ANALYSIS_SCHEMA,
			UrlHelpers.STEP_SCHEMA,
			UrlHelpers.CODE_SCHEMA,
			UrlHelpers.S3TOKEN_SCHEMA
	}, method = RequestMethod.GET)
	public @ResponseBody
	ObjectSchema getEntitiesSchema(HttpServletRequest request) throws DatastoreException {
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		ObjectSchema schema =  SchemaCache.getSchema(type.getClassForType());
		return schema;
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
	 * @param objectType 
	 * @param id 
	 * @param userId - The user that is doing the create.
	 * @param newAcl 
	 * @param request - The body is extracted from the request.
	 * @return The new ACL, which includes the id of the affected entity
	 * @throws DatastoreException - Thrown when an there is a server failure.
	 * @throws InvalidModelException - Thrown if the passed object does not match the expected entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException - Thrown only for the case where the entity is assigned a parent that does not exist.
	 * @throws IOException - Thrown if there is a failure to read the header.
	 * @throws ConflictingUpdateException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.OBJECT_TYPE_ID_ACL }, method = RequestMethod.POST)	
	public @ResponseBody
	AccessControlList createEntityAcl(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList newAcl,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {
		if(newAcl == null) throw new IllegalArgumentException("New ACL cannot be null");
		if(id == null) throw new IllegalArgumentException("ACL ID in the path cannot be null");
		// pass it along.
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		// This is a fix for PLFM-410
		newAcl.setId(id);
		AccessControlList acl = entityController.createEntityACL(userId, newAcl, request, type.getClassForType());
		return acl;
	}
	
	

	/**
	 * Get the Access Control List (ACL) for a given entity.
	 * @param objectType 
	 * @param id - The ID of the entity to get the ACL for.
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The entity ACL.
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 * @throws NotFoundException - Thrown if the entity does not exist.
	 * @throws UnauthorizedException 
	 * @throws ACLInheritanceException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.OBJECT_TYPE_ID_ACL	}, method = RequestMethod.GET)
	public @ResponseBody
	AccessControlList getEntityAcl(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException, ACLInheritanceException {
		// pass it along.
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		return entityController.getEntityACL(id, userId, request, type.getClassForType());
	}
	

	/**
	 * Get the Access Control List (ACL) for a given entity.
	 * @param objectType 
	 * @param id - The ID of the entity to get the ACL for.
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The entity ACL.
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 * @throws NotFoundException - Thrown if the entity does not exist.
	 * @throws UnauthorizedException 
	 * @throws ACLInheritanceException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.OBJECT_TYPE_ID_BENEFACTOR	}, method = RequestMethod.GET)
	public @ResponseBody
	EntityHeader getEntityBenefactor(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException, ACLInheritanceException {
		if(objectType == null) throw new IllegalArgumentException("PathVariable ObjectType cannot be null");
		if(id == null) throw new IllegalArgumentException("PathVariable ID cannot be null");
		// pass it along.
		EntityType type = EntityType.valueOf(objectType);
		return entityController.getEntityBenefactor(id, userId, request, type.getClassForType());
	}
	
	/**
	 * Update an entity's ACL.
	 * @param objectType 
	 * @param id
	 * @param userId
	 * @param updatedACL
	 * @param request
	 * @return the accessControlList
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.OBJECT_TYPE_ID_ACL	}, method = RequestMethod.PUT)
	public @ResponseBody
	AccessControlList updateEntityAcl(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList updatedACL,
			HttpServletRequest request) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		if(updatedACL == null) throw new IllegalArgumentException("ACL cannot be null");
		if(id == null) throw new IllegalArgumentException("ID cannot be null");
		// This is a fix for 
		if(!id.equals(updatedACL.getId())) throw new IllegalArgumentException("The path ID: "+id+" does not match the ACL's ID: "+updatedACL.getId());
		// This is a fix for PLFM-621
		updatedACL.setId(id);
		// pass it along.
		return entityController.updateEntityACL(userId, updatedACL, request);
	}
	

	/**
	 * Called to restore inheritance (vs. defining ones own ACL)
	 * @param objectType 
	 * @param userId - The user that is deleting the entity.
	 * @param id - The entity whose inheritance is to be restored
	 * @throws NotFoundException - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 
			UrlHelpers.OBJECT_TYPE_ID_ACL
			}, method = RequestMethod.DELETE)
	public void deleteEntityACL(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws NotFoundException,
			DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		entityController.deleteEntityACL(userId, id);
	}
	
	/**
	 * Get the schema for an ACL
	 * @return the ACL schema
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value ={UrlHelpers.ACL + UrlHelpers.SCHEMA}, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getAclSchema() throws DatastoreException {
		return entityController.getAclSchema();
	}
	
	/**
	 * @param objectType 
	 * @param id 
	 * @param userId 
	 * @param accessType 
	 * @param request 
	 * @return the access types that the given user has to the given resource
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value={UrlHelpers.OBJECT_TYPE+UrlHelpers.ID+UrlHelpers.ACCESS}, method=RequestMethod.GET)
	public @ResponseBody BooleanResult hasAccess(
			@PathVariable String objectType,
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false) String accessType,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		// pass it along.
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());
		return new BooleanResult(entityController.hasAccess(id, userId, request, type.getClassForType(), accessType));
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value={UrlHelpers.ENTITY+UrlHelpers.ID+UrlHelpers.PERMISSIONS}, method=RequestMethod.GET)
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
