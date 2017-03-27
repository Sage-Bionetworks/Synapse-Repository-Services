package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;

public interface DataAccessSubmissionDAO {

	/**
	 * Retrieve a submission status that user owns or is an accessor.
	 * 
	 * @param accessRequirementId
	 * @param userId
	 * @return
	 */
	public ACTAccessRequirementStatus getStatusByRequirementIdAndPrincipalId(String accessRequirementId, String userId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param submissionId
	 * @param newState
	 * @param reason
	 * @param userId
	 * @return
	 */
	public DataAccessSubmission updateSubmissionStatus(String submissionId,
			DataAccessSubmissionState newState, String reason, String userId,
			Long timestamp, String etag);

	/**
	 * Create a submission.
	 * 
	 * @param submissionToCreate
	 * @return
	 */
	public ACTAccessRequirementStatus create(DataAccessSubmission submissionToCreate);

	/**
	 * Cancel a submission.
	 * 
	 * @param submissionId
	 * @param userId
	 * @param timestamp
	 * @param etag
	 * @return
	 */
	public ACTAccessRequirementStatus cancel(String submissionId, String userId,
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

	/**
	 * Retrieving a submission given its ID
	 * 
	 * @param submissionId
	 * @return
	 */
	public DataAccessSubmission getSubmission(String submissionId);

	/**
	 * use for test
	 * 
	 * @param id
	 */
	public void delete(String id);

	/**
	 * Retrieve a page of submissions
	 * 
	 * @param accessRequirementId
	 * @param filterBy
	 * @param orderBy
	 * @param isAscending
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<DataAccessSubmission> getSubmissions(String accessRequirementId, DataAccessSubmissionState filterBy,
			DataAccessSubmissionOrder orderBy, Boolean isAscending, long limit, long offset);
}
