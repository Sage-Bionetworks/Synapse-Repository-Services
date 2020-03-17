package org.sagebionetworks.repo.manager.verification;

import java.util.List;

import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public interface VerificationManager {
	
	/**
	 * 
	 * @param userInfo
	 * @param verificationSubmission
	 * @return
	 */
	VerificationSubmission createVerificationSubmission(UserInfo userInfo, VerificationSubmission verificationSubmission);
	
	/**
	 * delete a verification submission.  Must be the creator to delete
	 * @param userInfo
	 * @param verificationId
	 */
	void deleteVerificationSubmission(UserInfo userInfo, Long verificationId);
	
	
	/**
	 * List the verification submissions, optionally filtering by 
	 * the current state of the verification submissions and/or the 
	 * user who is the subject of the verification submission.
	 * 
	 * @param userInfo
	 * @param currentVerificationState
	 * @param verifiedUserId
	 * @param limit
	 * @param offset
	 * @return
	 */
	VerificationPagedResults listVerificationSubmissions(
			UserInfo userInfo, List<VerificationStateEnum> currentVerificationState, Long verifiedUserId, long limit, long offset);
	
	/**
	 * 
	 * @param userInfo
	 * @param verificationSubmissionId
	 * @param newState
	 */
	void changeSubmissionState(UserInfo userInfo, long verificationSubmissionId, VerificationState newState);
	
	/**
	 * Create a message to the ACT members telling them there is a new VerificationSubmission
	 * waiting for review.
	 * 
	 * @param verificationSubmission
	 * @param notificationUnsubscribeEndpoint
	 * @return
	 */
	List<MessageToUserAndBody> createSubmissionNotification(VerificationSubmission verificationSubmission, String notificationUnsubscribeEndpoint);
	
	/**
	 * Create a message to the user requesting verification that the ACT has acted, accepting, rejecting, or
	 * suspending the verification.
	 * 
	 * @param verificationSubmissionId
	 * @param newState
	 * @param notificationUnsubscribeEndpoint
	 * @return
	 */
	List<MessageToUserAndBody> createStateChangeNotification(long verificationSubmissionId, VerificationState newState, String notificationUnsubscribeEndpoint);
}
