package org.sagebionetworks.repo.web.service;

import java.util.List;

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
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.provenance.Activity;
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
 * @param <T>
 *            the particular type of entity the controller is managing
 */
public interface EntityService {

	/**
	 * Get all versions of an entity. This list will be sorted on version number
	 * descending.
	 * 
	 * @param <T>
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
	public PaginatedResults<VersionInfo> getAllVersionsOfEntity(UserInfo userInfo,
			Integer offset, Integer limit, String entityId) throws DatastoreException,
			UnauthorizedException, NotFoundException;

	/**
	 * Get a specific entity
	 * <p>
	 * 
	 * @param id
	 *            the unique identifier for the entity to be returned
	 * @return the entity or exception if not found
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(UserInfo userInfo, String id,
										  Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get an entity without knowing the type
	 * 
	 * @param accessToken
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Entity getEntity(UserInfo userInfo, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Gets the header information for entities whose file's MD5 matches the given MD5 checksum.
	 */
	public List<EntityHeader> getEntityHeaderByMd5(UserInfo userInfo, String md5)
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
	public <T extends Entity> T getEntity(UserInfo info, String id,
										  Class<? extends T> clazz,
										  EventType eventType) throws NotFoundException, DatastoreException,
			UnauthorizedException;

	/**
	 * Get an entity version for an unknown type.
	 * 
	 * @param accessToken
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Entity getEntityForVersion(UserInfo userInfo, String id,
									  Long versionNumber)
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
	public <T extends Entity> T getEntityForVersion(UserInfo info, String id,
													Long versionNumber,
													Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param userInfo
	 * @param newEntity
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public <T extends Entity> T createEntity(UserInfo userInfo, T newEntity,
											 String activityId)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException;

	/**
	 * Get the full path of an entity.
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<EntityHeader> getEntityPath(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Update an existing entity
	 * <p>
	 * 
	 * @param accessToken
	 * @param updatedEntity
	 *            the object with which to overwrite the currently stored entity
	 * @return the updated entity
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T updateEntity(UserInfo userInfo, T updatedEntity,
											 boolean newVersion, String activityId)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException;

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param id
	 *            the unique identifier for the entity to be deleted
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> void deleteEntity(UserInfo userInfo, String id,
			Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	/**
	 * Delete an entity using only its id. This means we must lookup the type.
	 * 
	 * @param id
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public void deleteEntity(UserInfo userInfo, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the annotations of an entity for the current version.
	 * 
	 * @param userInfo
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotations(UserInfo userInfo, String id) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	/**
	 * Get the annotations of an entity for a specific version.
	 * 
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotationsForVersion(UserInfo userInfo, String id,
													  Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	public Annotations updateEntityAnnotations(UserInfo userInfo, String entityId,
											   Annotations updatedAnnotations)
			throws ConflictingUpdateException, NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param clazz
	 *            the class of the entity who ACL this is
	 * @param accessToken
	 * @param newEntity
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public AccessControlList createEntityACL(UserInfo userInfo,
											 AccessControlList newEntity)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException,
			ConflictingUpdateException;

	/**
	 * Get the ACL for a given entity
	 * 
	 * @param nodeId
	 * @param accessToken
	 * @param clazz
	 *            the class of the entity who ACL this is
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 *             - Thrown when attempting to get the ACL for a node that
	 *             inherits its permissions. The exception will include the
	 *             benefactor's ID.
	 */
	public AccessControlList getEntityACL(String entityId, UserInfo userInfo) throws NotFoundException,
			DatastoreException, UnauthorizedException, ACLInheritanceException;

	/**
	 * Get information about an entity's permissions.
	 * 
	 * @param <T>
	 * @param clazz
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 */
	public <T extends Entity> EntityHeader getEntityBenefactor(String entityId,
			UserInfo userInfo)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ACLInheritanceException;

