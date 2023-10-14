package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSearchSort;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessRequirementDAO {
	
	public static final Long INVALID_ANNOTATIONS_LOCK_ID = 1l;

	/**
	 * @param dto
	 *            object to be created
	 * @param paramsSchema the schema of the parameters field
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	<T extends AccessRequirement> T create(T dto) throws DatastoreException, InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AccessRequirement get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Updates the 'shallow' properties of an object.
	 *
	 * @param dto
	 * @throws DatastoreException
	 */
	<T extends AccessRequirement> T update(T accessRequirement) throws InvalidModelException,
			NotFoundException, ConflictingUpdateException, DatastoreException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	void delete(String id) throws DatastoreException, NotFoundException;

	/**
	 * Retrieve a page of AccessRequirements.
	 * 
	 * @param subject the subject of the access restriction
	 * @param limit
	 * @param offset
	 * @return the AccessRequirement objects related to this node
	 * @throws DatastoreException 
	 */
	List<AccessRequirement> getAccessRequirementsForSubject(
			List<Long> subjectIds, RestrictableObjectType type, long limit,
			long offset) throws DatastoreException;

	/**
	 * Retrieve the concreteType of an access requirement.
	 * 
	 * @param accessRequirementId
	 * @return
	 */
	String getConcreteType(String accessRequirementId);

	/**
	 * Retrieve the statistic of access requirements for list of given subjectIds
	 * 
	 * @param subjectIds
	 * @param type - if type is ENTITY, subjectIds should contain the entityID and its ancestor IDs;
	 * if type is TEAM, subjectIds should contain the teamID
	 * @return
	 */
	AccessRequirementStats getAccessRequirementStats(List<Long> subjectIds, RestrictableObjectType type);

	/**
	 * Retrieve information to update an AccessRequirement.
	 * 
	 * @param accessRequirementId
	 * @return
	 * @throws NotFoundException
	 */
	AccessRequirementInfoForUpdate getForUpdate(String accessRequirementId) throws NotFoundException;

	/**
	 * Returns all access requirement IDs that applies to source subjects but does not apply to destination subjects.
	 * 
	 * @param sourceSubjects
	 * @param destSubjects
	 * @param type
	 * @return
	 */
	List<String> getAccessRequirementDiff(List<Long> sourceSubjects, List<Long> destSubjects,
			RestrictableObjectType type);

	/**
	 * Retrieve an AccessRequirement for update
	 * 
	 * @param accessRequirementId
	 * @return
	 */
	AccessRequirement getAccessRequirementForUpdate(String accessRequirementId);

	/**
	 * Retrieve a page of subjects that the given accessRequirementId applies to
	 * 
	 * @param accessRequirementId
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<RestrictableObjectDescriptor> getSubjects(long accessRequirementId, long limit, long offset);
	
	/**
	 * Fetch the names for the given list of access requirements
	 *  
	 * @param accessRequirementIds
	 * @return
	 */
	Map<Long, String> getAccessRequirementNames(Set<Long> accessRequirementIds);
	
	// For testing
	
	void truncateAll();

	/**
	 * Map the given access requirement IDs to the provided project ID.
	 * @param arIds
	 * @param projectId
	 */
	void mapAccessRequirmentsToProject(Long[] arIds, Long projectId);
	
	/**
	 * Returns a map of the projects ids mapped to each of the ARs in the given set
	 * 
	 * @param arIds
	 * @return
	 */
	Map<Long, List<Long>> getAccessRequirementProjectsMap(Set<Long> arIds);

	/**
	 * Fetch a page of access requirements that mett the given criteria
	 * 
	 * @param sort Sort specification, required
	 * @param nameContains Optional filter for a name substring
	 * @param reviewerId Optional filter for an assigned reviewer
	 * @param projectId Optional filter for AR project
	 * @param accessType Optional filter for the type of access
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<AccessRequirement> searchAccessRequirements(List<AccessRequirementSearchSort> sort, String nameContains, String reviewerId,
			Long projectId, ACCESS_TYPE accessType, long limit, long offset);
	
	
	/**
	 * Get the dynamically bound access requirement IDs for the given subject.
	 * 
	 * @param subject
	 * @return
	 */
	List<Long> getDynamicallyBoundAccessRequirementIdsForSubject(RestrictableObjectDescriptor subject);
	
	/**
	 * Add the dynamically bound access requirements to the provided subject.
	 * Note: The caller is expected to only add new binding with this call, so it will fail if a binding already exists.
	 * 
	 * @param subject
	 * @param arIds
	 */
	void addDynamicallyBoundAccessRequirmentsToSubject(RestrictableObjectDescriptor subject, List<Long> arIds);
	
	/**
	 * Remove the dynamically bound access requirements from the provided subject.
	 * @param subject
	 * @param arIds
	 */
	void removeDynamicallyBoundAccessRequirementsFromSubject(RestrictableObjectDescriptor subject, List<Long> arIds);

	/**
	 * Bootstrap any access requirements.
	 */
	void bootstrap();
		
	
}
