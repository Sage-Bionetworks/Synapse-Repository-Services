package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DependentPropertyDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Controller interface for all operations common to dependent entities.
 * 
 * @author deflaux
 * 
 * @param <T>
 *            the dependent DTO
 * @param <S>
 *            the parent DTO
 */
public interface DependentEntityController<T, S> {

	/**
	 * Get a specific dependent entity
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
	public abstract T getDependentEntity(String userId, String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	/**
	 * Update an existing dependent entity
	 * <p>
	 * 
	 * @param userId
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
	 * @throws UnauthorizedException
	 */
	public abstract T updateDependentEntity(String userId, String id,
			Integer etag, T updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException;

	/**
	 * Set the Dependent Property DAO for this controller to use
	 * 
	 * @param dao
	 */
	public abstract void setDao(DependentPropertyDAO<T, S> dao);

}