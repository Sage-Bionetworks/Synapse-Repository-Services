package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.authorize;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.SchemaManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Translator;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.request.ReferenceList;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ListValidationResultsRequest;
import org.sagebionetworks.repo.model.schema.ListValidationResultsResponse;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
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
 * All data in Synapse is organize into
 * <a href="${org.sagebionetworks.repo.model.Project}">Projects</a>. These
 * Projects can be further organized into hierarchical
 * <a href="${org.sagebionetworks.repo.model.Folder}">Folders</a>. Finally, the
 * data is then represented by
 * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntities</a> or
 * Records (coming soon) that reside within Folders or directly within Projects.
 * All these objects (Projects, Folders, FileEntities, and Records) are derived
 * from a common object called
 * <a href="${org.sagebionetworks.repo.model.Entity}">Entity</a>. The Entity
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
 * An Entity can be annotated using the
 * <a href="${PUT.entity.id.annotations2}">PUT /entity/{id}/annotations2</a>
 * method. Each annotation is a key-value pair. The
 * <a href="${GET.entity.id.annotations2}">GET /entity/{id}/annotations2</a>
 * method can be used to get the current annotations of an entity.
 * </p>
 * <h6>Authorization</h6>
 * <p>
 * An Entity's authorization is controlled by the Entity's
 * <a href="${org.sagebionetworks.repo.model.AccessControlList}">Access Control
 * List (ACL)</a>. When a new Project is created a new ACL is automatically
 * created for the Project. New Folders and FileEnties start off inheriting the
 * ACL of their containing Project. This means they do not have their own ACL
 * and all authorization is controlled by their Project's ACL. The term
 * 'benefactor' is used to indicate which Entity an Entity inherits its ACL
 * from. For example, a newly created Project will be its own benefactor, while
 * a new FileEntity's benefactor will start off as its containing Project. The
 * current benefactor of any Entity can be determined using the
 * <a href="${GET.entity.id.benefactor}">GET /entity/{id}/benefactor</a> method.
 * </p>
 * <p>
 * For the case where a Folder or FileEntity needs its own ACL (as opposed to
 * inheriting it from its containing Project) a new ACL can be created for the
 * Entity using the <a href="${POST.entity.id.acl}">POST /entity/{id}/acl</a>
 * method. When a new ACL is created for an Entity it will no longer inherit its
 * permission from its containing Project and it will become its own benefactor.
 * </p>
 * <p>
 * While creating or updating an ACL, only Certified Users can add DOWNLOAD
 * permission for Authenticated Users group.
 * </p>
 * <p>
 * For the case where a Folder or FileEntity no longer needs its own ACL, the
 * ACL can deleted using the <a href="${DELETE.entity.id.acl}">DELETE
 * /entity/{id}/acl</a> method. When the ACL of an File or Folder is deleted, it
 * will automatically be assigned the same benefactor as its parent Entity.
 * Deleting the ACL of a Project is not allowed.
 * </p>
 * <p>
 * The <a href="${GET.entity.id.acl}">GET /entity/{id}/acl</a> can be used to
 * get an Entity's ACL.
 * </p>
 * <p>
 * To determine what permissions a User has on an Entity, the
 * <a href="${GET.entity.id.permissions}" >GET /entity/{id}/permissions</a>
 * method should be used.
 * </p>
 * <p>
 * In addition to authorization via ACLs, entities may be restricted via
 * AccessRequirements (ARs). For more information, see <a href=
 * "#org.sagebionetworks.repo.web.controller.AccessRequirementController">
 * Access Requirement Services</a> and
 * <a href="#org.sagebionetworks.repo.web.controller.AccessApprovalController">
 * Access Approval Services</a>
 * </p>
 * <h6>Versions</h6>
 * <p>
 * Currently,
 * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntities</a> are
 * "versionable" meaning it is possible for it to have multiple versions of the
 * file. Whenever, a FileEntity is updated with a new
 * <a href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a> whose
 * MD5 differs from the MD5 of the current file hanlde a new version of the 
 * FileEntity is automatically created. The history of a FileEntity can be 
 * retrieved using <a href="${GET.entity.id.version}">GET
 * /entity/{id}/version</a> method. A specific version of a FileEntity can be
 * retrieved using <a href="${GET.entity.id.version.versionNumber}">GET
 * /entity/{id}/version/{versionNumber}</a> method. The Annotations of a
 * specific version of an FileEntity can be retrieved using the
 * <a href="${GET.entity.id.version.versionNumber.annotations2}">GET
 * /entity/{id}/version/{versionNumber}/annotations</a> method.
 * </p>
 * <p>
 * Despite being <a href="${org.sagebionetworks.repo.model.VersionableEntity}">versionable</a>,
 * <a href="${org.sagebionetworks.repo.model.table.TableEntity}">Tables</a> and 
 * <a href="${org.sagebionetworks.repo.model.table.View}">Views</a> are versioned using
 * snapshots: see <a href="${POST.entity.id.table.snapshot}">POST /entity/{id}/table/snapshot</a>
 * and <a href="${POST.entity.id.table.transaction.async.start}">POST /entity/{id}/table/transaction/async/start</a>.
 * </p>
 * <p>
 * <b><i>Note: </b>Only the File and Annotations of an Entity are included in the
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
 * <b>Entity Service Limits</b>
 * <table border="1">
 * <tr>
 * <th>resource</th>
 * <th>limit</th>
 * </tr>
 * <tr>
 * <td>Maximum size of an Entity.name</td>
 * <td>256 characters</td>
 * </tr>
 * <tr>
 * <td>Maximum size of an Entity.desciption</td>
 * <td>1000 characters</td>
 * </tr>
 * </tr>
 * <tr>
 * <td>Maximum number of versions for a single Entity</td>
 * <td>15,000</td>
 * </tr>
 * <tr>
 * <td>Maximum number of keys in Annotations</td>
 * <td>100</td>
 * </tr>
 * <tr>
 * <td>Maximum number of values associated with a single key in Annotations</td>
 * <td>100</td>
 * </tr>
 * <tr>
 * <td>Maximum total character count for all values associated with a single key in Annotations when the AnnotationValueType is STRING</td>
 * <td>500</td>
 * </tr>
 * <tr>
 * <td>Maximum hierarchical depth of an Entity</td>
 * <td>50</td>
 * </tr>
 * </table>
 */
