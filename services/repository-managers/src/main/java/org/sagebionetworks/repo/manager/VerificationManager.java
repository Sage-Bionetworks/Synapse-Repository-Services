package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public interface VerificationManager {
	
	VerificationSubmission createVerificationSubmission(UserInfo userInfo, VerificationSubmission verificationSubmission);
	
	/*
	 * List the verification submissions, optionally filtering by 
	 * the current state of the verification submissions and/or the 
	 * user who is the subject of the verification submission.
	 */
	VerificationPagedResults listVerificationSubmissions(
			UserInfo userInfo, List<VerificationStateEnum> currentVerificationState, Long verifiedUserId, long limit, long offset);
	
	void changeSubmissionState(UserInfo userInfo, long verificationSubmissionId, VerificationState newState);
	
	String getDownloadURL(UserInfo userInfo, long verificationSubmissionId, long fileHandleId);

}
