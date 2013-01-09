package org.sagebionetworks.competition.dao;

import java.util.List;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableDAO;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CompetitionDAO extends MigratableDAO {	
	
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
	 * @param limit
	 * @param offset
	 * @param sort
	 * @param ascending
	 * @param schema
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Competition> getInRange(long limit, long offset) throws DatastoreException, NotFoundException;

	/**
	 * Get all Competitions, in a given range, filtered by CompetitionStatus.
	 * 
	 * @param limit
	 * @param offset
	 * @param sort
	 * @param ascending
	 * @param schema
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Competition> getInRange(long limit, long offset, CompetitionStatus status) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total count of Competitions in the system
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException;

	/**
	 * Updates a Competition. Note that this operation requires a current eTag,
	 * which will be updated upon completion.
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
	 * Updates a Competiton from backup. The passed eTag is persisted.
	 * 
	 * @param dto
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	public void updateFromBackup(Competition dto) throws DatastoreException,
				InvalidModelException, NotFoundException, ConflictingUpdateException;

	/**
	 * Delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

}
