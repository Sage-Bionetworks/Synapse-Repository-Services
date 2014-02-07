package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementDAO {

	/**
	 * @param dto
	 *            object to be created
	 * @param paramsSchema the schema of the parameters field
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public <T extends AccessRequirement> T create(T dto) throws DatastoreException, InvalidModelException;

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
	 * @param subject the subject of the access restriction
	 * @return the AccessRequirement objects related to this node
	 * @throws DatastoreException 
	 */
	public List<AccessRequirement> getForSubject(List<String> subjectIds, RestrictableObjectType type) throws DatastoreException;
	
	/**
	 * Updates the 'shallow' properties of an object.
	 *
	 * @param dto
	 * @throws DatastoreException
	 */
	public <T extends AccessRequirement> T update(T accessRequirement) throws InvalidModelException,
			NotFoundException, ConflictingUpdateException, DatastoreException;

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
	 * @return all IDs in the system
	 */
	List<String> getIds();

	/**
	 * 
	 * @param subject the subject of the access restriction
	 * @param principalIds the principalIds (user and groups) to which a user belongs
	 * @param accessType
	 * @return the AccessRequirement IDs for the given node and given access type which are unmet for ANY of the given principals
	 * @throws DatastoreException
	 */
	List<Long> unmetAccessRequirements(List<String> subjectIds, RestrictableObjectType type, Collection<Long> principalIds,
			Collection<ACCESS_TYPE> accessTypes) throws DatastoreException;

	long getCount() throws DatastoreException;
}
