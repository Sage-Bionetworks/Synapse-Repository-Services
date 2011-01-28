package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller interface for all operations common to annotate-able entities.
 * 
 * @author deflaux
 * 
 * @param <T>
 */
public interface AbstractAnnotatableEntityController<T extends Base> {

	/**
	 * Get annotations for a specific entity
	 * <p>
	 * <ul>
	 * <li>TODO response bread crumb urls when we have proper DTOs
	 * </ul>
	 * 
	 * @param id
	 *            the unique identifier for the entity's annotations to be
	 *            returned
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the entity's annotations or exception if not found
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}/annotations", method = RequestMethod.GET)
	public @ResponseBody
	abstract Annotations getEntityAnnotations(@PathVariable String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException;

	/**
	 * Update existing annotations for an entity
	 * <p>
	 * <ul>
	 * <li>TODO validate updated entity
	 * </ul>
	 * 
	 * @param id
	 *            the id of the entity whose annotations we will update
	 * @param etag
	 *            service-generated value used to detect conflicting updates
	 * @param updatedAnnotations
	 *            the object with which to overwrite the currently stored entity
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the updated entity
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}/annotations", method = RequestMethod.PUT)
	public @ResponseBody
	abstract Annotations updateEntityAnnotations(@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException;

	/**
	 * Set the DAO for this controller to use
	 * 
	 * @param dao
	 */
	public abstract void setDao(BaseDAO<T> dao);
}