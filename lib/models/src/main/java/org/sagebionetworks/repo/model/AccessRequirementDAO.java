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
	public List<AccessRequirement> getAllAccessRequirementsForSubject(List<String> subjectIds, RestrictableObjectType type) throws DatastoreException;
	
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
	List<Long> getAllUnmetAccessRequirements(List<String> subjectIds, RestrictableObjectType type, Collection<Long> principalIds,
			Collection<ACCESS_TYPE> accessTypes) throws DatastoreException;

	long getCount() throws DatastoreException;

	/**
	 * Retrieve a page of AccessRequirements.
	 * 
	 * @param subject the subject of the access restriction
	 * @param limit
	 * @param offset
	 * @return the AccessRequirement objects related to this node
	 * @throws DatastoreException 
	 */
	public List<AccessRequirement> getAccessRequirementsForSubject(
			List<String> subjectIds, RestrictableObjectType type, Long limit,
			Long offset) throws DatastoreException;

	/**
	 * Retrieve the concreteType of an access requirement.
	 * 
	 * @param accessRequirementId
	 * @return
	 */
	public String getConcreteType(String accessRequirementId);

	/**
	 * Retrieve the statistic of access requirements for list of given subjectIds
	 * 
	 * @param subjectIds
	 * @param type - if type is ENTITY, subjectIds should contain the entityID and its ancestor IDs;
	 * if type is TEAM, subjectIds should contain the teamID
	 * @return
	 */
	public AccessRequirementStats getAccessRequirementStats(List<String> subjectIds, RestrictableObjectType type);

	/**
	 * Retrieving the subjects under a given access requirement
	 * 
	 * @param accessRequirementId
	 * @return
	 */
	public List<RestrictableObjectDescriptor> getSubjects(Long accessRequirementId);
}
