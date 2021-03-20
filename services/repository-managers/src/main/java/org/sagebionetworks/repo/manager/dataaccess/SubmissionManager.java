package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;

public interface SubmissionManager {

	/**
	 * Create a submission
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public SubmissionStatus create(UserInfo userInfo, CreateSubmissionRequest request);

	/**
	 * Cancel a submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @return
	 */
	public SubmissionStatus cancel(UserInfo userInfo, String submissionId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public Submission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request);

	/**
	 * List a page of submissions for a given access requirement.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public SubmissionPage listSubmission(UserInfo userInfo, SubmissionPageRequest request);

	/**
	 * Delete a submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 */
	public void deleteSubmission(UserInfo userInfo, String submissionId);

	/**
	 * List the submission info for approved submissions.
	 * 
	 * @param request
	 * @return
	 */
	public SubmissionInfoPage listInfoForApprovedSubmissions(SubmissionInfoPageRequest request);

	/**
	 * Retrieve the status of a user meeting an access requirement
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 */
	public AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String accessRequirementId);

	/**
	 * Retrieve information about submitted Submissions.
	 * 
	 * @param userInfo
	 * @param nextPageToken
	 * @return
	 */
	public OpenSubmissionPage getOpenSubmissions(UserInfo userInfo, String nextPageToken);

}
