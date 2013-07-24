package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.manager.SchemaManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.ObjectSchema;
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
 * <p>
 * All data in Synapse is organize into <a
 * href="${org.sagebionetworks.repo.model.Project}">Projects</a>. These Projects
 * can be further organized into hierarchical <a
 * href="${org.sagebionetworks.repo.model.Folder}">Folders</a>. Finally, the
 * data is then represented by <a
 * href="${org.sagebionetworks.repo.model.FileEntity}">FileEntities</a> or
 * Records (coming soon) that reside within Folders or directly within Projects.
 * All these objects (Projects, Folders, FileEntities, and Records) are derived
 * from a common object called <a
 * href="${org.sagebionetworks.repo.model.Entity}">Entity</a>. The Entity
 * Services provide the means to create, read, update, and delete Synapse
 * Entities. There are also services for navigating the Entity hierarchies ,
 * setting Authorization rules, and Annotating Entities.
 * </p>
 * <p>
 * The following methods provide the basic Create, Read, Update, Delete (CRUD)
 * for Entities:
 * </p>
 * <ul>
 * <li><a href="${POST.entity}">POST /entity</a></li>
 * <li><a href="${GET.entity.id}">GET /entity/{id}</a></li>
 * <li><a href="${PUT.entity.id}">PUT /entity/{id}</a></li>
 * <li><a href="${DELETE.entity.id}">DELETE /entity/{id}</a></li>
 * </ul>
 * <h6>Annotations</h6>
 * <p>
 * An Entity can be annotated using the <a
 * href="${PUT.entity.id.annotations}">PUT /entity/{id}/annotations</a> method.
 * Each annotation is a key-value pair. The <a href="${GET.query}">GET
 * /query</a> can be used to query for Entities based on the key-value pairs of
 * annotations. The <a href="${GET.entity.id.annotations}">GET
 * /entity/{id}/annotations</a> method can be used to get the current
 * annotations of an entity.
 * </p>
 * <h6>Authorization</h6>
 * <p>
 * An Entity's authorization is controlled by the Entity's <a
 * href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
 * List (ACL)</a>. When a new Project is created a new ACL is automatically
 * created for the Project. New Folders and FileEnties start off inheriting the
 * ACL of their containing Project. This means they do not have their own ACL
 * and all authorization is controlled by their Project's ACL. The term
 * 'benefactor' is used to indicate which Entity an Entity inherits its ACL from.
 * For example, a newly created Project will be its own benefactor, while a new
 * FileEntity's benefactor will start off as its containing Project. The current
 * benefactor of any Entity can be determined using the <a
 * href="${GET.entity.id.benefactor}">GET /entity/{id}/benefactor</a> method.
 * </p>
 * <p>
 * For the case where a Folder or FileEntity needs its own ACL (as opposed to
 * inheriting it from its containing Project) a new ACL can be created for the
 * Entity using the <a href="${POST.entity.id.acl}">POST /entity/{id}/acl</a>
 * method. When a new ACL is created for an Entity it will no longer inherit its
 * permission from its containing Project and it will become its own benefactor.
 * </p>
 * <p>
 * For the case where a Folder or FileEntity no longer needs its own ACL, the
 * ACL can deleted using the <a href="${DELETE.entity.id.acl}">DELETE
 * /entity/{id}/acl</a> method. When the ACL of an File or Folder is deleted, it
 * will automatically be assigned the same benefactor as its parent Entity.
 * Deleting the ACL of a Project is not allowed.
 * </p>
 * <p>
 * The <a href="${GET.entity.id.acl">GET /entity/{id}/acl</a> can be used to get
 * an Entity's ACL.
 * </p>
 * <p>
 * To determine what permissions a User has on an Entity, the <a
 * href="${GET.entity.id.permissions}" >GET /entity/{id}/permissions</a> method
 * should be used.
 * </p>
 * 
 */
