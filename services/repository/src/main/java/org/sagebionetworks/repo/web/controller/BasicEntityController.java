package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.ServiceConstants;
import org.sagebionetworks.repo.manager.SchemaManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.PaginatedParameters;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
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
	
	@Autowired
	SchemaManager schemaManager;

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
			UrlHelpers.ENTITY_VERSION
			}, method = RequestMethod.PUT)
	public @ResponseBody
	Versionable createNewVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			@RequestHeader(ServiceConstants.ETAG_HEADER) String etag,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException, JSONObjectAdapterException {

		// This is simply an update with a new version created.
		return (Versionable) updateEntityImpl(userId, header, etag, true, request);
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
			InvalidModelException, UnauthorizedException, JSONObjectAdapterException {
		@SuppressWarnings("unchecked")
//		Entity entity = (Entity) objectTypeSerializer.deserialize(request.getInputStream(), header, type.getClassForType(), header.getContentType());
		Entity entity =  JSONEntityHttpMessageConverter.readEntity(request.getReader());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		// validate the entity
		entity = entityController.updateEntity(userId, entity, newVersion, request);
		// Return the result
		return entity;
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
			UrlHelpers.ENTITY_VERSION_NUMBER
			}, method = RequestMethod.DELETE)
	public void deleteEntityVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		entityController.deleteEntityVersion(userId, id, versionNumber);
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
		// Get the entity.
		@SuppressWarnings("unchecked")
		Entity updatedEntity = entityController.getEntityForVersion(userId, id, versionNumber, request);
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
     * Get the EntityHeader for an existing entity with a GET.  If any item in the batch fails (e.g., with a 404) they all fail.
     * 
     * @param userId
     *            -The user that is doing the get.
     * @param batch
     *            - The comma-separated list of IDs of the entity to fetch.
     * @param request
     * @return The requested Entity if it exists.
     * @throws NotFoundException
     *             - Thrown if the requested entity does not exist.
     * @throws DatastoreException
     *             - Thrown when an there is a server failure.
     * @throws UnauthorizedException
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = { UrlHelpers.ENTITY_TYPE }, method = RequestMethod.GET)
    public @ResponseBody
    BatchResults<EntityHeader> getEntityTypeBatch(
                    @RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
                    @RequestParam(value = ServiceConstants.BATCH_PARAM, required = true) String batch,
                    HttpServletRequest request) throws NotFoundException,
                    DatastoreException, UnauthorizedException {

            String ids[] = batch.split(",");

            List<EntityHeader> entityHeaders = new ArrayList<EntityHeader>();
            for (String id : ids) {
                    // Get the type of an entity by ID.
                    EntityHeader entityHeader = entityController.getEntityHeader(userId, id);
                    entityHeaders.add(entityHeader);
            }

            BatchResults<EntityHeader> results = new BatchResults<EntityHeader>();
            results.setResults(entityHeaders);
            results.setTotalNumberOfResults(entityHeaders.size());
            return results;
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
	
	/**
	 * Get the path of an entity.
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
			UrlHelpers.ENTITY_PATH
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
	 * Create a new ACL, overriding inheritance.
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
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.POST)	
	public @ResponseBody
	AccessControlList createEntityAcl(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList newAcl,
			HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {
		if(newAcl == null) throw new IllegalArgumentException("New ACL cannot be null");
		if(id == null) throw new IllegalArgumentException("ACL ID in the path cannot be null");
		// pass it along.
		// This is a fix for PLFM-410
		newAcl.setId(id);
		AccessControlList acl = entityController.createEntityACL(userId, newAcl, request);
		return acl;
	}
	
	/**
	 * Get the Access Control List (ACL) for a given entity.
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
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL	}, method = RequestMethod.GET)
	public @ResponseBody
	AccessControlList getEntityAcl(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException, ACLInheritanceException {
		// pass it along.
		return entityController.getEntityACL(id, userId, request);
	}
	
	/**
	 * Update an entity's ACL.
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
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL	}, method = RequestMethod.PUT)
	public @ResponseBody
	AccessControlList updateEntityAcl(
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
	 * @param userId - The user that is deleting the entity.
	 * @param id - The entity whose inheritance is to be restored
	 * @throws NotFoundException - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY_ID_ACL
			}, method = RequestMethod.DELETE)
	public void deleteEntityACL(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId) throws NotFoundException,
			DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		entityController.deleteEntityACL(userId, id);
	}
	
	/**
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
	@RequestMapping(value={UrlHelpers.ENTITY_ID+UrlHelpers.ACCESS}, method=RequestMethod.GET)
	public @ResponseBody BooleanResult hasAccess(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false) String accessType,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		// pass it along.
		return new BooleanResult(entityController.hasAccess(id, userId, request, accessType));
	}

	/**
	 * Get the Access Control List (ACL) for a given entity.
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
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ID_BENEFACTOR	}, method = RequestMethod.GET)
	public @ResponseBody
	EntityHeader getEntityBenefactor(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException, ACLInheritanceException {
		if(id == null) throw new IllegalArgumentException("PathVariable ID cannot be null");
		// pass it along.
		return entityController.getEntityBenefactor(id, userId, request);
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
			UrlHelpers.ENTITY_VERSION
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
		@SuppressWarnings("unchecked")
		PaginatedResults<Versionable> results = entityController.getAllVerionsOfEntity(userId, offset, limit, id, request);
		// Return the result
		return results;
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
			UrlHelpers.ENTITY_VERSION_ANNOTATIONS
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.REST_RESOURCES
			}, method = RequestMethod.GET)
	public @ResponseBody
	RestResourceList getRESTResources(HttpServletRequest request) {
		// Pass it along
		return schemaManager.getRESTResources();
	}
	
	/**

	 * 
	 * @param resourceId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.REST_RESOURCES+UrlHelpers.EFFECTIVE_SCHEMA
			}, method = RequestMethod.GET)
	public @ResponseBody
	ObjectSchema getEffectiveSchema(@RequestParam(value = UrlHelpers.RESOURCE_ID, required = true) String resourceId) throws NotFoundException, DatastoreException {
		if(resourceId == null) throw new IllegalArgumentException("The query parameter: '"+UrlHelpers.RESOURCE_ID+"' is required");
		return schemaManager.getEffectiveSchema(resourceId);
	}
	
	/**
	 * Get the full schema of a REST resource.
	 * 
	 * @param resourceId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.REST_RESOURCES+UrlHelpers.SCHEMA
			}, method = RequestMethod.GET)
	public @ResponseBody
	ObjectSchema getFullSchema(@RequestParam(value = UrlHelpers.RESOURCE_ID, required = true) String resourceId) throws NotFoundException, DatastoreException {
		if(resourceId == null) throw new IllegalArgumentException("The query parameter: '"+UrlHelpers.RESOURCE_ID+"' is required");
		// get the schema from the manager.
		return schemaManager.getFullSchema(resourceId);
	}
	
	/**
	 * Get the full schema of a REST resource.
	 * 
	 * @param resourceId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { 
			UrlHelpers.ENTITY+UrlHelpers.REGISTRY
			}, method = RequestMethod.GET)
	public @ResponseBody
	EntityRegistry getEntityRegistry(HttpServletRequest request) {
		// get the schema from the manager.
		return schemaManager.getEntityRegistry();
	}
	
}
