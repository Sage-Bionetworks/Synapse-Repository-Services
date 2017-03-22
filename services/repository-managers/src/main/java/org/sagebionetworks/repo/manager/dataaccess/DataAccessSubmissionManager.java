package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;

public interface DataAccessSubmissionManager {

	/**
	 * Create a submission
	 * 
	 * @param userInfo
	 * @param requestId
	 * @param etag 
	 * @return
	 */
	public DataAccessSubmissionStatus create(UserInfo userInfo, String requestId, String etag);

	/**
	 * Retrieve a submission status that the user owns or is an accessor.
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 */
	public DataAccessSubmissionStatus getSubmissionStatus(UserInfo userInfo, String accessRequirementId);

	/**
	 * Cancel a submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @return
	 */
	public DataAccessSubmissionStatus cancel(UserInfo userInfo, String submissionId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public DataAccessSubmission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request);

}
