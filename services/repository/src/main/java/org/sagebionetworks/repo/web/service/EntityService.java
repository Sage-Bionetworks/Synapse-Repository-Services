package org.sagebionetworks.repo.web.service;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.PaginatedParameters;
import org.sagebionetworks.repo.web.controller.metadata.EventType;
import org.springframework.dao.DeadlockLoserDataAccessException;

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
	 * Get entities
	 * 
	 * @param userId
	 * @param offset
	 *            1-Entityd pagination offset
	 * @param limit
	 *            maximum number of results to return
	 * @param sort
	 * @param ascending
	 * @param request
	 *            used to form return URLs in the body of the response
	 * @return list of all entities stored in the repository
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public <T extends Entity> PaginatedResults<T> getEntities(String userId,
			PaginatedParameters paging,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException,
			UnauthorizedException, NotFoundException;
	
	/**
	 * Get all versions of an entity.  This list will be sorted on version number descending.
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
	public <T extends Entity> PaginatedResults<T> getAllVerionsOfEntity(String userId, Integer offset, Integer limit, String entityId,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException,
			UnauthorizedException, NotFoundException;

	/**
	 * Get all versions of an entity.  This list will be sorted on version number descending.
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
	public <T extends Entity> PaginatedResults<T> getAllVerionsOfEntity(String userId, Integer offset, Integer limit, String entityId,
			HttpServletRequest request) throws DatastoreException,
			UnauthorizedException, NotFoundException;


	/**
	 * Get a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id
	 *            the unique identifier for the entity to be returned
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the entity or exception if not found
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(String userId, String id,
			HttpServletRequest request, Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException;
	
	/**
	 * Get an entity without knowing the type
	 * @param userId
	 * @param id
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Entity getEntity(String userId, String id, HttpServletRequest request) throws NotFoundException,	DatastoreException, UnauthorizedException;
	

	/**
	 * Same as above but takes a UserInfo instead of a username.
	 * @param <T>
	 * @param info
	 * @param id
	 * @param request
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(UserInfo info, String id, HttpServletRequest request, Class<? extends T> clazz, EventType eventType) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get a specific version of an entity.  This one takes a username instead of info.
	 * @param <T>
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @param request
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntityForVersion(String userId, String id, Long versionNumber,
			HttpServletRequest request, Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException;
	
	/**
	 * Get an entity version for an unknown type.
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Entity getEntityForVersion(String userId, String id, Long versionNumber,	HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException;
	

	/**
	 * Get a specific version of an entity.
	 * @param <T>
	 * @param info
	 * @param id
	 * @param versionNumber
	 * @param request
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntityForVersion(UserInfo info, String id, Long versionNumber,
			HttpServletRequest request, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get all of the children of a given type.
	 * @param <T>
	 * @param userId
	 * @param parentId
	 * @param clazz
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public <T extends Entity> List<T> getEntityChildrenOfType(String userId, String parentId, Class<? extends T> clazz, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Get the children of a given type with paging.
	 * @param <T>
	 * @param userId
	 * @param parentId
	 * @param clazz
	 * @param paging
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> PaginatedResults<T> getEntityChildrenOfTypePaginated(String userId, String parentId, Class<? extends T> clazz, PaginatedParameters paging, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param userId
	 * @param newEntity
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 */
	public <T extends Entity> T createEntity(String userId, T newEntity,
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * Get the full path of an entity.
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<EntityHeader> getEntityPath(String userId, String entityId) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Update an existing entity
	 * <p>
	 * 
	 * @param userId
	 * @param id
	 *            the unique identifier for the entity to be updated
	 * @param updatedEntity
	 *            the object with which to overwrite the currently stored entity
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the updated entity
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T updateEntity(String userId,T updatedEntity, boolean newVersion, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException;
	
	/**
	 * Update multiple children of a single entity within a single transaction.
	 * @param <T>
	 * @param userId
	 * @param parentId
	 * @param update
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> Collection<T> aggregateEntityUpdate(String userId, String parentId, Collection<T> update,HttpServletRequest request) throws NotFoundException,
	ConflictingUpdateException, DatastoreException,
	InvalidModelException, UnauthorizedException;

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id
	 *            the unique identifier for the entity to be deleted
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> void deleteEntity(String userId, String id, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Delete an entity using only its id.  This means we must lookup the type.
	 * @param userId
	 * @param id
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public void deleteEntity(String userId, String id) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the annotations of an entity for the current version.
	 * @param userId
	 * @param id
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotations(String userId, String id,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the annotations of an entity for a specific version.
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotationsForVersion(String userId, String id, Long versionNumber,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Same as above but with a UserInfo
	 * @param info
	 * @param id
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Annotations getEntityAnnotations(UserInfo info, String id,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	public Annotations updateEntityAnnotations(String userId, String entityId,
			Annotations updatedAnnotations, HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param userId
	 * @param newEntity
	 * @param request
	 *            used to get the servlet URL prefix
	 * @param clazz the class of the entity who ACL this is
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 */
	public  AccessControlList createEntityACL(String userId, AccessControlList newEntity,
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException, ConflictingUpdateException;

	
	/**
	 * Get the ACL for a given entity
	 * @param nodeId
	 * @param userId
	 * @param clazz the class of the entity who ACL this is
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException 
	 * @throws  ACLInheritanceException - Thrown when attempting to get the ACL for a node that inherits its permissions. The exception
	 * will include the benefactor's ID. 
	 */
	public  AccessControlList getEntityACL(String entityId, String userId, HttpServletRequest request) 
		throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException;
	
	/**
	 * Get information about an entity's permissions.
	 * @param <T>
	 * @param entityId
	 * @param userId
	 * @param request
	 * @param clazz
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ACLInheritanceException
	 */
	public  <T extends Entity>  EntityHeader getEntityBenefactor(String entityId, String userId, HttpServletRequest request) 
	throws NotFoundException, DatastoreException, UnauthorizedException, ACLInheritanceException;
	
	/**
	 * Update an entity ACL. If the String 'recursive' is "true", then the ACL 
	 * will be applied to all child entities via inheritance.
	 * @param userId
	 * @param updated
	 * @param recursive
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 */
	public AccessControlList updateEntityACL(String userId, AccessControlList updated, String recursive, HttpServletRequest request) throws 
		DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException;
	
	/**
	 * Update an entity ACL. If no such ACL exists, then create it.
	 * 
	 * If the String 'recursive' is "true", then the ACL will be applied to all
	 * child entities via inheritance.
	 * 
	 * @param userId
	 * @param acl
	 * @param recursive
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException
	 */
	public AccessControlList createOrUpdateEntityACL(String userId,	AccessControlList acl, String recursive, HttpServletRequest request)
			throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException,	ConflictingUpdateException;

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id the id of the node whose inheritance is to be restored
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 */
	public void deleteEntityACL(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Execute a query and include the annotations for each entity.
	 * @param <T>
	 * @param userInfo
	 * @param query
	 * @param clazz
	 * @param request
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws UnauthorizedException 
	 */
	public QueryResults executeQueryWithAnnotations(String userId, BasicQuery query, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * determine whether a user has the given access type for a given entity
	 * @param nodeId
	 * @param userId
	 * @param clazz the class of the entity
	 * @param accessType
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException 
	 */
	public <T extends Entity> boolean hasAccess(String entityId, String userId, HttpServletRequest request, String accessType) 
		throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Delete a specific version of an entity
	 * <p>
	 * 
	 * @param userId
	 * @param id
	 * @param versionNumber 
	 *            the unique identifier for the entity to be deleted
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> void deleteEntityVersion(String userId, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Delete a specific version of an entity.
	 * @param userId
	 * @param id
	 * @param versionNumber
	 * @param classForType
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws UnauthorizedException 
	 * @throws ConflictingUpdateException 
	 */
	public <T extends Entity> void deleteEntityVersion(String userId, String id,Long versionNumber, Class<? extends Entity> classForType) throws DatastoreException, NotFoundException, UnauthorizedException, ConflictingUpdateException;
	
	/**
	 * Get the type of an entity
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public EntityHeader getEntityHeader(String userId, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the entities which refer to the given version of the given entity
	 * @param userId
	 * @param entityId
	 * @param versionNumber
	 * @param offset ONE based pagination param
	 * @param limit pagination param
	 * @request
	 * @return the headers of the entities which have references to 'entityId'
	 * 
	 */
	public PaginatedResults<EntityHeader> getEntityReferences(String userId, String entityId, Integer versionNumber, Integer offset, Integer limit, HttpServletRequest request)
			throws NotFoundException, DatastoreException;
	
	/**
	 * Get the permission for a given user and entity combination.
	 * @param userId
	 * @param entityId
	 * @return
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public UserEntityPermissions getUserEntityPermissions(String userId, String entityId) throws NotFoundException, DatastoreException;
	
	
	/**
	 * Used to test deadlock.
	 * 
	 * @param param
	 * @return
	 */
	public String throwDeadlockException(DeadlockLoserDataAccessException toThrow);
	
	/**
	 * Create a S3 token for an entity attachment.
	 * 
	 * @param userId
	 * @param id
	 * @param token
	 * @return
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	public S3AttachmentToken createS3AttachmentToken(String userId, String id,
			S3AttachmentToken token) throws UnauthorizedException, NotFoundException, DatastoreException, InvalidModelException;

	/**
	 * Generate a presigned URL for an entity attachment.
	 * 
	 * @param userId
	 * @param id
	 * @param tokenID
	 * @return
	 * @throws InvalidModelException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public PresignedUrl getAttachmentUrl(String userId, String id,
			String tokenID) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Perform a query.
	 * 
	 * @param userId
	 * @param query
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public QueryResults query(String userId, String query, HttpServletRequest request)
			throws DatastoreException, ParseException, NotFoundException,
			UnauthorizedException;

	
	/**
	 * Get the number of children that this entity has.
	 * 
	 * @param userId
	 * @param entityId
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Long getChildCount(String userId, String entityId,
			HttpServletRequest request) throws DatastoreException,
			ParseException, NotFoundException, UnauthorizedException;
	
}