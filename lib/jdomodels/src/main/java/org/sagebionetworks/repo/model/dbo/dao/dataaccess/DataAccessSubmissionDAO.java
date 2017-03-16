package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;

public interface DataAccessSubmissionDAO {

	/**
	 * Retrieve a submission status that user owns or is an accessor.
	 * 
	 * @param accessRequirementId
	 * @param userId
	 * @return
	 */
	public DataAccessSubmissionStatus getStatus(String accessRequirementId, String userId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param submissionId
	 * @param newState
	 * @param reason
	 * @param userId
	 * @return
	 */
	public DataAccessSubmission updateStatus(String submissionId,
			DataAccessSubmissionState newState, String reason, String userId,
			Long timestamp, String etag);

	/**
	 * Create a submission.
	 * 
	 * @param submissionToCreate
	 * @return
	 */
	public DataAccessSubmissionStatus create(DataAccessSubmission submissionToCreate);

	/**
	 * Cancel a submission.
	 * 
	 * @param submissionId
	 * @param userId
	 * @param timestamp
	 * @param etag
	 * @return
	 */
	public DataAccessSubmissionStatus cancel(String submissionId, String userId,
			Long timestamp, String etag);

	/**
	 * @param userId
	 * @param accessRequirementId
	 * @return true if the user has a submission with the given state for the given accessRequirementId,
	 * false otherwise.
	 */
	public boolean hasSubmissionWithState(String userId, String accessRequirementId, DataAccessSubmissionState state);

	/**
	 * Retrieve a submission
	 * 
	 * @param submissionId
	 * @return
	 */
	public DataAccessSubmission getForUpdate(String submissionId);
}
