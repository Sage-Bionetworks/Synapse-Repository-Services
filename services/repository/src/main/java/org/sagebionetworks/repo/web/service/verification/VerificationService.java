package org.sagebionetworks.repo.web.service.verification;

import java.util.List;

import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public interface VerificationService {
	
	VerificationSubmission createVerificationSubmission(Long userId, VerificationSubmission verificationSubmission, String notificationUnsubscribeEndpoint);
	
	void deleteVerificationSubmission(Long userId, Long verificationId);
	
	VerificationPagedResults listVerificationSubmissions(
			Long userId, List<VerificationStateEnum> currentVerificationState, Long verifiedUserId, long limit, long offset);
	
	void changeSubmissionState(Long userId, long verificationSubmissionId, VerificationState newState, String notificationUnsubscribeEndpoint);
}
