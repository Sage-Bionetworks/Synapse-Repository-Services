package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfo;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;

public interface SubmissionDAO {

	/**
	 * Retrieve a submission status that user owns or is an accessor.
	 * 
	 * @param accessRequirementId
	 * @param userId
	 * @return
	 */
	public SubmissionStatus getStatusByRequirementIdAndPrincipalId(String accessRequirementId, String userId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param submissionId
	 * @param newState
	 * @param reason
	 * @param userId
	 * @return
	 */
	public Submission updateSubmissionStatus(String submissionId,
			SubmissionState newState, String reason, String userId, Long timestamp);

	/**
	 * Create a submission.
	 * 
	 * @param submissionToCreate
	 * @return
	 */
	public SubmissionStatus createSubmission(Submission submissionToCreate);

	/**
	 * Cancel a submission.
	 * 
	 * @param submissionId
	 * @param userId
	 * @param timestamp
	 * @param etag
	 * @return
	 */
	public SubmissionStatus cancel(String submissionId, String userId,
			Long timestamp, String etag);

	/**
	 * @param userId
	 * @param accessRequirementId
	 * @return true if the user has a submission with the given state for the given accessRequirementId,
	 * false otherwise.
	 */
	public boolean hasSubmissionWithState(String userId, String accessRequirementId, SubmissionState state);

	/**
	 * Retrieve a submission
	 * 
	 * @param submissionId
	 * @return
	 */
	public Submission getForUpdate(String submissionId);

	/**
	 * Retrieving a submission given its ID
	 * 
	 * @param submissionId
	 * @return
	 */
	public Submission getSubmission(String submissionId);

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
	public List<Submission> getSubmissions(String accessRequirementId, SubmissionState filterBy,
			SubmissionOrder orderBy, Boolean isAscending, long limit, long offset);

	/**
	 * 
	 * @param accessRequirementId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<SubmissionInfo> listInfoForApprovedSubmissions(String accessRequirementId, long limit, long offset);
	
	/**
	 * Return true if userId is an accessor of submissionId
	 * 
	 * @param submissionId
	 * @param userId
	 * @return
	 */
	public boolean isAccessor(String submissionId, String userId);

	/**
	 * Retrieve submitted Submission information
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<OpenSubmission> getOpenSubmissions(long limit, long offset);
	
	// For testing
	void truncateAll();
}
