package org.sagebionetworks.repo.model.evaluation;

import java.util.List;

import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDAO {

	/**
	 * Create a new Participant
	 * 
	 * @param dto
	 * @ return the ID
	 * @throws DatastoreException
	 */
	public long create(Participant dto) throws DatastoreException;

	/**
	 * Get a Participant by UserID and EvaluationId
	 * 
	 * @param userId
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Participant get(String userId, String evalId)
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
	 * Get all Participants for a given Evaluation
	 * 
	 * @param evalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	List<Participant> getAllByEvaluation(String evalId, long limit, long offset)
	throws DatastoreException, NotFoundException;

	/**
	 * Get the total number of Participants for a given Evaluation
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCountByEvaluation(String evalId) throws DatastoreException, NotFoundException;

	/**
	 * Delete a Participant
	 * 
	 * @param userId
	 * @param evalId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String userId, String evalId) throws DatastoreException,
			NotFoundException;

}