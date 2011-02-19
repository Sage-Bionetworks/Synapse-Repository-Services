package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller interface for all operations common to entities.
 * 
 * @author deflaux
 * 
 * @param <T>
 *            the particular type of entity the controller is managing
 */
public interface EntityController<T> {

	/**
	 * Get entities
	 * 
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
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "", method = RequestMethod.GET)
	public @ResponseBody
	abstract PaginatedResults<T> getEntities(@RequestParam(value="userId", required=false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException;

	/**
	 * Get a specific entity
	 * <p>
	 * 
	 * @param id
	 *            the unique identifier for the entity to be returned
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the entity or exception if not found
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public @ResponseBody
	abstract T getEntity(@PathVariable String id,  @RequestParam(value="userId", required=false) String userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Create a new entity
	 * <p>
	 * 
	 * @param newEntity
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the newly created entity
	 * @throws InvalidModelException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "", method = RequestMethod.POST)
	public @ResponseBody
	abstract T createEntity(@RequestBody T newEntity,  @RequestParam(value="userId", required=false) String userId, HttpServletRequest request)
			throws DatastoreException, InvalidModelException, UnauthorizedException ;

	/**
	 * Update an existing entity
	 * <p>
	 * 
	 * @param id
	 *            the unique identifier for the entity to be updated
	 * @param etag
	 *            service-generated value used to detect conflicting updates
	 * @param updatedEntity
	 *            the object with which to overwrite the currently stored entity
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the updated entity
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	abstract T updateEntity(@PathVariable String id,
			@RequestParam(value="userId", required=false) String userId, 
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody T updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException ;

	/**
	 * Delete a specific entity
	 * <p>
	 * 
	 * @param id
	 *            the unique identifier for the entity to be deleted
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public abstract void deleteEntity(@PathVariable String id,  @RequestParam(value="userId", required=false) String userId)
			throws NotFoundException, DatastoreException, UnauthorizedException ;

	/**
	 * Set the Base DAO for this controller to use
	 * 
	 * @param dao
	 */
	public abstract void setDao(BaseDAO<T> dao);

}