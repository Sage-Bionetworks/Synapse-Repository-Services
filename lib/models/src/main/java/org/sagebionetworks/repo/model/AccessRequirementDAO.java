package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;

public interface AccessRequirementDAO {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(AccessRequirement dto) throws DatastoreException, InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessRequirement get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param nodeId
	 * @return the AccessRequirement objects related to this node
	 * @throws DatastoreException 
	 */
	public Collection<AccessRequirement> getForNode(String nodeId) throws DatastoreException;
	

	/**
	 * updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public AccessRequirement update(AccessRequirement dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * gets the parameters of the given Access RequirementsObject
	 * @param id the ID of the AccessRequirements object
	 * @param paramsDto
	 * @param schema
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void getAccessRequirementParameters(String id, Object paramsDto, ObjectSchema schema)  throws DatastoreException, NotFoundException;


	/**
	 * Set the parameters field of the AccessRequirements.
	 * @param id
	 * @param etag
	 * @param paramsDto
	 * @param schema
	 * @return the updated etag
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public String setAccessRequirementParameters(String id, String etag, Object paramsDto, ObjectSchema schema) throws DatastoreException, InvalidModelException,
	NotFoundException, ConflictingUpdateException;
}
