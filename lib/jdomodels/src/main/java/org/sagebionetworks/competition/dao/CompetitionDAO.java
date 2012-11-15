package org.sagebionetworks.competition.dao;

import java.util.List;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CompetitionDAO {	
	
	/**
	 * Lookup a Competition ID by name. Returns null if the name is not in use.
	 * 
	 * @param name
	 * @return
	 * @throws DatastoreException 
	 */
	public String lookupByName(String name) throws DatastoreException;

	/**
	 * Create a new Competition
	 * 
	 * @param dto     object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(Competition dto, String ownerId) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieve the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Competition get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all Competitions, in a given range. Note that Competitions of all
	 * states are returned.
	 * 
	 * @param startIncl
	 * @param endExcl
	 * @param sort
	 * @param ascending
	 * @param schema
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Competition> getInRange(long startIncl, long endExcl) throws DatastoreException, NotFoundException;

	/**
	 * Get all Competitions, in a given range, filtered by CompetitionStatus.
	 * 
	 * @param startIncl
	 * @param endExcl
	 * @param sort
	 * @param ascending
	 * @param schema
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Competition> getInRange(long startIncl, long endExcl, CompetitionStatus status) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total count of Competitions in the system
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Updates the 'shallow' properties of an object.
	 *
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(Competition dto) throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException;

	/**
	 * Delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	/**
	 * Check whether there exists a Competition with the given ID
	 * 
	 * @param id
	 * @return
	 */
	boolean doesIdExist(String id);
}
