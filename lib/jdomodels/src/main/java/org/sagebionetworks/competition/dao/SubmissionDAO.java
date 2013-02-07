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
	 * Get all of the Submissions by a given User (may span multiple Competitions)
	 * 
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByUser(String userId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	public long getCountByUser(String userId) throws DatastoreException,
	NotFoundException;

	/**
	 * Get all of the Submissions for a given Competition
	 * 
	 * @param compId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByCompetition(String compId, long limit, long offset)
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
	 * Get all Submissions from a given Competition with a certain status. (e.g.
	 * get all UNSCORED submissions from Competition x).
	 * 
	 * @param compId
	 * @param status
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByCompetitionAndStatus(String compId,
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, NotFoundException;

	public long getCountByCompetitionAndStatus(String compId,
	SubmissionStatusEnum status) throws DatastoreException,
	NotFoundException;

	/**
	 * Get all Submissions from a given user for a given competition.
	 * 
	 * @param compId
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByCompetitionAndUser(String compId, String principalId,
			long limit, long offset)
			throws DatastoreException, NotFoundException;

	public long getCountByCompetitionAndUser(String compId, String userId)
			throws DatastoreException, NotFoundException;

}