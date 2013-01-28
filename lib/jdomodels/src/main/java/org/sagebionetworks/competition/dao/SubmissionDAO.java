package org.sagebionetworks.competition.dao;

import java.util.List;

import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionDAO {

	/**
	 * Create a new Submission
	 * 
	 * @param dto
	 * @throws DatastoreException
	 * @return the ID of the newly-created object
	 */
	public String create(Submission dto) throws DatastoreException;

	/**
	 * Get a Submission by ID
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Submission get(String id) throws DatastoreException,
			NotFoundException;

	/**
	 * Get all of the Submissions by a given User (may span multiple Competitions)
	 * 
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByUser(String userId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get all of the Submissions for a given Competition
	 * 
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByCompetition(String compId)
			throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total number of Submissions for a given Competition
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCountByCompetition(String compId) throws DatastoreException, 
			NotFoundException;

	/**
	 * Get the total number of Submissions in Synapse
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCount() throws DatastoreException, NotFoundException;

	/**
	 * Delete a Submission
	 * 
	 * @param id
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	/**
	 * Get all Submissions from a given Competition with a certain status. (e.g.
	 * get all UNSCORED submissions from Competition x).
	 * 
	 * @param compId
	 * @param status
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByCompetitionAndStatus(String compId,
			SubmissionStatusEnum status) throws DatastoreException,
			NotFoundException;

	/**
	 * Get all Submissions from a given user for a given competition.
	 * 
	 * @param userId
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	List<Submission> getAllByCompetitionAndUser(String compId, String principalId)
			throws DatastoreException, NotFoundException;

}