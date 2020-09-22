package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.json.JSONObject;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ListValidationResultsRequest;
import org.sagebionetworks.repo.model.schema.ListValidationResultsResponse;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.sts.StsCredentials;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.metadata.EventType;

/**
 * Service interface for all operations common to entities.
 * 
 * @author deflaux
 * 
 * @param <T> the particular type of entity the controller is managing
 */
public interface EntityService {

	/**
	 * Get all versions of an entity. This list will be sorted on version number
	 * descending.
	 * 
	 * @param <T>
	 * @param userId
	 * @param offest
	 * @param limmit
	 * @param entityId
	 * @param request
	 * @param clazz
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public PaginatedResults<VersionInfo> getAllVersionsOfEntity(Long userId, Integer offset, Integer limit,
			String entityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id     the unique identifier for the entity to be returned
	 * @return the entity or exception if not found
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(Long userId, String id, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get an entity without knowing the type
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Entity getEntity(Long userId, String id) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Gets the header information for entities whose file's MD5 matches the given
	 * MD5 checksum.
	 */
	public List<EntityHeader> getEntityHeaderByMd5(Long userId, String md5)
			throws NotFoundException, DatastoreException;

	/**
	 * Same as above but takes a UserInfo instead of a username.
	 * 
	 * @param <T>
	 * @param info
	 * @param id
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(UserInfo info, String id, Class<? extends T> clazz, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get a specific version of an entity. This one takes a username instead of
	 * info.
	 * 
	 * @param <T>
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntityForVersion(Long userId, String id, Long versionNumber,
			Class<? extends T> clazz) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get an entity version for an unknown type.
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Entity getEntityForVersion(Long userId, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get a specific version of an entity.
	 * 
	 * @param <T>
	 * @param info
	 * @param id
	 * @param versionNumber
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntityForVersion(UserInfo info, String id, Long versionNumber,
			Class<? extends T> clazz) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param userId
	 * @param newEntity
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public <T extends Entity> T createEntity(Long userId, T newEntity, String activityId)
			throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;

	/**
	 * Get the full path of an entity.
	 * 
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<EntityHeader> getEntityPath(Long userId, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Update an existing entity
	 * <p>
	 * 
	 * @param id            the unique identifier for the entity to be updated
	 * @param userId
	 * @param updatedEntity the object with which to overwrite the currently stored
	 *                      entity
	 * @return the updated entity
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T updateEntity(Long userId, T updatedEntity, boolean newVersion, String activityId)
			throws NotFoundException, ConflictingUpdateException, DatastoreException, InvalidModelException,
			UnauthorizedException;

	/**
	 * Update request for the file handle of an entity revision
	 * 
	 * @param userId
	 * @param entityId
	 * @param versionNumber
	 * @param updateRequest
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws UnauthorizedException
	 */
	void updateEntityFileHandle(Long userId, String entityId, Long versionNumber, FileHandleUpdateRequest updateRequest)
			throws NotFoundException, ConflictingUpdateException, UnauthorizedException;

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id     the unique identifier for the entity to be deleted
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> void deleteEntity(Long userId, String id, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Delete an entity using only its id. This means we must lookup the type.
	 * 
	 * @param userId
	 * @param id
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public void deleteEntity(Long userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the annotations of an entity for the current version.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotations(Long userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the annotations of an entity for a specific version.
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotationsForVersion(Long userId, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Same as above but with a UserInfo
	 * 
	 * @param info
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotations(UserInfo info, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	public Annotations updateEntityAnnotations(Long userId, String entityId, Annotations updatedAnnotations)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException;

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param clazz     the class of the entity who ACL this is
	 * @param userId
	 * @param newEntity
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public AccessControlList createEntityACL(Long userId, AccessControlList newEntity) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ConflictingUpdateException;

	/**
	 * Get the ACL for a given entity
	 * 
	 * @param nodeId
	 * @param userId
	 * @param clazz  the class of the entity who ACL this is
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException - Thrown when attempting to get the ACL for a
	 *                                 node that inherits its permissions. The
	 *                                 exception will include the benefactor's ID.
	 */
	public AccessControlList getEntityACL(String entityId, Long userId)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException;

	/**
	 * Get information about an entity's permissions.
	 * 
	 * @param <T>
	 * @param clazz
	 * @param entityId
	 * @param userId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 */
	public <T extends Entity> EntityHeader getEntityBenefactor(String entityId, Long userId)
			throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException;

	/**
	 * Update an entity ACL.
	 * 
	 * @param userId
	 * @param updated
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public AccessControlList updateEntityACL(Long userId, AccessControlList updated) throws DatastoreException,
			NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Update an entity ACL. If no such ACL exists, then create it.
	 * 
	 * @param userId
	 * @param acl
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	public AccessControlList createOrUpdateEntityACL(Long userId, AccessControlList acl) throws DatastoreException,
			NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id     the id of the node whose inheritance is to be restored
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	public void deleteEntityACL(Long userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * determine whether a user has the given access type for a given entity
	 * 
	 * @param nodeId
	 * @param clazz      the class of the entity
	 * @param userId
	 * @param accessType
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> boolean hasAccess(String entityId, Long userId, String accessType)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Delete a specific version of an entity
	 * <p>
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber the unique identifier for the entity to be deleted
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> void deleteEntityVersion(Long userId, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Delete a specific version of an entity.
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @param classForType
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	public <T extends Entity> void deleteEntityVersion(Long userId, String id, Long versionNumber,
			Class<? extends Entity> classForType)
			throws DatastoreException, NotFoundException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Get the type of an entity
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param vertionNumber (optional) null for current version
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public EntityHeader getEntityHeader(Long userId, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get a list of Headers given a list of References.
	 * 
	 * @param userId
	 * @param references
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public PaginatedResults<EntityHeader> getEntityHeader(Long userId, List<Reference> references)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the permission for a given user and entity combination.
	 * 
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public UserEntityPermissions getUserEntityPermissions(Long userId, String entityId)
			throws NotFoundException, DatastoreException;

	/**
	 * Get the number of children that this entity has.
	 * 
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public boolean doesEntityHaveChildren(Long userId, String entityId)
			throws DatastoreException, ParseException, NotFoundException, UnauthorizedException;

	/**
	 * Gets the activity for the given Entity
	 * 
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Activity getActivityForEntity(Long userId, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Gets the activity for the given Entity version
	 * 
	 * @param userId
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Activity getActivityForEntity(Long userId, String entityId, Long versionNumber)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Sets the activity generatedBy relationship for the given Entity
	 * 
	 * @param userId
	 * @param entityId
	 * @param activityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Activity setActivityForEntity(Long userId, String entityId, String activityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Deletes the generatedBy relationship for the given Entity
	 * 
	 * @param userId
	 * @param entityId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public void deleteActivityForEntity(Long userId, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Get the file redirect URL for the current version of the entity.
	 * 
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public String getFileRedirectURLForCurrentVersion(Long userId, String entityId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the file preview redirect URL for the current version of the entity.
	 * 
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public String getFilePreviewRedirectURLForCurrentVersion(Long userId, String entityId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the file redirect URL for a given version number.
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public String getFileRedirectURLForVersion(Long userId, String id, Long versionNumber)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the file preview redirect URL for a given version number.
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public String getFilePreviewRedirectURLForVersion(Long userId, String id, Long versionNumber)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the entity file handles for the current version of an entity.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public FileHandleResults getEntityFileHandlesForCurrentVersion(Long userId, String entityId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the entity file handles for a given version of an entity.
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public FileHandleResults getEntityFileHandlesForVersion(Long userId, String entityId, Long versionNumber)
			throws DatastoreException, NotFoundException;

	/**
	 * Lookup an Entity ID using an alias.
	 * 
	 * @param alias
	 * @return
	 */
	public EntityId getEntityIdForAlias(String alias);

	/**
	 * Get the Entity children for a given parent id.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	public EntityChildrenResponse getChildren(Long userId, EntityChildrenRequest request);

	/**
	 * Retrieve an entityId given its name and parentId.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	public EntityId lookupChild(Long userId, EntityLookupRequest request);

	/**
	 * Change an Entity's {@link DataType}
	 * 
	 * @param userId
	 * @param id
	 * @param dataType
	 * @return
	 */
	public DataTypeResponse changeEntityDataType(Long userId, String id, DataType dataType);

	/** Gets the temporary S3 credentials from STS for the given entity. */
	StsCredentials getTemporaryCredentialsForEntity(Long userId, String entityId, StsPermission permission);


	/**
	 * Bind a schema to an entity.
	 * @param userId
	 * @param request
	 * @return
	 */
	public JsonSchemaObjectBinding bindSchemaToEntity(Long userId, BindSchemaToEntityRequest request);

	/**
	 * Get metadata about a JSON schema bound to an Entity.
	 * @param userId
	 * @param id
	 * @return
	 */
	public JsonSchemaObjectBinding getBoundSchema(Long userId, String id);

	/**
	 * Clear the bound JSON schema from this Entity
	 * @param userId
	 * @param id
	 * @return
	 */
	public void clearBoundSchema(Long userId, String id);

	/**
	 * Get the JSON representation of an Entity and its annotations for JSON schema validation.
	 * @param userId
	 * @param entityId
	 * @return
	 */
	public JSONObject getEntityJson(Long userId, String entityId);

	/**
	 * Update an Entity's annotations from the JSON representation of the entity.
	 * @param userId
	 * @param entityId
	 * @param request
	 * @return
	 */
	public JSONObject updateEntityJson(Long userId, String entityId, JSONObject request);

	/**
	 * Get the validation results of Entity against its bound JSON schema.
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	public ValidationResults getEntitySchemaValidationResults(Long userId, String id);

	/**
	 * Get the JSON schema validation statistics for the given container.
	 * @param userId
	 * @param entityId
	 * @return
	 */
	public ValidationSummaryStatistics getEntitySchemaValidationSummaryStatistics(Long userId, String entityId);

	/**
	 * Get a single page of invalid JSON schema validation results for the given container.
	 * @param userId
	 * @param request
	 * @return
	 */
	public ListValidationResultsResponse getInvalidEntitySchemaValidationResults(Long userId, ListValidationResultsRequest request);
}