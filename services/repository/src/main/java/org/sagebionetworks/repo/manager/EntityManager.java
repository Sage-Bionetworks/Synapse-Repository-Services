package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	public <T extends Base> String createEntity(UserInfo userInfo, T newEntity) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
		
	/**
	 * Get an existing dataset
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public <T extends Base> T getEntity(UserInfo userInfo, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;
	
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
	 * Get the type of an entity
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public ObjectType getEntityType(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the children of of an entity.
	 * @param <T>
	 * @param userInfo
	 * @param parentId
	 * @param childrenClass
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Nodeable> List<T> getEntityChildren(UserInfo userInfo, String parentId, Class<? extends T> childrenClass) throws NotFoundException, DatastoreException, UnauthorizedException;

	
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
	public <T extends Base> EntityWithAnnotations<T> getEntityWithAnnotations(UserInfo userInfo, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;

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
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public <T extends Base> void updateEntity(UserInfo userInfo, T updated, boolean newVersion) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException;
	
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
	public <T extends Nodeable> List<String> aggregateEntityUpdate(UserInfo userInfo, String parentId, Collection<T> update) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException;
	
	/**
	 * List all version numbers for an entity.
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public List<Long> getAllVersionNumbersForEntity(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
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
	public <T extends Nodeable> T getEntityForVersion(UserInfo userInfo, String entityId, Long versionNumber, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Used to override this dao for a test.
	 * @param mockAuth
	 */
	public void overrideAuthDaoForTest(AuthorizationManager mockAuth);





}