@ControllerInfo(displayName = "Entity Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class EntityController {

	@Autowired
	ServiceProvider serviceProvider;

	@Autowired
	SchemaManager schemaManager;

	/**
	 * Create a new Entity. This method is used to create Projects, Folders,
	 * FileEntities and Records (coming soon). The passed request body should
	 * contain the following fields:
	 * <ul>
	 * <li>name - Give your new entity a Name. <b>Note:</b> A name must be unique
	 * within the given parent, similar to a file in a folder.</li>
	 * <li>parentId - The ID of the parent Entity, such as a Folder or Project. This
	 * field should be excluded when creating a Project.</li>
	 * <li>concreteType - Indicates the type of Entity to create. The value should
	 * be one of the following: org.sagebionetworks.repo.model.Project,
	 * org.sagebionetworks.repo.model.Folder, or
	 * org.sagebionetworks.repo.model.FileEntity</li>
	 * </ul>
	 * <p>
	 * Note: To create an Entity the caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CREATE</a> on the parent Entity. Any authenticated caller can
	 * create a new Project (with parentId=null).
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum number of children for a single parent entity</td>
	 * <td>10 K</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @param header      - Used to get content type information.
	 * @param generatedBy To track the Provenance of an Entity create, include the
	 *                    ID of the <a href=
	 *                    "${org.sagebionetworks.repo.model.provenance.Activity}"
	 *                    >Activity</a> that was created to track the change. For
	 *                    more information see: <a href="${POST.activity}">POST
	 *                    /activity</a>. You must be the creator of the <a href=
	 *                    "${org.sagebionetworks.repo.model.provenance.Activity}"
	 *                    >Activity</a> used here.
	 * @return The new entity with an etag, id, and type specific metadata.
	 * @throws DatastoreException         - Thrown when an there is a server
	 *                                    failure.
	 * @throws InvalidModelException      - Thrown if the passed object does not
	 *                                    match the expected entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException          - Thrown only for the case where the
	 *                                    entity is assigned a parent that does not
	 *                                    exist.
	 * @throws IOException                - Thrown if there is a failure to read the
	 *                                    header.
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.ENTITY }, method = RequestMethod.POST)
	public @ResponseBody Entity createEntity(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestBody Entity entity) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, IOException, JSONObjectAdapterException {
		// Now create the entity
		Entity createdEntity = serviceProvider.getEntityService().createEntity(userId, entity, generatedBy);
		// Finally, add the type specific metadata.
		return createdEntity;
	}

	/**
	 * Get an Entity using its ID.
	 * <p>
	 * Note: To get an Entity the caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * on the Entity.
	 * </p>
	 * 
	 * @param id      The ID of the entity to fetch.
	 * @param request
	 * @return The requested Entity if it exists.
	 * @throws NotFoundException     - Thrown if the requested entity does not
	 *                               exist.
	 * @throws DatastoreException    - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID }, method = RequestMethod.GET)
	public @ResponseBody Entity getEntity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the entity.
		Entity entity = serviceProvider.getEntityService().getEntity(userId, id);
		return entity;
	}

	/**
	 * Update an entity.
	 * <p>
	 * If the Entity is a FileEntity and the dataFileHandleId fields is set to a new
	 * value, then a new version will automatically be created for this update if the 
	 * MD5 of the new file handle does not match the MD5 of the existing file handle or if
	 * the file handles do not have an MD5 set. You can also force the creation of a new 
	 * version using the newVersion parameter
	 * (see below).
	 * </p>
	 * <p>
	 * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle
	 * concurrent updates. Each time an Entity is updated a new etag will be issued
	 * to the Entity. When an update is request, Synapse will compare the etag of
	 * the passed Entity with the current etag of the Entity. If the etags do not
	 * match, then the update will be rejected with a PRECONDITION_FAILED (412)
	 * response. When this occurs the caller should get the latest copy of the
	 * Entity (see: <a href="${GET.entity.id}">GET /entity/{id}</a>) and re-apply
	 * any changes to the object, then re-attempt the Entity update. This ensure the
	 * caller has any changes applied by other users before applying their own
	 * changes.
	 * </p>
	 * <p>
	 * Note: To update an Entity the caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the Entity.
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum number of children for a single parent entity</td>
	 * <td>10 K</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @param id          The ID of the entity to update. This ID must match the ID
	 *                    of the passed Entity in the request body.
	 * @param newVersion  To force the creation of a new version for a 
	 *                    <a href="${org.sagebionetworks.repo.model.VersionableEntity}">versionable</a> 
	 *                    entity such as a <a href= "${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a>, 
	 *                    include this optional parameter with a value set to true (i.e. newVersion=true).
	 *                    This parameter is ignored for entities of type 
	 *                    <a href="${org.sagebionetworks.repo.model.table.Table}">Table</a>
	 *                    (See <a href="${POST.entity.id.table.snapshot}">POST /entity/{id}/table/snapshot</a> instead)
	 * @param generatedBy To track the Provenance of an Entity update, include the
	 *                    ID of the <a href=
	 *                    "${org.sagebionetworks.repo.model.provenance.Activity}"
	 *                    >Activity</a> that was created to track the change. For
	 *                    more information see: <a href="${POST.activity}">POST
	 *                    /activity</a>. You must be the creator of the <a href=
	 *                    "${org.sagebionetworks.repo.model.provenance.Activity}"
	 *                    >Activity</a> used here.
	 * @throws NotFoundException          - Thrown if the given entity does not
	 *                                    exist.
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not
	 *                                    match the current etag of an entity. This
	 *                                    will occur when an entity gets updated
	 *                                    after getting the current etag.
	 * @throws DatastoreException         - Thrown when there is a server side
	 *                                    problem.
	 * @throws InvalidModelException      - Thrown if the passed entity contents doe
	 *                                    not match the expected schema.
	 * @throws UnauthorizedException
	 * @throws IOException                - There is a problem reading the contents.
	 * @throws JSONObjectAdapterException
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID }, method = RequestMethod.PUT)
	public @ResponseBody Entity updateEntity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestParam(value = "newVersion", required = false) String newVersion, @RequestBody Entity entity)
			throws NotFoundException, ConflictingUpdateException, DatastoreException, InvalidModelException,
			UnauthorizedException, IOException, JSONObjectAdapterException {
		boolean newVersionBoolean = false;
		if (newVersion != null) {
			newVersionBoolean = Boolean.parseBoolean(newVersion);
		}
		return serviceProvider.getEntityService().updateEntity(userId, entity, newVersionBoolean, generatedBy);
	}
	
	/**
	 * Updates the <a href="${org.sagebionetworks.repo.model.file.FileHandle}">FileHandle</a> associated with the <a href= "${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a>
	 * with the provided entity id and version.
	 * 
	 * @param id The id of the file entity
	 * @param versionNumber The entity version
	 * 
	 * @throws NotFoundException If a <a href= "${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> with the given id does not exist
	 * @throws ConflictingUpdateException If the old file handle id specified in the request does not match the current id of the entity or if the MD5 of the file handles does not match
	 * @throws UnauthorizedException If the user is not authorized to read and update the entity or if the new file handle id specified in the request is not owned by the user
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_FILE_HANDLE }, method = RequestMethod.PUT)
	public void updateEntityFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@PathVariable Long versionNumber,
			@RequestBody FileHandleUpdateRequest fileHandleUpdateRequest) 
					throws NotFoundException, ConflictingUpdateException, UnauthorizedException {
		serviceProvider.getEntityService().updateEntityFileHandle(userId, id, versionNumber, fileHandleUpdateRequest);
	}

	/**
	 * Moves an entity in the trash can, if the skipTrashCan is set to true will
	 * flag the entity for purge and it will be deleted as soon as possible.
	 * 
	 * <p>
	 * Note: To delete an Entity the caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> on the Entity.
	 * </p>
	 * 
	 * @param id           The ID of the Entity to delete.
	 * @param userId       - The user that is deleting the entity.
	 * @param skipTrashCan If true the entity will be flag for priority purge and
	 *                     deleted as soon as possible
	 * @param request
	 * @throws NotFoundException     - Thrown when the entity to delete does not
	 *                               exist.
	 * @throws DatastoreException    - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID }, method = RequestMethod.DELETE)
	public void deleteEntity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.SKIP_TRASH_CAN_PARAM, required = false) Boolean skipTrashCan,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException {
		boolean priorityPurge = false;

		if (skipTrashCan != null) {
			priorityPurge = skipTrashCan;
		}

		serviceProvider.getTrashService().moveToTrash(userId, id, priorityPurge);
	}

	/**
	 * Get the annotations for an entity.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * on the Entity, to get its annotations.
	 * </p>
	 * 
	 * @param id      - The id of the entity to update.
	 * @param userId  - The user that is doing the update.
	 * @param request - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException     - Thrown if the given entity does not exist.
	 * @throws DatastoreException    - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS }, method = RequestMethod.GET)
	@Deprecated
	public @ResponseBody org.sagebionetworks.repo.model.Annotations getEntityAnnotations(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return AnnotationsV2Translator
				.toAnnotationsV1(serviceProvider.getEntityService().getEntityAnnotations(userId, id));
	}

	/**
	 * Update an entities annotations.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the Entity, to update its annotations.
	 * </p>
	 * 
	 * @param id                 - The id of the entity to update.
	 * @param userId             - The user that is doing the update.
	 * @param etag               - A valid etag must be provided for every update
	 *                           call.
	 * @param updatedAnnotations - The updated annotations
	 * @param request
	 * @return the updated annotations
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not
	 *                                    match the current etag of an entity. This
	 *                                    will occur when an entity gets updated
	 *                                    after getting the current etag.
	 * @throws NotFoundException          - Thrown if the given entity does not
	 *                                    exist.
	 * @throws DatastoreException         - Thrown when there is a server side
	 *                                    problem.
	 * @throws UnauthorizedException
	 * @throws InvalidModelException      - Thrown if the passed entity contents doe
	 *                                    not match the expected schema.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS }, method = RequestMethod.PUT)
	@Deprecated
	public @ResponseBody org.sagebionetworks.repo.model.Annotations updateEntityAnnotations(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody org.sagebionetworks.repo.model.Annotations updatedAnnotations, HttpServletRequest request)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		return AnnotationsV2Translator.toAnnotationsV1(serviceProvider.getEntityService()
				.updateEntityAnnotations(userId, id, AnnotationsV2Translator.toAnnotationsV2(updatedAnnotations)));
	}

	/**
	 * Get the annotations for an entity.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * on the Entity, to get its annotations.
	 * </p>
	 *
	 * @param id      - The id of the entity to update.
	 * @param userId  - The user that is doing the update.
	 * @param request - Used to read the contents.
	 * @return The annotations for the given entity.
	 * @throws NotFoundException     - Thrown if the given entity does not exist.
	 * @throws DatastoreException    - Thrown when there is a server side problem.
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS_V2 }, method = RequestMethod.GET)
	public @ResponseBody Annotations getEntityAnnotationsV2(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getEntityAnnotations(userId, id);
	}

	/**
	 * Get an Entity's annotations for a specific version of a FileEntity.
	 *
	 * @param id            The ID of the Entity.
	 * @param versionNumber The version number of the Entity.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_ANNOTATIONS_V2 }, method = RequestMethod.GET)
	public @ResponseBody Annotations getEntityAnnotationsV2ForVersion(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getEntityAnnotationsForVersion(userId, id, versionNumber);
	}

	/**
	 * Update an Entity's annotations.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> on the Entity, to update its annotations.
	 * </p>
	 *
	 * @param id                 - The id of the entity to update.
	 * @param updatedAnnotations - The updated annotations
	 * @param request
	 * @return the updated annotations
	 * @throws ConflictingUpdateException - Thrown when the passed etag does not
	 *                                    match the current etag of an entity. This
	 *                                    will occur when an entity gets updated
	 *                                    after getting the current etag.
	 * @throws NotFoundException          - Thrown if the given entity does not
	 *                                    exist.
	 * @throws DatastoreException         - Thrown when there is a server side
	 *                                    problem.
	 * @throws UnauthorizedException
	 * @throws InvalidModelException      - Thrown if the passed entity contents doe
	 *                                    not match the expected schema.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ANNOTATIONS_V2 }, method = RequestMethod.PUT)
	public @ResponseBody Annotations updateEntityAnnotationsV2(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Annotations updatedAnnotations, HttpServletRequest request) throws ConflictingUpdateException,
			NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		return serviceProvider.getEntityService().updateEntityAnnotations(userId, id, updatedAnnotations);
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
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION }, method = RequestMethod.PUT)
	public @ResponseBody Versionable createNewVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM, required = false) String generatedBy,
			@RequestHeader HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, IOException,
			ConflictingUpdateException, JSONObjectAdapterException {

		// This is simply an update with a new version created.
		return (Versionable) updateEntityImpl(userId, header, true, generatedBy, request);
	}

	/**
	 * This is a duplicate of update and will be removed.
	 * 
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
	@Deprecated
	@RequiredScope({ view, modify })
	private Entity updateEntityImpl(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			HttpHeaders header, boolean newVersion, String activityId, HttpServletRequest request)
			throws IOException, NotFoundException, ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException, JSONObjectAdapterException {
		Entity entity = JSONEntityHttpMessageConverter.readEntity(request.getReader());
		// validate the entity
		entity = serviceProvider.getEntityService().updateEntity(userId, entity, newVersion, activityId);
		// Return the result
		return entity;
	}

	/**
	 * Delete a specific version of a FileEntity.
	 * 
	 * @param id            The ID of the Entity
	 * @param userId
	 * @param versionNumber The version number of the Entity to delete.
	 * @param request
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_NUMBER }, method = RequestMethod.DELETE)
	public void deleteEntityVersion(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable Long versionNumber,
			HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		serviceProvider.getEntityService().deleteEntityVersion(userId, id, versionNumber);
	}

	/**
	 * Get a specific version of an Entity.
	 * <p>
	 * Note: Only the current version of the Entity can be used for an Entity
	 * update. Therefore, only the current version of the Entity will be returned
	 * with the actual etag. All older versions will be returned with an eTag
	 * '00000000-0000-0000-0000-000000000000'.
	 * </p>
	 * 
	 * @param id            The ID of the Entity.
	 * @param userId
	 * @param versionNumber The version number of the Entity to get.
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_NUMBER }, method = RequestMethod.GET)
	public @ResponseBody Entity getEntityForVersion(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the entity.
		Entity updatedEntity = serviceProvider.getEntityService().getEntityForVersion(userId, id, versionNumber);
		// Return the results
		return updatedEntity;
	}

	/**
	 * Get the EntityHeader of an Entity given its ID. The EntityHeader is a light
	 * weight object with basic information about an Entity includes its type.
	 * 
	 * @param userId
	 * @param id      The ID of the Entity to get the EntityHeader for.
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_TYPE }, method = RequestMethod.GET)
	public @ResponseBody EntityHeader getEntityType(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the type of an entity by ID.
		return serviceProvider.getEntityService().getEntityHeader(userId, id);
	}

	/**
	 * Get a batch of EntityHeader given multile Entity IDs. The EntityHeader is a
	 * light weight object with basic information about an Entity includes its type.
	 * 
	 * @param userId
	 * @param batch        A comma separated list of Entity IDs to get EntityHeaders
	 *                     for.
	 * @param loginRequest
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_TYPE }, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<EntityHeader> getEntityTypeBatch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.BATCH_PARAM, required = true) String batch)
			throws NotFoundException, DatastoreException, UnauthorizedException {

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
	 * Get the EntityHeader for a list of references with a POST. If any item in the
	 * batch fails (e.g., with a 404) it will be EXCLUDED in the result set.
	 * 
	 * @param userId       -The user that is doing the get.
	 * @param batch        - The comma-separated list of IDs of the entity to fetch.
	 * @param loginRequest
	 * @return The requested Entity if it exists.
	 * @throws DatastoreException    - Thrown when an there is a server failure.
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_TYPE_HEADER }, method = RequestMethod.POST)
	public @ResponseBody PaginatedResults<EntityHeader> getEntityVersionedTypeBatch(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ReferenceList referenceList) throws DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getEntityHeader(userId, referenceList.getReferences());
	}

	/**
	 * Get the list of permission that the caller has on a given Entity.
	 * <p>
	 * A User's permission on an Entity is a calculation based several factors
	 * including the permission granted by the Entity's ACL and the User's group
	 * membership. There might also be extra requirement for an Entity, such as
	 * special terms-of-use or special restrictions for sensitive data. This means a
	 * client cannot accurately calculate a User's permission on an Entity simply by
	 * inspecting the Entity's ACL. Instead, all clients should use this method to
	 * get the calculated permission a User has on an Entity.
	 * </p>
	 * 
	 * @param id      The ID of the Entity to get permissions for.
	 * @param userId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID + UrlHelpers.PERMISSIONS }, method = RequestMethod.GET)
	public @ResponseBody UserEntityPermissions getUserEntityPermissions(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		return serviceProvider.getEntityService().getUserEntityPermissions(userId, id);
	}

	/**
	 * Determine if the caller have a given permission on a given Entity.
	 * <p>
	 * A User's permission on an Entity is a calculation based several factors
	 * including the permission granted by the Entity's ACL and the User's group
	 * membership. There might also be extra requirement for an Entity, such as
	 * special terms-of-use or special restrictions for sensitive data. This means a
	 * client cannot accurately calculate a User's permission on an Entity simply by
	 * inspecting the Entity's ACL. Instead, all clients should use this method to
	 * get the calculated permission a User has on an Entity.
	 * </p>
	 * 
	 * @param id         The ID of the Entity to check the permission on.
	 * @param accessType The permission to check. Must be from:
	 *                   <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 *                   >ACCESS_TYPE</a>
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID + UrlHelpers.ACCESS }, method = RequestMethod.GET)
	public @ResponseBody BooleanResult hasAccess(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.ACCESS_TYPE_PARAM, required = false) String accessType,
			HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		return new BooleanResult(serviceProvider.getEntityService().hasAccess(id, userId, accessType));
	}

	/**
	 * Get the full path of an Entity as a List of EntityHeaders. The first
	 * EntityHeader will be the Root Entity, and the last EntityHeader will be the
	 * requested Entity.
	 * 
	 * @param id      The ID of the Entity to get the full path for.
	 * @param userId
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_PATH }, method = RequestMethod.GET)
	public @ResponseBody EntityPath getEntityPath(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Wrap it up and pass it along
		List<EntityHeader> paths = serviceProvider.getEntityService().getEntityPath(userId, id);
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(paths);
		return entityPath;
	}

	/**
	 * Create a new Access Control List (ACL), overriding inheritance.
	 * <p>
	 * By default, Entities such as FileEntity and Folder inherit their permission
	 * from their containing Project. For such Entities the Project is the Entity's
	 * 'benefactor'. This permission inheritance can be overridden by creating an
	 * ACL for the Entity. When this occurs the Entity becomes its own benefactor
	 * and all permission are determined by its own ACL.
	 * </p>
	 * <p>
	 * If the ACL of an Entity is deleted, then its benefactor will automatically be
	 * set to its parent's benefactor.
	 * </p>
	 * <p>
	 * Note: The caller must be granted
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CHANGE_PERMISSIONS</a> on the Entity to call this method.
	 * </p>
	 * 
	 * @param id      The ID of the Entity to create an ACL for.
	 * @param newAcl
	 * @param request - The body is extracted from the request.
	 * @return The new ACL, which includes the id of the affected entity
	 * @throws DatastoreException         - Thrown when an there is a server
	 *                                    failure.
	 * @throws InvalidModelException      - Thrown if the passed object does not
	 *                                    match the expected entity schema.
	 * @throws UnauthorizedException
	 * @throws NotFoundException          - Thrown only for the case where the
	 *                                    entity is assigned a parent that does not
	 *                                    exist.
	 * @throws IOException                - Thrown if there is a failure to read the
	 *                                    header.
	 * @throws ConflictingUpdateException
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.POST)
	public @ResponseBody AccessControlList createEntityAcl(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AccessControlList newAcl, HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, IOException, ConflictingUpdateException {
		if (newAcl == null)
			throw new IllegalArgumentException("New ACL cannot be null");
		if (id == null)
			throw new IllegalArgumentException("ACL ID in the path cannot be null");
		newAcl.setId(id);
		AccessControlList acl = serviceProvider.getEntityService().createEntityACL(userId, newAcl);
		return acl;
	}

	/**
	 * Get the Access Control List (ACL) for a given entity.
	 * <p>
	 * Note: If this method is called on an Entity that is inheriting its permission
	 * from another Entity a NOT_FOUND (404) response will be generated. The error
	 * response message will include the Entity's benefactor ID.
	 * </p>
	 * 
	 * @param id The ID of the Entity to get the ACL for.
	 * @return The entity ACL.
	 * @throws DatastoreException      - Thrown when there is a server-side problem.
	 * @throws NotFoundException       - Thrown if the entity does not exist.
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.GET)
	public @ResponseBody AccessControlList getEntityAcl(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId)
			throws DatastoreException, NotFoundException, UnauthorizedException, ACLInheritanceException {
		return serviceProvider.getEntityService().getEntityACL(id, userId);
	}

	/**
	 * Update an Entity's ACL.
	 * <p>
	 * Note: The caller must be granted
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CHANGE_PERMISSIONS</a> on the Entity to call this method.
	 * </p>
	 * 
	 * @param id         The ID of the Entity to create an ACL for.
	 * @param updatedACL
	 * @param request
	 * @return the accessControlList
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.PUT)
	public @ResponseBody AccessControlList updateEntityAcl(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AccessControlList updatedACL, HttpServletRequest request) throws DatastoreException,
			NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		if (updatedACL == null)
			throw new IllegalArgumentException("ACL cannot be null");
		if (id == null)
			throw new IllegalArgumentException("ID cannot be null");
		if (!id.equals(updatedACL.getId()))
			throw new IllegalArgumentException(
					"The path ID: " + id + " does not match the ACL's ID: " + updatedACL.getId());
		// This is a fix for PLFM-621
		updatedACL.setId(id);
		return serviceProvider.getEntityService().updateEntityACL(userId, updatedACL);
	}

	/**
	 * Delete the Access Control List (ACL) for a given Entity.
	 * <p>
	 * By default, Entities such as FileEntity and Folder inherit their permission
	 * from their containing Project. For such Entities the Project is the Entity's
	 * 'benefactor'. This permission inheritance can be overridden by creating an
	 * ACL for the Entity. When this occurs the Entity becomes its own benefactor
	 * and all permission are determined by its own ACL.
	 * </p>
	 * <p>
	 * If the ACL of an Entity is deleted, then its benefactor will automatically be
	 * set to its parent's benefactor. The ACL for a Project cannot be deleted.
	 * </p>
	 * <p>
	 * Note: The caller must be granted
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.CHANGE_PERMISSIONS</a> on the Entity to call this method.
	 * </p>
	 * 
	 * @param id     The ID of the Entity that should have its ACL deleted.
	 * @param userId - The user that is deleting the entity.
	 * @throws NotFoundException          - Thrown when the entity to delete does
	 *                                    not exist.
	 * @throws DatastoreException         - Thrown when there is a server side
	 *                                    problem.
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ACL }, method = RequestMethod.DELETE)
	public void deleteEntityACL(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// Determine the object type from the url.
		serviceProvider.getEntityService().deleteEntityACL(userId, id);
	}

	/**
	 * Get an Entity's benefactor.
	 * <p>
	 * The term 'benefactor' is used indicate which Entity an Entity inherits is ACL
	 * from. For example, a newly created Project will have its own ACL and
	 * therefore, it will be its own benefactor. A Folder will inherit its ACL (by
	 * default) from its containing Project so the Project will be the Folder's
	 * benefactor. This method will return the EntityHeader of an Entity's
	 * benefactor.
	 * </p>
	 * 
	 * @param id      The ID of the entity to get the benefactor for.
	 * @param userId  - The user that is making the request.
	 * @param request
	 * @return The entity ACL.
	 * @throws DatastoreException      - Thrown when there is a server-side problem.
	 * @throws NotFoundException       - Thrown if the entity does not exist.
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_ID_BENEFACTOR }, method = RequestMethod.GET)
	public @ResponseBody EntityHeader getEntityBenefactor(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException, ACLInheritanceException {
		if (id == null)
			throw new IllegalArgumentException("PathVariable ID cannot be null");
		// pass it along.
		return serviceProvider.getEntityService().getEntityBenefactor(id, userId);
	}

	/**
	 * Get all versions of an Entity one page at a time.
	 * 
	 * @param id           The ID of the Entity to get all versions for.
	 * @param offset       The offset index determines where this page will start
	 *                     from. When null it will default to 0.
	 * @param limit        Limits the number of entities that will be fetched for
	 *                     this page. When null it will default to 10.
	 * @param loginRequest
	 * @return A paginated list of results.
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION }, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<VersionInfo> getAllVersionsOfEntity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		// Determine the object type from the url.
		PaginatedResults<VersionInfo> results = serviceProvider.getEntityService().getAllVersionsOfEntity(userId,
				offset, limit, id);
		// Return the result
		return results;
	}

	/**
	 * Get an Entity's annotations for a specific version of a FileEntity.
	 * 
	 * @param id            The ID of the Entity.
	 * @param versionNumber The version number of the Entity.
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@Deprecated
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_ANNOTATIONS }, method = RequestMethod.GET)
	public @ResponseBody org.sagebionetworks.repo.model.Annotations getEntityAnnotationsForVersion(
			@PathVariable String id, @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		return AnnotationsV2Translator.toAnnotationsV1(
				serviceProvider.getEntityService().getEntityAnnotationsForVersion(userId, id, versionNumber));
	}

	/**
	 * Get the list of Resource names for all Resources of Synapse. This includes
	 * The full names of each Entity type and Model object of the API.
	 * <p>
	 * The resulting names can be used to get the full schema or effective schema of
	 * each object (see : <a href="${GET.REST.resources.schema}">GET
	 * /REST/resources/schema</a> and
	 * <a href="${GET.REST.resources.effectiveSchema}">GET
	 * /REST/resources/effectiveSchema</a>)
	 * </p>
	 * 
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REST_RESOURCES }, method = RequestMethod.GET)
	public @ResponseBody RestResourceList getRESTResources(HttpServletRequest request) {
		// Pass it along
		return schemaManager.getRESTResources();
	}

	/**
	 * Get the effective schema of a resource using its name.
	 * <p>
	 * Many of the Synapse resource are composition objects and one must navigate
	 * various interfaces of an object to fully digest it. This method provides a
	 * flattened (or effective) schema for the requested resource.
	 * </p>
	 * 
	 * @param resourceId The full name of the resource (see
	 *                   <a href="${GET.REST.resources}">GET /REST/resources</a> for
	 *                   the full list of names).
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REST_RESOURCES + UrlHelpers.EFFECTIVE_SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody ObjectSchema getEffectiveSchema(
			@RequestParam(value = UrlHelpers.RESOURCE_ID, required = true) String resourceId,
			HttpServletRequest request) throws NotFoundException, DatastoreException {
		if (resourceId == null)
			throw new IllegalArgumentException("The query parameter: '" + UrlHelpers.RESOURCE_ID + "' is required");
		return schemaManager.getEffectiveSchema(resourceId);
	}

	/**
	 * Get the full schema of a REST resource.
	 * <p>
	 * Many of the Synapse resource are composition objects and the various
	 * interfaces must be navigated to fully digest the object. The schema objects
	 * provided by this method include this composition. If the full composition is
	 * not needed, then a flattened or effective schema can be retrieved with the
	 * <a href="${GET.REST.resources.effectiveSchema}">GET
	 * /REST/resources/effectiveSchema</a> method.
	 * </p>
	 * 
	 * @param resourceId The full name of the resource (see
	 *                   <a href="${GET.REST.resources}">GET /REST/resources</a> for
	 *                   the full list of names).
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.REST_RESOURCES + UrlHelpers.SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody ObjectSchema getFullSchema(
			@RequestParam(value = UrlHelpers.RESOURCE_ID, required = true) String resourceId,
			HttpServletRequest request) throws NotFoundException, DatastoreException {
		if (resourceId == null)
			throw new IllegalArgumentException("The query parameter: '" + UrlHelpers.RESOURCE_ID + "' is required");
		// get the schema from the manager.
		return schemaManager.getFullSchema(resourceId);
	}

	/**
	 * Get an existing activity for the current version of an Entity.
	 * 
	 * @param id      The ID of the activity to fetch.
	 * @param userId  -The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException     - Thrown if the requested activity does not
	 *                               exist.
	 * @throws DatastoreException    - Thrown when an there is a server failure.
	 * @throws UnauthorizedException - Thrown if specified user is unauthorized to
	 *                               access this activity.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_GENERATED_BY }, method = RequestMethod.GET)
	public @ResponseBody Activity getActivityForEntity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getActivityForEntity(userId, id);
	}

	/**
	 * Get an existing activity for a specific version of an Entity.
	 * 
	 * @param id            The ID of the entity to fetch.
	 * @param versionNumber the version of the entity
	 * @param userId        -The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException     - Thrown if the requested activity does not
	 *                               exist.
	 * @throws DatastoreException    - Thrown when an there is a server failure.
	 * @throws UnauthorizedException - Thrown if specified user is unauthorized to
	 *                               access this activity.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_VERSION_GENERATED_BY }, method = RequestMethod.GET)
	public @ResponseBody Activity getActivityForEntityVersion(@PathVariable String id, @PathVariable Long versionNumber,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().getActivityForEntity(userId, id, versionNumber);
	}

	/**
	 * Sets the generatedBy relationship for the current version of an Entity.
	 * 
	 * @param id          The ID of the entity to update.
	 * @param generatedBy The id of the activity to connect to the entity. You must
	 *                    be the creator of the <a href=
	 *                    "${org.sagebionetworks.repo.model.provenance.Activity}"
	 *                    >Activity</a> used here.
	 * @param userId      The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException     - Thrown if the requested activity does not
	 *                               exist.
	 * @throws DatastoreException    - Thrown when an there is a server failure.
	 * @throws UnauthorizedException - Thrown if specified user is unauthorized to
	 *                               access this activity.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_GENERATED_BY }, method = RequestMethod.PUT)
	public @ResponseBody Activity updateActivityForEntity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.GENERATED_BY_PARAM) String generatedBy, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return serviceProvider.getEntityService().setActivityForEntity(userId, id, generatedBy);
	}

	/**
	 * Deletes the generatedBy relationship for the current version of an Entity.
	 * 
	 * @param id      - The ID of the activity to fetch.
	 * @param userId  -The user that is doing the get.
	 * @param request
	 * @return The requested Activity if it exists.
	 * @throws NotFoundException     - Thrown if the requested activity does not
	 *                               exist.
	 * @throws DatastoreException    - Thrown when an there is a server failure.
	 * @throws UnauthorizedException - Thrown if specified user is unauthorized to
	 *                               access this activity.
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_GENERATED_BY }, method = RequestMethod.DELETE)
	public void deleteActivityForEntity(@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		serviceProvider.getEntityService().deleteActivityForEntity(userId, id);
	}

	// Files
	/**
	 * Get the actual URL of the file associated with the current version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the actual
	 * file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id       The ID of the FileEntity to get.
	 * @param redirect When set to false, the URL will be returned as text/plain
	 *                 instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@RequiredScope({ download })
	@RequestMapping(value = UrlHelpers.ENTITY_FILE, method = RequestMethod.GET)
	public void fileRedirectURLForCurrentVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@RequestParam(required = false) Boolean redirect, HttpServletResponse response)
			throws DatastoreException, NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService().getFileRedirectURLForCurrentVersion(userId, id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the actual URL of the file associated with a specific version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the actual
	 * file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id            The ID of the FileEntity to get.
	 * @param versionNumber The version number of the FileEntity to get.
	 * @param redirect      When set to false, the URL will be returned as
	 *                      text/plain instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@RequiredScope({ download })
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE, method = RequestMethod.GET)
	public void fileRedirectURLForVersion(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable Long versionNumber, @RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException, NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService().getFileRedirectURLForVersion(userId, id, versionNumber);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the URL of the preview file associated with the current version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the actual
	 * file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id       The ID of the FileEntity to get.
	 * @param redirect When set to false, the URL will be returned as text/plain
	 *                 instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({ download })
	@RequestMapping(value = UrlHelpers.ENTITY_FILE_PREVIEW, method = RequestMethod.GET)
	public void filePreviewRedirectURLForCurrentVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@RequestParam(required = false) Boolean redirect, HttpServletResponse response)
			throws DatastoreException, NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService().getFilePreviewRedirectURLForCurrentVersion(userId, id);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the URL of the preview file associated with a specific version of a
	 * FileEntity.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the actual
	 * file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id            The ID of the FileEntity to get.
	 * @param versionNumber The version number of the FileEntity to get.
	 * @param redirect      When set to false, the URL will be returned as
	 *                      text/plain instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({ download })
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE_PREVIEW, method = RequestMethod.GET)
	public void filePreviewRedirectURLForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@PathVariable Long versionNumber, @RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException, NotFoundException, IOException {
		// Get the redirect url
		String redirectUrl = serviceProvider.getEntityService().getFilePreviewRedirectURLForVersion(userId, id,
				versionNumber);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the FileHandles of the file currently associated with the current version
	 * of the Entity
	 * <p>
	 * If a preview exists for the file then the handle of the preview and the file
	 * will be returned with this call.
	 * </p>
	 * 
	 * @param id The ID of the FileEntity to get.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({ view })
	@RequestMapping(value = UrlHelpers.ENTITY_FILE_HANDLES, method = RequestMethod.GET)
	public @ResponseBody FileHandleResults getEntityFileHandlesForCurrentVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id)
			throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getEntityService().getEntityFileHandlesForCurrentVersion(userId, id);
	}

	/**
	 * Get the FileHandles of the file associated with a specific version of a
	 * FileEntity.
	 * <p>
	 * If a preview exists for the file then the handle of the preview and the file
	 * will be returned with this call.
	 * </p>
	 * 
	 * @param id            The ID of the FileEntity to get.
	 * @param versionNumber The version number of the FileEntity to get
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({ view })
	@RequestMapping(value = UrlHelpers.ENTITY_VERSION_FILE_HANDLES, method = RequestMethod.GET)
	public @ResponseBody FileHandleResults getEntityFileHandlesForVersion(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@PathVariable Long versionNumber) throws DatastoreException, NotFoundException, IOException {
		return serviceProvider.getEntityService().getEntityFileHandlesForVersion(userId, id, versionNumber);
	}

	/**
	 * Gets at most 200 FileEntities matching the given MD5 string which the
	 * user has read access to. NOTE: Another option is to create a file view that includes MD5 values.
	 * See <a href="https://docs.synapse.org/articles/views.html">https://docs.synapse.org/articles/views.html</a>
	 *
	 * 
	 * @param md5
	 * @param userId The user making the request
	 * @throws NotFoundException If no such entity can be found
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_MD5 }, method = RequestMethod.GET)
	public @ResponseBody PaginatedResults<EntityHeader> getEntityHeaderByMd5(@PathVariable String md5,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException {
		List<EntityHeader> entityHeaders = serviceProvider.getEntityService().getEntityHeaderByMd5(userId, md5);
		PaginatedResults<EntityHeader> results = new PaginatedResults<EntityHeader>();
		results.setResults(entityHeaders);
		results.setTotalNumberOfResults(entityHeaders.size());
		return results;
	}

	/**
	 * Lookup an Entity ID using an alias.
	 * 
	 * @param alias
	 * @throws NotFoundException If the given alias is not assigned to an entity.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ALIAS }, method = RequestMethod.GET)
	public @ResponseBody EntityId getEntityIdByAlias(@PathVariable String alias) throws NotFoundException {
		return serviceProvider.getEntityService().getEntityIdForAlias(alias);
	}

	/**
	 * Get a page of children for a given parent ID. This service can also be used
	 * to list projects by setting the parentId to NULL in EntityChildrenRequest.
	 * 
	 * @param userId
	 * @param parentId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_CHILDREN }, method = RequestMethod.POST)
	public @ResponseBody EntityChildrenResponse getChildren(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody(required = true) EntityChildrenRequest request) {
		return serviceProvider.getEntityService().getChildren(userId, request);
	}

	/**
	 * Retrieve an entityId for a given parent ID and entity name. This service can
	 * also be used to lookup projectId by setting the parentId to NULL in
	 * EntityLookupRequest.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_CHILD }, method = RequestMethod.POST)
	public @ResponseBody EntityId lookupChild(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody(required = true) EntityLookupRequest request) {
		return serviceProvider.getEntityService().lookupChild(userId, request);
	}

	/**
	 * Change the <a href="${org.sagebionetworks.repo.model.DataType}" >DataType</a>
	 * of the given entity. The entity's DataType controls how the entity can be
	 * accessed. For example, an entity's DataType must be set to 'open_data' in
	 * order for anonymous to be allowed to access its contents.
	 * 
	 * <p>
	 * Note: The caller must be a member of the 'Synapse Access and Compliance Team'
	 * (id=464532) in order to change an Entity's type to 'OPEN_DATA'. The caller
	 * must be grated UPDATED on the Entity to change the its type to any other
	 * value.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 * @param dataType
	 */
	@RequiredScope({ view, modify, authorize })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_DATA_TYPE }, method = RequestMethod.PUT)
	public @ResponseBody DataTypeResponse changeEntityDataType(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@RequestParam(value = "type") DataType dataType) {
		return serviceProvider.getEntityService().changeEntityDataType(userId, id, dataType);
	}

	/**
	 * Gets the temporary S3 credentials from STS for the given entity. These
	 * credentials are only good for the bucket and base key specified by the
	 * returned credentials and expire 12 hours after this API is called.
	 *
	 * <p>
	 * The specified entity must be a folder with an STS-enabled storage location.
	 * If that storage location is external storage, you may request read-only or
	 * read-write permissions. If that storage location is Synapse storage, you must
	 * request read-only permissions.
	 * </p>
	 *
	 * @param id         The ID of the entity to get credentials. This must be a
	 *                   folder with an STS-enabled storage location.
	 * @param permission Read-only or read-write permissions. See <a href=
	 *                   "${org.sagebionetworks.repo.model.sts.StsPermission}">StsPermission</a>.
	 */
	@RequiredScope({ view, modify, download })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_STS }, method = RequestMethod.GET)
	public @ResponseBody StsCredentials getTemporaryCredentialsForEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@RequestParam(value = "permission") StsPermission permission) {
		return serviceProvider.getEntityService().getTemporaryCredentialsForEntity(userId, id, permission);
	}

	/**
	 * <p>
	 * Bind a JSON schema to an Entity. The bound schema will be used to validate
	 * the Entity's metadata (annotations). Any child Entity that does not have a
	 * bound schema will inherit the first bound schema found in its hierarchy.
	 * </p>
	 * <p>
	 * Only a single schema can be bound to an Entity at a time. If you have more
	 * than one schema to bind to an Entity you will need to create and bind a
	 * single composition schema using keywords such as 'anyOf', 'allOf' or 'oneOf'
	 * that defines how the schemas should be used for validation.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the Entity to bind.
	 * </p>
	 * 
	 * @param userId
	 * @param id      The syn ID of the entity to bind.
	 * @param request The request identifies the JSON schema to bind.
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_BIND_JSON_SCHEMA }, method = RequestMethod.PUT)
	public @ResponseBody JsonSchemaObjectBinding bindJsonSchemaToEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id, @RequestBody(required = true) BindSchemaToEntityRequest request) {
		request.setEntityId(id);
		return serviceProvider.getEntityService().bindSchemaToEntity(userId, request);
	}

	/**
	 * Get information about a JSON schema bound to an Entity. Note: Any child
	 * Entity that does not have a bound schema will inherit the first bound schema
	 * found in its hierarchy.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * permission on the Entity.
	 * </p>
	 * 
	 * @param userId
	 * @param id     The ID of the entity.
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_BIND_JSON_SCHEMA }, method = RequestMethod.GET)
	public @ResponseBody JsonSchemaObjectBinding getBoundJsonSchema(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id) {
		return serviceProvider.getEntityService().getBoundSchema(userId, id);
	}

	/**
	 * Clear the bound JSON schema from this Entity. The schema will no longer be
	 * used to validate this Entity or its children.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.DELETE</a> permission on the Entity.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	@RequiredScope({ modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = { UrlHelpers.ENTITY_BIND_JSON_SCHEMA }, method = RequestMethod.DELETE)
	public void clearBoundSchema(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id) {
		serviceProvider.getEntityService().clearBoundSchema(userId, id);
	}

	/**
	 * Get the raw JSON for the given entity. The resulting JSON can be used for the
	 * validation of a entity against a
	 * <a href="${org.sagebionetworks.repo.model.schema.JsonSchema}">JsonSchema</a>.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * permission on the Entity.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_JSON }, method = RequestMethod.GET)
	public @ResponseBody JSONObject getEntityJson(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id) {
		return serviceProvider.getEntityService().getEntityJson(userId, id);
	}

	/**
	 * Update the annotations of an entity using the raw JSON of the entity.
	 * <p>
	 * See: <a href="${GET.entity.id.json}">GET entity/{id}/json</a> to get the JSON
	 * of an entity.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.UPDATE
	 * and ACCESS_TYPE.READ</a> permission on the Entity.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 * @param request
	 * @return
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_JSON }, method = RequestMethod.PUT)
	public @ResponseBody JSONObject updateEntityWithJson(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id, @RequestBody(required = true) JSONObject request) {
		return serviceProvider.getEntityService().updateEntityJson(userId, id, request);
	}

	/**
	 * Get the validation results of an Entity against its bound JSON schema. The
	 * validation of an Entity against its bound schema is automatic and eventually
	 * consistent. The validation results include the etag of the Entity at the time
	 * of the last validation. If the returned etag does not match the current etag
	 * of the Entity then the results should be considered out-of-date. If an Entity
	 * has not been validated for the first time, or if the Entity does not have a
	 * bound schema, this method will return a 404 (not-found). Keep checking for
	 * the latest validation results.
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * permission on the Entity.
	 * </p>
	 * 
	 * @param userId
	 * @param id     The ID of the Entity.
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_VALIDATION }, method = RequestMethod.GET)
	public @ResponseBody ValidationResults getEntitySchemaValidationResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id) {
		return serviceProvider.getEntityService().getEntitySchemaValidationResults(userId, id);
	}

	/**
	 * Get the The summary statistics of the JSON schema validation results for a
	 * single container Entity such as a Project or Folder. Only direct children of
	 * the container are included in the results. The statistics include the total
	 * number of children in the container, and the counts for both the invalid and
	 * valid children. If an Entity has not been validated for the first time, or it
	 * does not have bound schema it will be counted as 'unknown'.
	 * <p>
	 * The validation of an Entity against its bound schema is automatic and
	 * eventually consistent. Keep checking this method to get the latest validation
	 * statistics for the given container.
	 * </p>
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * permission on the container Entity. The resulting statistics will only
	 * include children that the caller has the READ permission on.
	 * </p>
	 * 
	 * @param userId
	 * @param id     The ID of the container Entity.
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_VALIDATION_STATISTICS }, method = RequestMethod.GET)
	public @ResponseBody ValidationSummaryStatistics getEntitySchemaValidationStatistics(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id) {
		return serviceProvider.getEntityService().getEntitySchemaValidationSummaryStatistics(userId, id);
	}

	/**
	 * Get a single page of invalid JSON schema validation results for a container
	 * Entity (Project or Folder). The validation of an Entity against its bound
	 * schema is automatic and eventually consistent. The validation results include
	 * the etag of the Entity at the time of the last validation. If the returned
	 * etag does not match the current etag of the Entity then the results should be
	 * considered out-of-date. 
	 * <p>
	 * Note: The caller must be granted the
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * permission on the container Entity. The results will only include children
	 * that the caller has the READ permission on.
	 * </p>
	 * 
	 * @param userId
	 * @param id      The ID of the container Entity.
	 * @param request
	 * @return
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = { UrlHelpers.ENTITY_ID_VALIDATION_INVALID }, method = RequestMethod.POST)
	public @ResponseBody ListValidationResultsResponse getInvalidValidationResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable(required = true) String id,
			@RequestBody(required = true) ListValidationResultsRequest request) {
		request.setContainerId(id);
		return serviceProvider.getEntityService().getInvalidEntitySchemaValidationResults(userId, request);
	}
}
