package org.sagebionetworks.repo.web;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider.EventType;

/**
 * Controller interface for all operations common to entities.
 * 
 * @author deflaux
 * 
 * @param <T>
 *            the particular type of entity the controller is managing
 */
public interface GenericEntityController {

	/**
	 * Get entities
	 * 
	 * @param userId
	 * @param offset
	 *            1-based pagination offset
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
	public <T extends Nodeable> PaginatedResults<T> getEntities(String userId,
			PaginatedParameters paging,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException,
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
	public <T extends Nodeable> T getEntity(String userId, String id,
			HttpServletRequest request, Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException;
	
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
	public <T extends Nodeable> T getEntity(UserInfo info, String id, HttpServletRequest request, Class<? extends T> clazz, EventType eventType) throws NotFoundException, DatastoreException, UnauthorizedException;
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
	public <T extends BaseChild> List<T> getEntityChildrenOfType(String userId, String parentId, Class<? extends T> clazz, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException;

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
	public <T extends BaseChild> PaginatedResults<T> getEntityChildrenOfTypePaginated(String userId, String parentId, Class<? extends T> clazz, PaginatedParameters paging, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException;

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
	public <T extends Nodeable> T createEntity(String userId, T newEntity,
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException;

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
	public <T extends Nodeable> T updateEntity(String userId,T updatedEntity, HttpServletRequest request)
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
	public <T extends BaseChild> Collection<T> aggregateEntityUpdate(String userId, String parentId, Collection<T> update,HttpServletRequest request) throws NotFoundException,
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
	public <T extends Nodeable> void deleteEntity(String userId, String id, Class<? extends T> clazz)
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
	 * Get the schema for an entity<p>
	 * <ul>
	 * <li> http://json-schema.org/ 
	 * <li> http://wiki.fasterxml.com/JacksonJsonSchemaGeneration
	 * </ul>
	 * @return the schema
	 * @throws DatastoreException 
	 */
	public <T extends Nodeable> JsonSchema getEntitySchema(Class<? extends T> clazz) throws DatastoreException;

	/**
	 * Get the schema for a paginated list of entities<p>
	 * <ul>
	 * <li> http://json-schema.org/ 
	 * <li> http://wiki.fasterxml.com/JacksonJsonSchemaGeneration
	 * </ul>
	 * @return the schema
	 * @throws DatastoreException 
	 */
	public <T extends Nodeable> JsonSchema getEntitiesSchema(Class<? extends T> clazz) throws DatastoreException;

	public Annotations getEntityAnnotations(String userId, String id,
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

	public JsonSchema getEntityAnnotationsSchema() throws DatastoreException;
	
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
	 */
	public  <T extends Base> AccessControlList createEntityACL(String userId, AccessControlList newEntity,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException;

	
	/**
	 * Get the ACL for a given entity
	 * @param nodeId
	 * @param userId
	 * @param clazz the class of the entity who ACL this is
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException 
	 */
	public  <T extends Base>  AccessControlList getEntityACL(String entityId, String userId, HttpServletRequest request, Class<? extends T> clazz) 
		throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Update an entity ACL.
	 * @param userId
	 * @param updated
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 */
	public AccessControlList updateEntityACL(String userId, AccessControlList updated) throws 
		DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException;
	

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id the id of the node whose inheritance is to be restored
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public  void deleteEntityACL(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * @return the JSON schema for an access control list
	 */
	public <T extends Base> JsonSchema getAclSchema() throws DatastoreException;
	
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
	 */
	public <T extends Nodeable> QueryResults executeQueryWithAnnotations(String userId, BasicQuery query, Class<? extends T> clazz, HttpServletRequest request) throws DatastoreException, NotFoundException;
}