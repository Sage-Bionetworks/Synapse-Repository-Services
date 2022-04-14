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
	SubmissionStatus getStatusByRequirementIdAndPrincipalId(String accessRequirementId, String userId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param submissionId
	 * @param newState
	 * @param reason
	 * @param userId
	 * @return
	 */
	Submission updateSubmissionStatus(String submissionId,
			SubmissionState newState, String reason, String userId, Long timestamp);

	/**
	 * Create a submission.
	 * 
	 * @param submissionToCreate
	 * @return
	 */
	SubmissionStatus createSubmission(Submission submissionToCreate);

	/**
	 * Cancel a submission.
	 * 
	 * @param submissionId
	 * @param userId
	 * @param timestamp
	 * @param etag
	 * @return
	 */
	SubmissionStatus cancel(String submissionId, String userId,
			Long timestamp, String etag);

	/**
	 * @param userId
	 * @param accessRequirementId
	 * @return true if the user has a submission with the given state for the given accessRequirementId,
	 * false otherwise.
	 */
	boolean hasSubmissionWithState(String userId, String accessRequirementId, SubmissionState state);

	/**
	 * Retrieve a submission
	 * 
	 * @param submissionId
	 * @return
	 */
	Submission getForUpdate(String submissionId);

	/**
	 * Retrieving a submission given its ID
	 * 
	 * @param submissionId
	 * @return
	 */
	Submission getSubmission(String submissionId);

	/**
	 * use for test
	 * 
	 * @param id
	 */
	void delete(String id);

	/**
	 * Retrieve a page of submissions
	 * 
	 * @param accessRequirementId
	 * @param stateFilter
	 * @param accessorId
	 * @param orderBy
	 * @param isAscending
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Submission> getSubmissions(String accessRequirementId, SubmissionState stateFilter, String accessorId,
			SubmissionOrder orderBy, Boolean isAscending, long limit, long offset);

	/**
	 * 
	 * @param accessRequirementId
	 * @param limit
	 * @param offset
	 * @param includeAccessorChanges
	 * @return
	 */
	List<SubmissionInfo> listInfoForApprovedSubmissions(String accessRequirementId, long limit, long offset, boolean includeAccessorChanges);
	
	/**
	 * Return true if userId is an accessor of submissionId
	 * 
	 * @param submissionId
	 * @param userId
	 * @return
	 */
	boolean isAccessor(String submissionId, String userId);

	/**
	 * Retrieve submitted Submission information
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<OpenSubmission> getOpenSubmissions(long limit, long offset);
	
	/**
	 * @param submissionId
	 * @return The id of the access requirement for the submission with the given id
	 */
	String getAccessRequirementId(String submissionId);
	
	// For testing
	void truncateAll();
}
