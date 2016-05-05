package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.SchemaManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.provenance.Activity;
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
 * 'benefactor' is used to indicate which Entity an Entity inherits its ACL
 * from. For example, a newly created Project will be its own benefactor, while
 * a new FileEntity's benefactor will start off as its containing Project. The
 * current benefactor of any Entity can be determined using the <a
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
 * <p>
 * In addition to authorization via ACLs, entities may be restricted via AccessRequirements (ARs).
 * For more information, see <a href="#org.sagebionetworks.repo.web.controller.AccessRequirementController">
 * Access Requirement Services</a> and <a href="#org.sagebionetworks.repo.web.controller.AccessApprovalController">
 * Access Approval Services</a>
 * </p>
 * <h6>Versions</h6>
 * <p>
 * Currently, <a
 * href="${org.sagebionetworks.repo.model.FileEntity}">FileEntities</a> are
 * "versionable" meaning it is possible for it to have multiple versions of the
 * file. Whenever, a FileEntity is updated with a new <a
 * href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a> a new
 * version of the FileEntity is automatically created. The file history an
 * FileEntity can be retrieved using <a href="${GET.entity.id.version}">GET
 * /entity/{id}/version</a> method. A specific version of a FileEntity can be
 * retrieved using <a href="${GET.entity.id.version.versionNumber}">GET
 * /entity/{id}/version/{versionNumber}</a> method. The Annotations of a
 * specific version of an FileEntity can be retrieved using the <a
 * href="${GET.entity.id.version.versionNumber.annotations">GET
 * /entity/{id}/version/{versionNumber}/annotations</a> method.
 * </p>
 * <p>
 * <b><i>Note: </b>Only the File and Annotations of an Entity are include in the
 * version. All other components of an Entity such as description, name, parent,
 * ACL, and WikiPage are <b>not</b> not part of the version, and will not vary
 * from version to version.</i>
 * </p>
 * <h6>JSON Schemas</h6>
 * <p>
 * Each Entity type and Model object in Synapse is defined by a JSON schema. The
 * <a href="${GET.REST.resources}">GET /REST/resources</a> method will list the
 * full name of all Resources used by Synapse. The schema for each Resource is
 * accessible via <a href="${GET.REST.resources.schema}">GET
 * /REST/resources/schema</a>. Note: Many of these resources are composition
 * objects and one must navigate various interfaces of an object to fully digest
 * it. Therefore, a flattened (or effective) schema for each resource is
 * available from the <a href="${GET.REST.resources.effectiveSchema}">GET
 * /REST/resources/effectiveSchema</a>
 * </p>
 */
