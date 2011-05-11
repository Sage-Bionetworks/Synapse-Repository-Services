package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;

/**
 * Controller interface for all operations common to annotate-able entities.
 * 
 * @author deflaux
 * 
 * @param <T>
 *            the particular type of entity whose annotations the controller is
 *            managing
 */
public interface AnnotationsController<T extends Base> {

	/**
	 * Get annotations for a specific entity
	 * <p>
	 * 
	 * @param userId
	 * @param id
	 *            the unique identifier for the entity's annotations to be
	 *            returned
	 * @param request
	 *            used to get the servlet URL prefix
	 * @return the entity's annotations or exception if not found
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public abstract Annotations getEntityAnnotations(String userId, String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	/**
	 * Update existing annotations for an entity
	 * <p>
	 * 
	 * @param userId
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
	 * @throws UnauthorizedException
	 * @throws InvalidModelException 
	 */
	abstract Annotations updateEntityAnnotations(String userId, String id,
			Integer etag, Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException, InvalidModelException;

	/**
	 * Get the schema for an entity's annotations<p>
	 * <ul>
	 * <li> http://json-schema.org/ 
	 * <li> http://wiki.fasterxml.com/JacksonJsonSchemaGeneration
	 * </ul>
	 * @return the schema
	 * @throws DatastoreException 
	 */
	public abstract JsonSchema getEntityAnnotationsSchema() throws DatastoreException;

//	/**
//	 * Set the DAO for this controller to use
//	 * 
//	 * @param dao
//	 */
//	public abstract void setDao(BaseDAO<T> dao);
}