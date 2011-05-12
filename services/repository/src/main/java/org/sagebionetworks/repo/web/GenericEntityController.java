package org.sagebionetworks.repo.web;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;

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
	public <T extends Base> PaginatedResults<T> getEntities(String userId,
			Integer offset, Integer limit, String sort, Boolean ascending,
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
	public <T extends Base> T getEntity(String userId, String id,
			HttpServletRequest request, Class<? extends T> clazz) throws NotFoundException,
			DatastoreException, UnauthorizedException;
	
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
	public <T extends Base> T createEntity(String userId, T newEntity,
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
	public <T extends Base> T updateEntity(String userId, String id,
			T updatedEntity, HttpServletRequest request)
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
	public  void deleteEntity(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the schema for an entity<p>
	 * <ul>
	 * <li> http://json-schema.org/ 
	 * <li> http://wiki.fasterxml.com/JacksonJsonSchemaGeneration
	 * </ul>
	 * @return the schema
	 * @throws DatastoreException 
	 */
	public <T extends Base> JsonSchema getEntitySchema(Class<? extends T> clazz) throws DatastoreException;

	/**
	 * Get the schema for a paginated list of entities<p>
	 * <ul>
	 * <li> http://json-schema.org/ 
	 * <li> http://wiki.fasterxml.com/JacksonJsonSchemaGeneration
	 * </ul>
	 * @return the schema
	 * @throws DatastoreException 
	 */
	public <T extends Base> JsonSchema getEntitiesSchema(Class<? extends T> clazz) throws DatastoreException;

	public Annotations getEntityAnnotations(String userId, String id,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException;

	public Annotations updateEntityAnnotations(String userId, String entityId,
			Annotations updatedAnnotations, HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	public JsonSchema getEntityAnnotationsSchema() throws DatastoreException;
	

}