@ControllerInfo(displayName = "Entity Services", path = "repo/v1")
@Controller
public class EntityController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	@Autowired
	SchemaManager schemaManager;

	/**
	 * Create a new Entity. This method is used to create Projects, Folders,
	 * FileEntities and Records (coming soon). The passed request body should
	 * contain the following fields:
	 * <ul>
	 * <li>name - Give your new entity a Name. <b>Note:</b> A name must be
	 * unique within the given parent, similar to a file in a folder.</li>
	 * <li>parentId - The ID of the parent Entity, such as a Folder or Project.
	 * This field should be excluded when creating a Project.</li>
	 * <li>concreteType - Indicates the type of Entity to create. The value
	 * should be one of the following: org.sagebionetworks.repo.model.Project,
	 * org.sagebionetworks.repo.model.Folder, or
	 * org.sagebionetworks.repo.model.FileEntity</li>
	 * </ul>
	 * <p>
	 * Note: To create an Entity the caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CREATE</a> on the parent Entity. Any authenticated caller
	 * can create a new Project (with parentId=null).
	 * </p>
	 * 
	 * @param userId
	 *            - The user that is doing the create.
	 * @param header
	 *            - Used to get content type information.
	 * @param activityId
	 *            To track the Provenance of an Entity create, include the ID of
	 *            the <a
	 *            href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *            >Activity</a> that was created to track the change. For more
	 *            information see: <a href="${POST.activity}">POST
	 *            /activity</a>.
	 * @return The new entity with an etag, id, and type specific metadata.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws InvalidModelException
	 *             - Thrown if the passed object does not match the expected
	 *             entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 *             - Thrown only for the case where the entity is assigned a
	 *             parent that does not exist.
	 * @throws IOException
	 *             - Thrown if there is a failure to read the header.
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.ENTITY }, method = RequestMethod.POST)
	public @ResponseBody
	Entity createEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String activityId,
			@RequestBody Entity entity, @RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException,
			IOException, JSONObjectAdapterException {
		// Now create the entity
		Entity createdEntity = serviceProvider.getEntityService().createEntity(
				userId, entity, activityId, request);
		// Finally, add the type specific metadata.
		return createdEntity;
	}

	/**
	 * Get an Entity using its ID.
	 * <p>
	 * Note: To get an Entity the caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the Entity.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the entity to fetch.
	 * @param userId
	 *            -The user that is doing the get.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException
	 *             - Thrown if the requested entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID }, method = RequestMethod.GET)
	public @ResponseBody
	Entity getEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Get the entity.
		Entity entity = serviceProvider.getEntityService().getEntity(userId,
				id, request);
		return entity;
	}

	/**
	 * Update an entity.
	 * <p>
	 * If the Entity is a FileEntity and the dataFileHandleId fields is set to a
	 * new value, then a new version will automatically be created for this
	 * update. You can also force the creation of a new version using the
	 * newVersion parameter (see below).
	 * </p>
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time an Entity is updated a new etag will be
	 * issued to the Entity. When an update is request, Synapse will compare the
	 * etag of the passed Entity with the current etag of the Entity. If the
	 * etags do not match, then the update will be rejected with a
	 * PRECONDITION_FAILED (412) response. When this occurs the caller should
	 * get the latest copy of the Entity (see: <a href="${GET.entity.id}">GET
	 * /entity/{id}</a>) and re-apply any changes to the object, then re-attempt
	 * the Entity update. This ensure the caller has any changes applied by
	 * other users before applying their own changes.
	 * </p>
	 * <p>
	 * Note: To udpate an Entity the caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the Entity.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the entity to update. This ID must match the ID of
	 *            the passed Entity in the request body.
	 * @param newVersion
	 *            To force the creation of a new version for a versionable
	 *            entity such as a FileEntity, include this optional parameter
	 *            with a value set to true (i.e. newVersion=true).
	 * @param activityId
	 *            To track the Provenance of an Entity update, include the ID of
	 *            the <a
	 *            href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *            >Activity</a> that was created to track the change. For more
	 *            information see: <a href="${POST.activity}">POST
	 *            /activity</a>.
	 * @throws NotFoundException
	 *             - Thrown if the given entity does not exist.
	 * @throws ConflictingUpdateException
	 *             - Thrown when the passed etag does not match the current etag
	 *             of an entity. This will occur when an entity gets updated
	 *             after getting the current etag.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws InvalidModelException
	 *             - Thrown if the passed entity contents doe not match the
	 *             expected schema.
	 * @throws UnauthorizedException
	 * @throws IOException
	 *             - There is a problem reading the contents.
	 * @throws JSONObjectAdapterException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID }, method = RequestMethod.PUT)
	public @ResponseBody
	Entity updateEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String activityId,
			@RequestParam(value = "newVersion", required = false) String newVersion,
			@RequestBody Entity entity, @RequestHeader HttpHeaders header,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, IOException,
			JSONObjectAdapterException {
		boolean newVersionBoolean = false;
		if (newVersion != null) {
			newVersionBoolean = Boolean.parseBoolean(newVersion);
		}
		// validate the entity
		entity = serviceProvider.getEntityService().updateEntity(userId,
				entity, newVersionBoolean, activityId, request);
		// Return the result
		return entity;
	}

	/**
	 * Delete an entity using its ID.
	 * <p>
	 * Note: To delete an Entity the caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> on the Entity.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the Entity to delete.
	 * 
	 * @param userId
	 *            - The user that is deleting the entity.
	 * @param request
	 * @throws NotFoundException
	 *             - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID }, method = RequestMethod.DELETE)
	public void deleteEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Pass it along
		serviceProvider.getEntityService().deleteEntity(userId, id);
	}

	/**
	 * Get the annotations for an entity.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> on the Entity, to get its annotations.
	 * </p>
	 * 
	 * @param id
	 *            - The id of the entity to update.
	 * @param userId
	 *            - The user that is doing the update.
	 * @param request
	 *            - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException
	 *             - Thrown if the given entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS }, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotations(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Pass it along
		return serviceProvider.getEntityService().getEntityAnnotations(userId,
				id, request);
	}

	/**
	 * Update an entities annotations.
	 * <p>
	 * Note: The caller must be granted the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the Entity, to update its annotations.
	 * </p>
	 * 
	 * @param id
	 *            - The id of the entity to update.
	 * @param userId
	 *            - The user that is doing the update.
	 * @param etag
	 *            - A valid etag must be provided for every update call.
	 * @param updatedAnnotations
	 *            - The updated annotations
	 * @param request
	 * @return the updated annotations
	 * @throws ConflictingUpdateException
	 *             - Thrown when the passed etag does not match the current etag
	 *             of an entity. This will occur when an entity gets updated
	 *             after getting the current etag.
	 * @throws NotFoundException
	 *             - Thrown if the given entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 *             - Thrown if the passed entity contents doe not match the
	 *             expected schema.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS }, method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateEntityAnnotations(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws ConflictingUpdateException,
			NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		// Pass it along
		return serviceProvider.getEntityService().updateEntityAnnotations(
				userId, id, updatedAnnotations, request);
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
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION }, method = RequestMethod.PUT)
	public @ResponseBody
	Versionable createNewVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String activityId,
			@RequestHeader HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException,
			ConflictingUpdateException, JSONObjectAdapterException {

		// This is simply an update with a new version created.
		return (Versionable) updateEntityImpl(userId, header, true, activityId,
				request);
	}

	/**
	 * Does the actually entity update.
	 * 
	 * @param userId
	 * @param header
	 * @param etag
	 * @param newVersion
	 *            - Should a new version be created to do this update?
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
			boolean newVersion, String activityId, HttpServletRequest request)
			throws IOException, NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException,
			JSONObjectAdapterException {
		@SuppressWarnings("unchecked")
		// Entity entity = (Entity)
		// objectTypeSerializer.deserialize(request.getInputStream(), header,
		// type.getClassForType(), header.getContentType());
		Entity entity = JSONEntityHttpMessageConverter.readEntity(request
				.getReader());
		// validate the entity
		entity = serviceProvider.getEntityService().updateEntity(userId,
				entity, newVersion, activityId, request);
		// Return the result
		return entity;
	}

	/**
	 * Called to delete an entity.
	 * 
	 * @param id
	 *            - The id of the user that is deleting the entity.
	 * @param userId
	 *            - The user that is deleting the entity.
	 * @param versionNumber
	 * @param request
	 * @throws NotFoundException
	 *             - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_NUMBER }, method = RequestMethod.DELETE)
	public void deleteEntityVersion(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		serviceProvider.getEntityService().deleteEntityVersion(userId, id,
				versionNumber);
	}

	/**
	 * Get an existing entity with a GET.
	 * 
	 * @param id
	 *            - The ID of the entity to fetch.
	 * @param userId
	 *            -The user that is doing the get.
	 * @param versionNumber
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException
	 *             - Thrown if the requested entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_NUMBER }, method = RequestMethod.GET)
	public @ResponseBody
	Entity getEntityForVersion(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the entity.
		@SuppressWarnings("unchecked")
		Entity updatedEntity = serviceProvider.getEntityService()
				.getEntityForVersion(userId, id, versionNumber, request);
		// Return the results
		return updatedEntity;
	}

	/**
	 * Get an existing entity with a GET.
	 * 
	 * @param id
	 *            - The ID of the entity to fetch.
	 * @param userId
	 *            -The user that is doing the get.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException
	 *             - Thrown if the requested entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_TYPE }, method = RequestMethod.GET)
	public @ResponseBody
	EntityHeader getEntityType(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the type of an entity by ID.
		return serviceProvider.getEntityService().getEntityHeader(userId, id,
				null);
	}

	/**
	 * Get the EntityHeader for an existing entity with a GET. If any item in
	 * the batch fails (e.g., with a 404) they all fail.
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
			EntityHeader entityHeader = serviceProvider.getEntityService()
					.getEntityHeader(userId, id, null);
			entityHeaders.add(entityHeader);
		}

		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>();
		results.setResults(entityHeaders);
		results.setTotalNumberOfResults(entityHeaders.size());
		return results;
	}

	/**
	 * Get the EntityHeader for a list of references with a POST. If any item in
	 * the batch fails (e.g., with a 404) it will be EXCLUDED in the result set.
	 * 
	 * @param userId
	 *            -The user that is doing the get.
	 * @param batch
	 *            - The comma-separated list of IDs of the entity to fetch.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_TYPE_HEADER }, method = RequestMethod.POST)
	public @ResponseBody
	BatchResults<EntityHeader> getEntityVersionedTypeBatch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody ReferenceList referenceList, HttpServletRequest request)
			throws DatastoreException, UnauthorizedException {
		List<EntityHeader> entityHeaders = new ArrayList<EntityHeader>();
		if (referenceList.getReferences() != null) {
			for (Reference ref : referenceList.getReferences()) {
				// Get the type of an entity by ID.
				EntityHeader entityHeader = null;
				try {
					entityHeader = serviceProvider.getEntityService()
							.getEntityHeader(userId, ref.getTargetId(),
									ref.getTargetVersionNumber());
				} catch (NotFoundException e) {
					// skip
				} catch (UnauthorizedException e) {
					// skip
				}
				if (entityHeader != null)
					entityHeaders.add(entityHeader);
			}
		}

		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>();
		results.setResults(entityHeaders);
		results.setTotalNumberOfResults(entityHeaders.size());
		return results;
	}

	/**
	 * Get the list of permission that the caller has on a given Entity.
	 * <p>
	 * A User's permission on an Entity is a calculation based several factors
	 * including the permission granted by the Entity's ACL and the User's group
	 * membership. There might also be extra requirement for an Entity, such as
	 * special terms-of-use or special restrictions for sensitive data. This
	 * means a client cannot accurately calculate a User's permission on an
	 * Entity simply by inspecting the Entity's ACL. Instead, all clients should use this
	 * method to get the calculated permission a User has on an Entity.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the Entity to get permissions for.
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID + UrlHelpers.PERMISSIONS }, method = RequestMethod.GET)
	public @ResponseBody
	UserEntityPermissions getUserEntityPermissions(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		// pass it along.
		return serviceProvider.getEntityService().getUserEntityPermissions(
				userId, id);
	}

	/**
	 * Get the headers for entities having references to an existing entity.
	 * 
	 * @param id
	 *            - The target entity's ID.
	 * @param userId
	 *            -The user that is doing the get.
	 * @param request
	 * @return The headers of the entities having references to the given entity
	 * @throws NotFoundException
	 *             - Thrown if the requested entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID + UrlHelpers.REFERENCED_BY }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<EntityHeader> getEntityReferences(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {
		// Get the type of an entity by ID.
		return serviceProvider.getEntityService().getEntityReferences(userId,
				id, null, offset, limit, request);
	}

	/**
	 * Get the headers for entities having references to an existing entity.
	 * 
	 * @param id
	 *            - The target entity's ID.
	 * @param userId
	 *            -The user that is doing the get.
	 * @param request
	 * @return The headers of the entities having references to the given entity
	 * @throws NotFoundException
	 *             - Thrown if the requested entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID + UrlHelpers.VERSION
			+ UrlHelpers.VERSION_NUMBER + UrlHelpers.REFERENCED_BY }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<EntityHeader> getEntityReferences(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@PathVariable int versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		// Get the type of an entity by ID.
		return serviceProvider.getEntityService().getEntityReferences(userId,
				id, versionNumber, offset, limit, request);
	}

	/**
	 * Get the path of an entity.
	 * 
	 * @param id
	 *            - The id of the entity to update.
	 * @param userId
	 *            - The user that is doing the update.
	 * @param request
	 *            - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException
	 *             - Thrown if the given entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_PATH }, method = RequestMethod.GET)
	public @ResponseBody
	EntityPath getEntityPath(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Wrap it up and pass it along
		List<EntityHeader> paths = serviceProvider.getEntityService()
				.getEntityPath(userId, id);
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(paths);
		return entityPath;
	}

	/**
	 * Create a new ACL, overriding inheritance.
	 * 
	 * @param id
	 * @param userId
	 *            - The user that is doing the create.
	 * @param newAcl
	 * @param request
	 *            - The body is extracted from the request.
	 * @return The new ACL, which includes the id of the affected entity
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws InvalidModelException
	 *             - Thrown if the passed object does not match the expected
	 *             entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 *             - Thrown only for the case where the entity is assigned a
	 *             parent that does not exist.
	 * @throws IOException
	 *             - Thrown if there is a failure to read the header.
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.POST)
	public @ResponseBody
	AccessControlList createEntityAcl(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList newAcl, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException,
			ConflictingUpdateException {
		if (newAcl == null)
			throw new IllegalArgumentException("New ACL cannot be null");
		if (id == null)
			throw new IllegalArgumentException(
					"ACL ID in the path cannot be null");
		newAcl.setId(id);
		AccessControlList acl = serviceProvider.getEntityService()
				.createEntityACL(userId, newAcl, request);
		return acl;
	}

	/**
	 * Get the Access Control List (ACL) for a given entity.
	 * <p>
	 * Note: If this method is called on an Entity that is inheriting its
	 * permission from another Entity a NOT_FOUND (404) response will be
	 * generated. The error response message will include the Entity's
	 * benefactor ID.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the entity to get the ACL for.
	 * @param userId
	 *            - The user that is making the request.
	 * @param request
	 * @return The entity ACL.
	 * @throws DatastoreException
	 *             - Thrown when there is a server-side problem.
	 * @throws NotFoundException
	 *             - Thrown if the entity does not exist.
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.GET)
	public @ResponseBody
	AccessControlList getEntityAcl(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, ACLInheritanceException {
		// pass it along.
		return serviceProvider.getEntityService().getEntityACL(id, userId,
				request);
	}

	/**
	 * Update an entity's ACL.
	 * 
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
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.PUT)
	public @ResponseBody
	AccessControlList updateEntityAcl(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody AccessControlList updatedACL,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		if (updatedACL == null)
			throw new IllegalArgumentException("ACL cannot be null");
		if (id == null)
			throw new IllegalArgumentException("ID cannot be null");
		if (!id.equals(updatedACL.getId()))
			throw new IllegalArgumentException("The path ID: " + id
					+ " does not match the ACL's ID: " + updatedACL.getId());
		// This is a fix for PLFM-621
		updatedACL.setId(id);
		return serviceProvider.getEntityService().updateEntityACL(userId,
				updatedACL, null, request);
	}

	/**
	 * Called to restore inheritance (vs. defining ones own ACL)
	 * 
	 * @param id
	 *            - The entity whose inheritance is to be restored
	 * @param userId
	 *            - The user that is deleting the entity.
	 * @throws NotFoundException
	 *             - Thrown when the entity to delete does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.DELETE)
	public void deleteEntityACL(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException,
			ConflictingUpdateException {
		// Determine the object type from the url.
		serviceProvider.getEntityService().deleteEntityACL(userId, id);
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
	@RequestMapping(value = { UrlHelpers.ENTITY_ID + UrlHelpers.ACCESS }, method = RequestMethod.GET)
	public @ResponseBody
	BooleanResult hasAccess(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false) String accessType,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		// pass it along.
		return new BooleanResult(serviceProvider.getEntityService().hasAccess(
				id, userId, request, accessType));
	}

	/**
	 * Get an Entity's benefactor.
	 * <p>
	 * The term 'benefactor' is used indicate which Entity an Entity inherits is
	 * ACL from. For example, a newly created Project will have its own ACL and
	 * therefore, it will be its own benefactor. A Folder will inherit its ACL
	 * (by default) from its containing Project so the Project will be the
	 * Folder's benefactor. This method will return the EntityHeader of an
	 * Entity's benefactor.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the entity to get the benefactor for.
	 * @param userId
	 *            - The user that is making the request.
	 * @param request
	 * @return The entity ACL.
	 * @throws DatastoreException
	 *             - Thrown when there is a server-side problem.
	 * @throws NotFoundException
	 *             - Thrown if the entity does not exist.
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ID_BENEFACTOR }, method = RequestMethod.GET)
	public @ResponseBody
	EntityHeader getEntityBenefactor(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, ACLInheritanceException {
		if (id == null)
			throw new IllegalArgumentException("PathVariable ID cannot be null");
		// pass it along.
		return serviceProvider.getEntityService().getEntityBenefactor(id,
				userId, request);
	}

	/**
	 * Fetch all of the entities of a given type in a paginated form.
	 * 
	 * @param id
	 * @param userId
	 *            - The id of the user doing the fetch.
	 * @param offset
	 *            - The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1.
	 * @param limit
	 *            - Limits the number of entities that will be fetched for this
	 *            page. When null it will default to 10.
	 * @param request
	 * @return A paginated list of results.
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<VersionInfo> getAllVersionsOfEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException {

		if (limit == null) {
			limit = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM_INT;
		}

		// Determine the object type from the url.
		@SuppressWarnings("unchecked")
		PaginatedResults<VersionInfo> results = serviceProvider
				.getEntityService().getAllVersionsOfEntity(userId, offset,
						limit, id, request);
		// Return the result
		return results;
	}

	/**
	 * Get the annotations for a given version of an entity.
	 * 
	 * @param id
	 *            - The id of the entity to update.
	 * @param userId
	 *            - The user that is doing the update.
	 * @param versionNumber
	 * @param request
	 *            - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException
	 *             - Thrown if the given entity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_ANNOTATIONS }, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotationsForVersion(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Pass it along
		return serviceProvider.getEntityService()
				.getEntityAnnotationsForVersion(userId, id, versionNumber,
						request);
	}

	/**
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REST_RESOURCES }, method = RequestMethod.GET)
	public @ResponseBody
	RestResourceList getRESTResources(HttpServletRequest request) {
		// Pass it along
		return schemaManager.getRESTResources();
	}

	/**
	 * 
	 * 
	 * @param resourceId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REST_RESOURCES
			+ UrlHelpers.EFFECTIVE_SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody
	ObjectSchema getEffectiveSchema(
			@RequestParam(value = UrlHelpers.RESOURCE_ID, required = true) String resourceId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {
		if (resourceId == null)
			throw new IllegalArgumentException("The query parameter: '"
					+ UrlHelpers.RESOURCE_ID + "' is required");
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
	@RequestMapping(value = { UrlHelpers.REST_RESOURCES + UrlHelpers.SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody
	ObjectSchema getFullSchema(
			@RequestParam(value = UrlHelpers.RESOURCE_ID, required = true) String resourceId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {
		if (resourceId == null)
			throw new IllegalArgumentException("The query parameter: '"
					+ UrlHelpers.RESOURCE_ID + "' is required");
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
	@RequestMapping(value = { UrlHelpers.ENTITY + UrlHelpers.REGISTRY }, method = RequestMethod.GET)
	public @ResponseBody
	EntityRegistry getEntityRegistry(HttpServletRequest request) {
		// get the schema from the manager.
		return schemaManager.getEntityRegistry();
	}

	/**
	 * Create a token used to upload an attachment.
	 * 
	 * @param id
	 * @param userId
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
	@RequestMapping(value = { UrlHelpers.ENTITY_S3_ATTACHMENT_TOKEN }, method = RequestMethod.POST)
	public @ResponseBody
	S3AttachmentToken createS3AttachmentToken(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody S3AttachmentToken token, HttpServletRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		// Pass it along
		return serviceProvider.getEntityService().createS3AttachmentToken(
				userId, id, token);
	}

	/**
	 * Create a token used to upload an attachment.
	 * 
	 * @param id
	 * @param userId
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
	@RequestMapping(value = { UrlHelpers.ENTITY_ATTACHMENT_URL }, method = RequestMethod.POST)
	public @ResponseBody
	PresignedUrl getAttachmentUrl(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody PresignedUrl url, HttpServletRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		if (url == null)
			throw new IllegalArgumentException(
					"A PresignedUrl must be provided");
		// Pass it along.
		return serviceProvider.getEntityService().getAttachmentUrl(userId, id,
				url.getTokenID());
	}

	/**
	 * Get an existing activity for the current version of an Entity with a GET.
	 * 
	 * @param id
	 *            - The ID of the activity to fetch.
	 * @param userId
	 *            -The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException
	 *             - Thrown if the requested activity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 *             - Thrown if specified user is unauthorized to access this
	 *             activity.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_GENERATED_BY }, method = RequestMethod.GET)
	public @ResponseBody
	Activity getActivityForEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getActivityForEntity(userId,
				id, request);
	}

	/**
	 * Get an existing activity for a specific version of an Entity with a GET.
	 * 
	 * @param id The ID of the entity to fetch.
	 * @param versionNumber
	 *            the version of the entity
	 * @param userId
	 *            -The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException
	 *             - Thrown if the requested activity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 *             - Thrown if specified user is unauthorized to access this
	 *             activity.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_GENERATED_BY }, method = RequestMethod.GET)
	public @ResponseBody
	Activity getActivityForEntityVersion(
			@PathVariable String id,
			@PathVariable Long versionNumber,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getActivityForEntity(userId,
				id, versionNumber, request);
	}

	/**
	 * Sets the genratedBy relationship for the current version of an Entity
	 * with a PUT
	 * 
	 * @param id
	 *            The ID of the entity to fetch.
	 * @param activityId
	 *            the id of the activity to connect to the entity
	 * @param userId
	 *            The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException
	 *             - Thrown if the requested activity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 *             - Thrown if specified user is unauthorized to access this
	 *             activity.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_GENERATED_BY }, method = RequestMethod.PUT)
	public @ResponseBody
	Activity updateActivityForEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM) String activityId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().setActivityForEntity(userId,
				id, activityId, request);
	}

	/**
	 * Deletes the genratedBy relationship for the current version of an Entity
	 * with a DELETE
	 * 
	 * @param id
	 *            - The ID of the activity to fetch.
	 * @param userId
	 *            -The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException
	 *             - Thrown if the requested activity does not exist.
	 * @throws DatastoreException
	 *             - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 *             - Thrown if specified user is unauthorized to access this
	 *             activity.
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_GENERATED_BY }, method = RequestMethod.DELETE)
	public void deleteActivityForEntity(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		serviceProvider.getEntityService().deleteActivityForEntity(userId, id,
				request);
	}

	// Files
	/**
	 * Attempt to download the raw file currently associated with the current
	 * version of the Entity. Note: This call will result in a HTTP temporary
	 * redirect (307), to the real file URL if the caller meets all of the
	 * download requirements.
	 * 
	 * @param userId
	 * @param id
	 *            The owning entity id.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void fileRedirectURLForCurrentVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider.getEntityService()
				.getFileRedirectURLForCurrentVersion(userId, id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Attempt to download the raw file of an entity for a given version number
	 * of the entity. Note: This call will result in a HTTP temporary redirect
	 * (307), to the real file URL if the caller meets all of the download
	 * requirements.
	 * 
	 * @param userId
	 * @param id
	 *            The owning entity id.
	 * @param versionNumber
	 *            The version number of the owing entity.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void fileRedirectURLForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider.getEntityService()
				.getFileRedirectURLForVersion(userId, id, versionNumber);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Attempt to download the preview of the file currently associated with the
	 * current version of the Entity. Note: This call will result in a HTTP
	 * temporary redirect (307), to the real file URL if the caller meets all of
	 * the download requirements.
	 * 
	 * @param userId
	 * @param id
	 *            The owning entity id.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void filePreviewRedirectURLForCurrentVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider.getEntityService()
				.getFilePreviewRedirectURLForCurrentVersion(userId, id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Attempt to download preview of the file of an entity for a given version
	 * number of the entity. Note: This call will result in a HTTP temporary
	 * redirect (307), to the real file URL if the caller meets all of the
	 * download requirements.
	 * 
	 * @param userId
	 * @param id
	 *            The owning entity id.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param versionNumber
	 *            The version number of the owing entity.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void filePreviewRedirectURLForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		URL redirectUrl = serviceProvider.getEntityService()
				.getFilePreviewRedirectURLForVersion(userId, id, versionNumber);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the FileHandles of the file currently associated with the current
	 * version of the Entity. If a preview exists for the file then the handle
	 * of the preview and the file will be returned with this call.
	 * 
	 * @param userId
	 * @param id
	 *            The owning entity id.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_FILE_HANDLES, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getEntityFileHandlesForCurrentVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id) throws DatastoreException,
			NotFoundException, IOException {
		// pass it!
		return serviceProvider.getEntityService()
				.getEntityFileHandlesForCurrentVersion(userId, id);
	}

	/**
	 * Get the FileHandles of the file associated with the given version number
	 * of the entity. If a preview exists for the file then the handle of the
	 * preview and the file will be returned with this call.
	 * 
	 * @param userId
	 * @param id
	 *            The owning entity id.
	 * @param versionNumber
	 *            The version number of the owing entity.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE_HANDLES, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getEntityFileHandlesForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, @PathVariable Long versionNumber)
			throws DatastoreException, NotFoundException, IOException {
		// pass it!
		return serviceProvider.getEntityService()
				.getEntityFileHandlesForVersion(userId, id, versionNumber);
	}

	/**
	 * Gets the entity whose file's MD5 is the same as the specified MD5 string.
	 * 
	 * @param md5
	 *            The MD5 to look up
	 * @param userId
	 *            The user making the request
	 * @throws NotFoundException
	 *             If no such entity can be found
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_MD5 }, method = RequestMethod.GET)
	public @ResponseBody
	BatchResults<EntityHeader> getEntityHeaderByMd5(
			@PathVariable String md5,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {
		List<EntityHeader> entityHeaders = serviceProvider.getEntityService()
				.getEntityHeaderByMd5(userId, md5, request);
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>();
		results.setResults(entityHeaders);
		results.setTotalNumberOfResults(entityHeaders.size());
		return results;
	}
}
