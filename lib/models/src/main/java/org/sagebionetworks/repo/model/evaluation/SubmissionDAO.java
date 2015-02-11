package org.sagebionetworks.repo.model.evaluation;

import java.util.List;

import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionDAO {

	/**
	 * Create a new Submission
	 * 
	 * @param dto
	 * @return ID of the created Submission
	 */
	public String create(Submission dto);


	void addSubmissionContributor(String submissionId, SubmissionContributor dto);

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
	 * Get all of the Submissions by a given User (may span multiple Evaluations)
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
	 * Get all of the Submissions for a given Evaluation
	 * 
	 * @param evalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByEvaluation(String evalId, long limit, long offset)
			throws DatastoreException, NotFoundException;
	
	/**
	 * Get the total number of Submissions for a given Evaluation
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getCountByEvaluation(String evalId) throws DatastoreException, 
			NotFoundException;

	/**
	 * Get all Submissions from a given Evaluation with a certain status. (e.g.
	 * get all UNSCORED submissions from Evaluation x).
	 * 
	 * @param evalId
	 * @param status
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByEvaluationAndStatus(String evalId,
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, NotFoundException;

	public long getCountByEvaluationAndStatus(String evalId,
	SubmissionStatusEnum status) throws DatastoreException,
	NotFoundException;

	/**
	 * Get all Submissions from a given user for a given Evaluation.
	 * 
	 * @param evalId
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<Submission> getAllByEvaluationAndUser(String evalId, String principalId,
			long limit, long offset)
			throws DatastoreException, NotFoundException;

	public long getCountByEvaluationAndUser(String evalId, String userId)
			throws DatastoreException, NotFoundException;

}