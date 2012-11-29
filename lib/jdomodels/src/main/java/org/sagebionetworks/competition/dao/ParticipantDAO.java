package org.sagebionetworks.competition.dao;

import java.util.List;

import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDAO {

	/**
	 * Create a new Participant
	 * 
	 * @param dto
	 * @throws DatastoreException
	 */
	public void create(Participant dto) throws DatastoreException;

	/**
	 * Get a Participant by UserID and CompetitionId
	 * 
	 * @param userId
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Participant get(String userId, String compId)
			throws DatastoreException, NotFoundException;
	
	/**
	 * Get all Participants, in a given range
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Participant> getInRange(long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * Get all Participants for a given Competition
	 * 
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	List<Participant> getAllByCompetition(String compId)
	throws DatastoreException, NotFoundException;

	/**
	 * Get the total number of Participants for a given Competition
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCountByCompetition(String compId) throws DatastoreException, NotFoundException;

	/**
	 * Delete a Participant
	 * 
	 * @param userId
	 * @param compId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String userId, String compId) throws DatastoreException,
			NotFoundException;

}