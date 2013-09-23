package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

public interface TeamDAO {
	/**
	 * @param dto object to be created
	 * @return the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public Team create(Team dto) throws DatastoreException, InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Team get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the Teams in the system
	 * 
	 * @param offset
	 * @param limit
	 * 
	 */
	public List<Team> getInRange(long offset, long limit) throws DatastoreException;

	/**
	 * Get the Teams a member belongs to
	 * @param princialId the team member
	 * @param offset
	 * @param limit
	 * @return the Teams this principal belongs to
	 * @throws DatastoreException 
	 */
	public List<Team> getForMemberInRange(String principalId, long offset, long limit) throws DatastoreException;
	
	/**
	 * Updates the 'shallow' properties of an object.
	 *
	 * @param dto
	 * @throws DatastoreException
	 */
	public Team update(Team team) throws InvalidModelException,
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

}