@ControllerInfo(displayName = "Entity Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
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
	 * @param generatedBy
	 *            To track the Provenance of an
	 *            Entity create, include the ID of the <a
	 *            href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *            >Activity</a> that was created to track the change. For more
	 *            information see: <a href="${POST.activity}">POST
	 *            /activity</a>. You must be the creator of the <a
	 *            href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *            >Activity</a> used here. 
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestBody Entity entity, @RequestHeader HttpHeaders header,
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException,
			IOException, JSONObjectAdapterException {
		// Now create the entity
		Entity createdEntity = serviceProvider.getEntityService().createEntity(
				userId, entity, generatedBy, request);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
	 * Note: To update an Entity the caller must be granted the <a
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
	 * @param generatedBy
	 *            To track the Provenance of an Entity update, include the ID of
	 *            the <a
	 *            href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *            >Activity</a> that was created to track the change. For more
	 *            information see: <a href="${POST.activity}">POST
	 *            /activity</a>. You must be the creator of the <a
	 *            href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *            >Activity</a> used here.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
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
				entity, newVersionBoolean, generatedBy, request);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.SKIP_TRASH_CAN_PARAM, required = false) Boolean skipTrashCan,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		if (skipTrashCan != null && skipTrashCan) {
			serviceProvider.getEntityService().deleteEntity(userId, id);
		} else {
			serviceProvider.getTrashService().moveToTrash(userId, id);
		}
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws ConflictingUpdateException,
			NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		// Pass it along
		return serviceProvider.getEntityService().updateEntityAnnotations(
				userId, id, updatedAnnotations, request);
	}

	/**
	 * This is a duplicate method to update.
	 * 
	 * @param userId
	 * @param generatedBy
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 * @throws JSONObjectAdapterException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION }, method = RequestMethod.PUT)
	public @ResponseBody
	Versionable createNewVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestHeader HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException,
			ConflictingUpdateException, JSONObjectAdapterException {

		// This is simply an update with a new version created.
		return (Versionable) updateEntityImpl(userId, header, true, generatedBy,
				request);
	}

	/**
	 * This is a duplicate of update and will be removed.
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
	@Deprecated
	private Entity updateEntityImpl(Long userId, HttpHeaders header,
			boolean newVersion, String activityId, HttpServletRequest request)
			throws IOException, NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException,
			JSONObjectAdapterException {
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
	 * Delete a specific version of a FileEntity.
	 * 
	 * @param id
	 *            The ID of the Entity
	 * @param userId
	 * @param versionNumber
	 *            The version number of the Entity to delete.
	 * @param request
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_NUMBER }, method = RequestMethod.DELETE)
	public void deleteEntityVersion(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		serviceProvider.getEntityService().deleteEntityVersion(userId, id,
				versionNumber);
	}

	/**
	 * Get a specific version of a FileEntity.
	 * 
	 * @param id
	 *            The ID of the Entity.
	 * @param userId
	 * @param versionNumber
	 *            The version number of the Entity to get.
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_NUMBER }, method = RequestMethod.GET)
	public @ResponseBody
	Entity getEntityForVersion(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the entity.
		Entity updatedEntity = serviceProvider.getEntityService()
				.getEntityForVersion(userId, id, versionNumber, request);
		// Return the results
		return updatedEntity;
	}

	/**
	 * Get the EntityHeader of an Entity given its ID. The EntityHeader is a
	 * light weight object with basic information about an Entity includes its
	 * type.
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the Entity to get the EntityHeader for.
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_TYPE }, method = RequestMethod.GET)
	public @ResponseBody
	EntityHeader getEntityType(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the type of an entity by ID.
		return serviceProvider.getEntityService().getEntityHeader(userId, id,
				null);
	}

	/**
	 * Get a batch of EntityHeader given multile Entity IDs. The EntityHeader is
	 * a light weight object with basic information about an Entity includes its
	 * type.
	 * 
	 * @param userId
	 * @param batch
	 *            A comma separated list of Entity IDs to get EntityHeaders for.
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_TYPE }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<EntityHeader> getEntityTypeBatch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.BATCH_PARAM, required = true) String batch) throws NotFoundException,
			DatastoreException, UnauthorizedException {

		String ids[] = batch.split(",");

		List<Reference> request = new ArrayList<Reference>();
		for (String id : ids) {
			Reference ref = new Reference();
			ref.setTargetId(id);
			request.add(ref);
		}
		return serviceProvider.getEntityService().getEntityHeader(userId, request);
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
	PaginatedResults<EntityHeader> getEntityVersionedTypeBatch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ReferenceList referenceList)
			throws DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getEntityHeader(userId, referenceList.getReferences());
	}

	/**
	 * Get the list of permission that the caller has on a given Entity.
	 * <p>
	 * A User's permission on an Entity is a calculation based several factors
	 * including the permission granted by the Entity's ACL and the User's group
	 * membership. There might also be extra requirement for an Entity, such as
	 * special terms-of-use or special restrictions for sensitive data. This
	 * means a client cannot accurately calculate a User's permission on an
	 * Entity simply by inspecting the Entity's ACL. Instead, all clients should
	 * use this method to get the calculated permission a User has on an Entity.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		// pass it along.
		return serviceProvider.getEntityService().getUserEntityPermissions(
				userId, id);
	}

	/**
	 * Determine if the caller have a given permission on a given Entity.
	 * <p>
	 * A User's permission on an Entity is a calculation based several factors
	 * including the permission granted by the Entity's ACL and the User's group
	 * membership. There might also be extra requirement for an Entity, such as
	 * special terms-of-use or special restrictions for sensitive data. This
	 * means a client cannot accurately calculate a User's permission on an
	 * Entity simply by inspecting the Entity's ACL. Instead, all clients should
	 * use this method to get the calculated permission a User has on an Entity.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the Entity to check the permission on.
	 * @param accessType
	 *            The permission to check. Must be from: <a
	 *            href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 *            >ACCESS_TYPE</a>
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID + UrlHelpers.ACCESS }, method = RequestMethod.GET)
	public @ResponseBody
	BooleanResult hasAccess(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false) String accessType,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException {
		// pass it along.
		return new BooleanResult(serviceProvider.getEntityService().hasAccess(
				id, userId, request, accessType));
	}

	/**
	 * Get the full path of an Entity as a List of EntityHeaders. The first
	 * EntityHeader will be the Root Entity, and the last EntityHeader will be
	 * the requested Entity.
	 * 
	 * @param id
	 *            The ID of the Entity to get the full path for.
	 * @param userId
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_PATH }, method = RequestMethod.GET)
	public @ResponseBody
	EntityPath getEntityPath(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
	 * Create a new Access Control List (ACL), overriding inheritance.
	 * <p>
	 * By default, Entities such as FileEntity and Folder inherit their permission
	 * from their containing Project. For such Entities the Project is the
	 * Entity's 'benefactor'. This permission inheritance can be overridden by
	 * creating an ACL for the Entity. When this occurs the Entity becomes its
	 * own benefactor and all permission are determined by its own ACL.
	 * </p>
	 * <p>
	 * If the ACL of an Entity is deleted, then its benefactor will
	 * automatically be set to its parent's benefactor.
	 * </p>
	 * <p>
	 * Note: The caller must be granted <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CHANGE_PERMISSIONS</a> on the Entity to call this method.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the Entity to create an ACL for.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
	 *            The ID of the Entity to get the ACL for.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, ACLInheritanceException {
		// pass it along.
		return serviceProvider.getEntityService().getEntityACL(id, userId);
	}

	/**
	 * Update an Entity's ACL.
	 * <p>
	 * Note: The caller must be granted <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CHANGE_PERMISSIONS</a> on the Entity to call this method.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the Entity to create an ACL for.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
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
	 * Delete the Access Control List (ACL) for a given Entity.
	 * <p>
	 * By default, Entities such as FileEntity and Folder inherit their permission
	 * from their containing Project. For such Entities the Project is the
	 * Entity's 'benefactor'. This permission inheritance can be overridden by
	 * creating an ACL for the Entity. When this occurs the Entity becomes its
	 * own benefactor and all permission are determined by its own ACL.
	 * </p>
	 * <p>
	 * If the ACL of an Entity is deleted, then its benefactor will
	 * automatically be set to its parent's benefactor. The ACL for a Project
	 * cannot be deleted.
	 * </p>
	 * <p>
	 * Note: The caller must be granted <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CHANGE_PERMISSIONS</a> on the Entity to call this method.
	 * </p>
	 * 
	 * @param id
	 *            The ID of the Entity that should have its ACL deleted.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException,
			ConflictingUpdateException {
		// Determine the object type from the url.
		serviceProvider.getEntityService().deleteEntityACL(userId, id);
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, ACLInheritanceException {
		if (id == null)
			throw new IllegalArgumentException("PathVariable ID cannot be null");
		// pass it along.
		return serviceProvider.getEntityService().getEntityBenefactor(id,
				userId, request);
	}

	/**
	 * Get all versions of an Entity one page at a time.
	 * 
	 * @param id
	 *            The ID of the Entity to get all versions for.
	 * @param offset
	 *            The offset index determines where this page will start from.
	 *            An index of 1 is the first entity. When null it will default
	 *            to 1. Note: Starting at 1 is a misnomer for offset and will be
	 *            changed to 0 in future versions of Synapse.
	 * @param limit
	 *            Limits the number of entities that will be fetched for this
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NO_OFFSET_EQUALS_ONE) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit) throws DatastoreException,
			UnauthorizedException, NotFoundException {

		if (limit == null) {
			limit = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM_INT;
		}

		// Determine the object type from the url.
		PaginatedResults<VersionInfo> results = serviceProvider
				.getEntityService().getAllVersionsOfEntity(userId, offset,
						limit, id);
		// Return the result
		return results;
	}

	/**
	 * Get an Entity's annotations for a specific version of a FileEntity.
	 * 
	 * @param id
	 *            The ID of the Entity.
	 * @param versionNumber
	 *            The version number of the Entity.
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_ANNOTATIONS }, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotationsForVersion(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long versionNumber, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Pass it along
		return serviceProvider.getEntityService()
				.getEntityAnnotationsForVersion(userId, id, versionNumber,
						request);
	}

	/**
	 * Get the list of Resource names for all Resources of Synapse. This
	 * includes The full names of each Entity type and Model object of the API.
	 * <p>
	 * The resulting names can be used to get the full schema or effective
	 * schema of each object (see : <a href="${GET.REST.resources.schema}">GET
	 * /REST/resources/schema</a> and <a
	 * href="${GET.REST.resources.effectiveSchema}">GET
	 * /REST/resources/effectiveSchema</a>)
	 * </p>
	 * 
	 * @param request
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REST_RESOURCES }, method = RequestMethod.GET)
	public @ResponseBody
	RestResourceList getRESTResources(HttpServletRequest request) {
		// Pass it along
		return schemaManager.getRESTResources();
	}

	/**
	 * Get the effective schema of a resource using its name.
	 * <p>
	 * Many of the Synapse resource are composition objects and one must
	 * navigate various interfaces of an object to fully digest it. This method
	 * provides a flattened (or effective) schema for the requested resource.
	 * </p>
	 * 
	 * @param resourceId
	 *            The full name of the resource (see <a
	 *            href="${GET.REST.resources}">GET /REST/resources</a> for the
	 *            full list of names).
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
	 * <p>
	 * Many of the Synapse resource are composition objects and the various
	 * interfaces must be navigated to fully digest the object. The schema
	 * objects provided by this method include this composition. If the full
	 * composition is not needed, then a flattened or effective schema can be
	 * retrieved with the <a href="${GET.REST.resources.effectiveSchema}">GET
	 * /REST/resources/effectiveSchema</a> method.
	 * </p>
	 * 
	 * @param resourceId
	 *            The full name of the resource (see <a
	 *            href="${GET.REST.resources}">GET /REST/resources</a> for the
	 *            full list of names).
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
	 * Get an existing activity for the current version of an Entity.
	 * 
	 * @param id
	 *            The ID of the activity to fetch.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getActivityForEntity(userId,
				id, request);
	}

	/**
	 * Get an existing activity for a specific version of an Entity.
	 * 
	 * @param id
	 *            The ID of the entity to fetch.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getActivityForEntity(userId,
				id, versionNumber, request);
	}

	/**
	 * Sets the genratedBy relationship for the current version of an Entity.
	 * 
	 * @param id
	 *            The ID of the entity to update.
	 * @param generatedBy
	 *            The id of the activity to connect to the entity. You must be
	 *            the creator of the <a
	 *            href="${org.sagebionetworks.repo.model.provenance.Activity}"
	 *            >Activity</a> used here.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM) String generatedBy,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().setActivityForEntity(userId,
				id, generatedBy, request);
	}

	/**
	 * Deletes the genratedBy relationship for the current version of an Entity.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		serviceProvider.getEntityService().deleteActivityForEntity(userId, id,
				request);
	}

	// Files
	/**
	 * Get the actual URL of the file associated with the current version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService()
				.getFileRedirectURLForCurrentVersion(userId, id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the actual URL of the file associated with a specific version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @param versionNumber
	 *            The version number of the FileEntity to get.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService()
				.getFileRedirectURLForVersion(userId, id, versionNumber);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the URL of the preview file associated with the current version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService()
				.getFilePreviewRedirectURLForCurrentVersion(userId, id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the URL of the preview file associated with a specific version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @param versionNumber
	 *            The version number of the FileEntity to get.
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void filePreviewRedirectURLForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService()
				.getFilePreviewRedirectURLForVersion(userId, id, versionNumber);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the FileHandles of the file currently associated with the current
	 * version of the Entity
	 * <p>
	 * If a preview exists for the file then the handle of the preview and the
	 * file will be returned with this call.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_FILE_HANDLES, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getEntityFileHandlesForCurrentVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id) throws DatastoreException,
			NotFoundException, IOException {
		// pass it!
		return serviceProvider.getEntityService()
				.getEntityFileHandlesForCurrentVersion(userId, id);
	}

	/**
	 * Get the FileHandles of the file associated with a specific version of a
	 * FileEntity.
	 * <p>
	 * If a preview exists for the file then the handle of the preview and the
	 * file will be returned with this call.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @param versionNumber
	 *            The version number of the FileEntity to get
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE_HANDLES, method = RequestMethod.GET)
	public @ResponseBody
	FileHandleResults getEntityFileHandlesForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable Long versionNumber)
			throws DatastoreException, NotFoundException, IOException {
		// pass it!
		return serviceProvider.getEntityService()
				.getEntityFileHandlesForVersion(userId, id, versionNumber);
	}

	/**
	 * Gets all FileEntities whose file's MD5 is the same as the specified MD5
	 * string.
	 * 
	 * @param md5
	 * @param userId
	 *            The user making the request
	 * @throws NotFoundException
	 *             If no such entity can be found
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_MD5 }, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<EntityHeader> getEntityHeaderByMd5(
			@PathVariable String md5,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {
		List<EntityHeader> entityHeaders = serviceProvider.getEntityService()
				.getEntityHeaderByMd5(userId, md5, request);
		PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>();
		results.setResults(entityHeaders);
		results.setTotalNumberOfResults(entityHeaders.size());
		return results;
	}

	/**
	 * Lookup an Entity ID using an alias.
	 * 
	 * @param alias
	 * @throws NotFoundException
	 *             If the given alias is not assigned to an entity.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ALIAS }, method = RequestMethod.GET)
	public @ResponseBody
	EntityId getEntityIdByAlias(
			@PathVariable String alias) throws NotFoundException{
		return serviceProvider.getEntityService().getEntityIdForAlias(alias);
	}
}
