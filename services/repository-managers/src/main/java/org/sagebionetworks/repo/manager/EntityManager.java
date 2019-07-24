package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A manager for basic editing of entities.
 * 
 * @author jmhill
 *
 */
public interface EntityManager {

	/**
	 * Create a new data.
	 * @param userInfo
	 * @param newEntity
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 */
	public <T extends Entity> String createEntity(UserInfo userInfo, T newEntity, String activityId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
		
	/**
	 * Get an existing dataset
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public <T extends Entity> T getEntity(UserInfo userInfo, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the full path of an entity.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @return The first EntityHeader in the list will be the root parent for this node, and the last
	 * will be the EntityHeader for the given node.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getEntityPath(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the full path of an entity as a '/' separated string
	 * 
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public String getEntityPathAsFilePath(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException;

	/**
	 * This version of should only be used for validation, and should not be exposed directly to the caller.
	 * 
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getEntityPathAsAdmin(String entityId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the type of an entity
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityType getEntityType(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the type of an entity for purposes of a delete action
	 * 
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public EntityType getEntityTypeForDeletion(String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException;

	/**
	 * Get the entity header.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber (optional) null means current version.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public EntityHeader getEntityHeader(UserInfo userInfo, String entityId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get an entity header for each reference.
	 * 
	 * @param userInfo
	 * @param references
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getEntityHeader(UserInfo userInfo, List<Reference> references) throws NotFoundException, DatastoreException, UnauthorizedException;

	
	/**
	 * Get the entity and annotations together.
	 * @param <T>
	 * @param userInfo
	 * @param entityId
	 * @param entityClass
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntityWithAnnotations(UserInfo userInfo, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Delete an existing dataset.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void deleteEntity(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Delete a specfic version of an entity
	 * @param userInfo
	 * @param id
	 * @param versionNumber
	 * @throws ConflictingUpdateException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void deleteEntityVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;
	
	/**
	 * Get the annotations of an entity.
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Annotations getAnnotations(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the annotations of an entity for a given version.
	 * @param userInfo
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Annotations getAnnotationsForVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * update a datasets annotations 
	 * @param userInfo
	 * @param updated
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public void updateAnnotations(UserInfo userInfo, String entityId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;
	
	/**
	 * Update a dataset.
	 * @param userInfo
	 * @param updated
	 * @param newVersion should a new version be created for this update?
	 * @param activityId Activity id for version. Activity id for entity will not be updated if new version is false and activity id is null
	 * @return True if this update created a new version of the entity.  Note: There are cases where the provided newVersion is false, but a 
	 * new version is automatically created anyway.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public <T extends Entity> boolean updateEntity(UserInfo userInfo, T updated, boolean newVersion, String activityId) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException;
	
	/**
	 * Update multiple children of a single parent within the same transaction.
	 * @param <T>
	 * @param userInfo
	 * @param update
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public <T extends Entity> List<String> aggregateEntityUpdate(UserInfo userInfo, String parentId, Collection<T> update) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException;
	
	/**
	 * Get a specific version of an entity.
	 * @param <T>
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public <T extends Entity> T getEntityForVersion(UserInfo userInfo, String entityId, Long versionNumber, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Gets the entity whose file's MD5 is the same as the specified MD5 string.
	 */
	public List<EntityHeader> getEntityHeaderByMd5(UserInfo userInfo, String md5)
			throws NotFoundException, DatastoreException;

	/**
	 * Validate that the user has read access.
	 * 
	 * @param userId
	 * @param entityId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public void validateReadAccess(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * Dev Note: since the user has update permission, we do not need to check
	 * whether they have signed the use agreement, also this is just for uploads
	 * 
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public void validateUpdateAccess(UserInfo userInfo, String entityId) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * Does an entity have children?
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 */
	public boolean doesEntityHaveChildren(UserInfo userInfo, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Return a paginated list of all version of this entity.
	 * @param userInfo
	 * @param entityId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<VersionInfo> getVersionsOfEntity(UserInfo userInfo, String entityId, long offset, long limit) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Gets the activity for the given Entity
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public Activity getActivityForEntity(UserInfo userInfo, String entityId,
			Long versionNumber) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Sets the activity for the current version of the Entity
	 * @param userInfo
	 * @param entityId
	 * @param activityId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public Activity setActivityForEntity(UserInfo userInfo, String entityId,
			String activityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Deletes the activity generated by relationship to the current version of the Entity
	 * @param userInfo
	 * @param entityId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void deleteActivityForEntity(UserInfo userInfo, String entityId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get the FileHandle ID for a given version number.
	 * @param userInfo
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	public String getFileHandleIdForVersion(UserInfo userInfo, String id, Long versionNumber)
			throws UnauthorizedException, NotFoundException;

	/**
	 * Get a reference for the current version of the given entity ids
	 * 
	 * @param entityIds entities ids to lookup
	 * @return list of References with the current version filled in
	 */
	public List<Reference> getCurrentRevisionNumbers(List<String> entityIds);

	/**
	 * Get an entity with just the ID.
	 * @param user
	 * @param entityId
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 */
	public Entity getEntity(UserInfo user, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Lookup an Entity ID using an alias.
	 * @param alias
	 * @return
	 */
	public String getEntityIdForAlias(String alias);
	
	/**
	 * A consistent query to get a page children for a given container.
	 *  
	 * @param user
	 * @param request
	 * @return
	 */
	public EntityChildrenResponse getChildren(UserInfo user, EntityChildrenRequest request);

	/**
	 * Retrieve the entityId based on its name and parentId.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public EntityId lookupChild(UserInfo userInfo, EntityLookupRequest request);

	/**
	 * Change the given entity's {@link DataType}
	 * @param userInfo
	 * @param id
	 * @param dataType
	 * @return
	 */
	public DataTypeResponse changeEntityDataType(UserInfo userInfo, String id, DataType dataType);
}