	/**
	 * Update an entity ACL.
	 * 
	 * @param userInfo
	 * @param updated
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public AccessControlList updateEntityACL(UserInfo userInfo, AccessControlList updated) throws DatastoreException,
			NotFoundException, InvalidModelException, UnauthorizedException,
			ConflictingUpdateException;

	/**
	 * Update an entity ACL. If no such ACL exists, then create it.
	 * 
	 * @param userInfo
	 * @param acl
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	public AccessControlList createOrUpdateEntityACL(UserInfo userInfo, AccessControlList acl)
			throws DatastoreException, NotFoundException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException;

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param id
	 *            the id of the node whose inheritance is to be restored
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	public void deleteEntityACL(UserInfo userInfo, String id)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException;

	/**
	 * determine whether a user has the given access type for a given entity
	 * 
	 * @param nodeId
	 * @param clazz
	 *            the class of the entity
	 * @param accessType
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> boolean hasAccess(String entityId, UserInfo userInfo,
												String accessType)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Delete a specific version of an entity
	 * <p>
	 * 
	 * @param id
	 * @param versionNumber
	 *            the unique identifier for the entity to be deleted
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> void deleteEntityVersion(UserInfo userInfo,
			String id, Long versionNumber) throws NotFoundException,
			DatastoreException, UnauthorizedException,
			ConflictingUpdateException;

	/**
	 * Delete a specific version of an entity.
	 * 
	 * @param id
	 * @param versionNumber
	 * @param classForType
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	public <T extends Entity> void deleteEntityVersion(UserInfo userInfo,
			String id, Long versionNumber, Class<? extends Entity> classForType)
			throws DatastoreException, NotFoundException,
			UnauthorizedException, ConflictingUpdateException;

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
	public EntityHeader getEntityHeader(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get a list of Headers given a list of References.
	 * @param references
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public PaginatedResults<EntityHeader> getEntityHeader(UserInfo userInfo, List<Reference> references)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the permission for a given user and entity combination.
	 * 
	 * @param accessToken
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public UserEntityPermissions getUserEntityPermissions(UserInfo userInfo,
			String entityId) throws NotFoundException, DatastoreException;

	/**
	 * Get the number of children that this entity has.
	 * 
	 * @param accessToken
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public boolean doesEntityHaveChildren(UserInfo userInfo, String entityId) throws DatastoreException,
			ParseException, NotFoundException, UnauthorizedException;

	/**
	 * Gets the activity for the given Entity
	 * 
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Activity getActivityForEntity(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Gets the activity for the given Entity version
	 * 
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Activity getActivityForEntity(UserInfo userInfo, String entityId,
										 Long versionNumber)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Sets the activity generatedBy relationship for the given Entity
	 * 
	 * @param entityId
	 * @param activityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Activity setActivityForEntity(UserInfo userInfo, String entityId,
										 String activityId)
			throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Deletes the generatedBy relationship for the given Entity
	 * @param entityId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public void deleteActivityForEntity(UserInfo userInfo, String entityId) throws DatastoreException,
			NotFoundException, UnauthorizedException;

	/**
	 * Get the file redirect URL for the current version of the entity.
	 * @param entityId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getFileRedirectURLForCurrentVersion(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the file preview redirect URL for the current version of the entity.
	 * @param entityId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getFilePreviewRedirectURLForCurrentVersion(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException;


	/**
	 * Get the file redirect URL for a given version number.
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getFileRedirectURLForVersion(UserInfo userInfo, String id, Long versionNumber) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the file preview redirect URL for a given version number.
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getFilePreviewRedirectURLForVersion(UserInfo userInfo, String id, Long versionNumber) throws DatastoreException,
			NotFoundException;

	/**
	 * Get the entity file handles for the current version of an entity.
	 * 
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public FileHandleResults getEntityFileHandlesForCurrentVersion(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException;

	/**
	 * Get the entity file handles for a given version of an entity.
	 * @param accessToken
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public FileHandleResults getEntityFileHandlesForVersion(UserInfo userInfo, String entityId, Long versionNumber) throws DatastoreException, NotFoundException;

	/**
	 * Lookup an Entity ID using an alias.
	 * @param alias
	 * @return
	 */
	public EntityId getEntityIdForAlias(String alias);
	
	/**
	 * Get the Entity children for a given parent id.
	 * 
	 * @param request
	 * @return
	 */
	public EntityChildrenResponse getChildren(UserInfo userInfo, EntityChildrenRequest request);

	/**
	 * Retrieve an entityId given its name and parentId.
	 * 
	 * @param request
	 * @return
	 */
	public EntityId lookupChild(UserInfo userInfo, EntityLookupRequest request);

	/**
	 * Change an Entity's {@link DataType}
	 * 
	 * @param id
	 * @param dataType
	 * @return
	 */
	public DataTypeResponse changeEntityDataType(UserInfo userInfo, String id, DataType dataType);

	/** Gets the temporary S3 credentials from STS for the given entity. */
	StsCredentials getTemporaryCredentialsForEntity(UserInfo userInfo, String entityId, StsPermission permission);
}