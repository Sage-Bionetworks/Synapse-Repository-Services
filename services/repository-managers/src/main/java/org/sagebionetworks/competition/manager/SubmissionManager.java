package org.sagebionetworks.competition.manager;

import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionManager {

	/**
	 * Get a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Submission getSubmission(String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the SubmissionStatus object for a Submission.
	 * 
	 * @param submissionId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public SubmissionStatus getSubmissionStatus(String submissionId)
			throws DatastoreException, NotFoundException;

	
	/**
	 * Create a Submission.
	 * 
	 * @param userInfo
	 * @param submission
	 * @return
	 * @throws NotFoundException
	 */
	public Submission createSubmission(UserInfo userInfo, Submission submission)
			throws NotFoundException;

	/**
	 * Update the SubmissionStatus object for a Submission. Note that the
	 * requesting user must be an admin of the Competition for which this
	 * Submission was created.
	 * 
	 * @param userInfo
	 * @param submissionStatus
	 * @return
	 * @throws NotFoundException
	 */
	public SubmissionStatus updateSubmissionStatus(UserInfo userInfo,
			SubmissionStatus submissionStatus) throws NotFoundException;

	/**
	 * Delete a Submission. Note that the requesting user must be an admin
	 * of the Competition for which this Submission was created.
	 * 
	 * Use of this method is discouraged, since Submissions should be immutable.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Deprecated
	public void deleteSubmission(UserInfo userInfo, String submissionId)
			throws DatastoreException, NotFoundException;

	/**
	 * Get all Submissions for a given Competition. This method requires admin
	 * rights.
	 * 
	 * If a SubmissionStatusEnum is provided, results will be filtered
	 * accordingly.
	 * 
	 * @param userInfo
	 * @param compId
	 * @param status
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public QueryResults<Submission> getAllSubmissions(UserInfo userInfo, String compId,
			SubmissionStatusEnum status, long limit, long offset) 
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get all Submissions by a given Synapse user.
	 * 
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public QueryResults<Submission> getAllSubmissionsByUser(String userId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * Get all Submissions by a given Synapse user to a given Competition.
	 * 
	 * @param compId
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	QueryResults<Submission> getAllSubmissionsByCompetitionAndUser(UserInfo userInfo,
			String compId, long limit, long offset)
			throws DatastoreException, NotFoundException;

	/**
	 * Get the number of Submissions to a given Competition.
	 * 
	 * @param compId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public long getSubmissionCount(String compId) throws DatastoreException,
			NotFoundException;

}