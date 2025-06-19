package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.UserSubmissionSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.UserSubmissionSearchResponse;

public interface SubmissionManager {

	/**
	 * Create a submission
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	SubmissionStatus create(UserInfo userInfo, CreateSubmissionRequest request);

	/**
	 * Cancel a submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @return
	 */
	SubmissionStatus cancel(UserInfo userInfo, String submissionId);

	/**
	 * Update the state of a submission.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	Submission updateStatus(UserInfo userInfo, SubmissionStateChangeRequest request);

	/**
	 * List a page of submissions for a given access requirement.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	SubmissionPage listSubmission(UserInfo userInfo, SubmissionPageRequest request);

	/**
	 * Delete a submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 */
	void deleteSubmission(UserInfo userInfo, String submissionId);

	/**
	 * List the submission info for approved submissions.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	SubmissionInfoPage listInfoForApprovedSubmissions(UserInfo user, SubmissionInfoPageRequest request);

	/**
	 * Retrieve the status of a user meeting an access requirement
	 * 
	 * @param userInfo
	 * @param accessRequirementId
	 * @return
	 */
	AccessRequirementStatus getAccessRequirementStatus(UserInfo userInfo, String accessRequirementId);

	/**
	 * Retrieve information about submitted Submissions.
	 * 
	 * @param userInfo
	 * @param nextPageToken
	 * @return
	 */
	OpenSubmissionPage getOpenSubmissions(UserInfo userInfo, String nextPageToken);
	
	/**
	 * Search through submissions visible to the user, if the user is part of ACT they can see any submission. Otherwise only those submissions 
	 * whose user is a reviewer for their access requirement will be returned.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	SubmissionSearchResponse searchSubmissions(UserInfo userInfo, SubmissionSearchRequest request);

	/**
	 * Fetch a submission by its id, if the user is not part of ACT they must be validated and assigned as reviewers of the AR submissions in order to fetch the submission.
	 * 
	 * @param userInfo
	 * @param submissionId
	 * @return
	 */
	Submission getSubmission(UserInfo userInfo, String submissionId);

	/**
	 * Fetch their own access approval information specific to a submission, as long as the user is an accessor in the submission.
	 *
	 * @param userInfo
	 * @param submissionId
	 * @return
	 */
	AccessApproval getUserAccessApproval(UserInfo userInfo, String submissionId);

	/**
	 * List a page of submissions with access approval.
	 *
	 * @param userInfo
	 * @param request
	 * @return
	 */
	 UserSubmissionSearchResponse searchUserSubmissions(UserInfo userInfo, UserSubmissionSearchRequest request);

}
