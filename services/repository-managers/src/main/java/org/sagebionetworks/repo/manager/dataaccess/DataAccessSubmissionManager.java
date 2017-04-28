package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
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
	public ACTAccessRequirementStatus create(UserInfo userInfo, String requestId, String etag);

	/**
	 * Cancel a submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @return
	 */
	public ACTAccessRequirementStatus cancel(UserInfo userInfo, String submissionId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public DataAccessSubmission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request);

	/**
	 * List a page of submissions for a given access requirement.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public DataAccessSubmissionPage listSubmission(UserInfo userInfo, DataAccessSubmissionPageRequest request);

	/**
	 * Retrieve the status of a user meeting an access requirement
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 */
	public AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String accessRequirementId);

	/**
	 * Retrieve information about submitted DataAccessSubmissions.
	 * 
	 * @param userInfo
	 * @param nextPageToken
	 * @return
	 */
	public OpenSubmissionPage getOpenSubmissions(UserInfo userInfo, String nextPageToken);

}
