package org.sagebionetworks.repo.manager.verification;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public class VerificationHelper {
	private static VerificationState getLatestState(VerificationSubmission verificationSubmission) {
		if (verificationSubmission==null) return null;
		List<VerificationState> list = verificationSubmission.getStateHistory();
		if (list==null || list.isEmpty()) return null;
		return list.get(list.size()-1);
	}
	
	public static boolean isVerified(VerificationSubmission verificationSubmission) {
		VerificationState currentState = getLatestState(verificationSubmission);
		if (currentState==null) return false;
		return currentState.getState()==VerificationStateEnum.APPROVED;
	}
	
	public static Date getApprovalDate(VerificationSubmission verificationSubmission) {
		VerificationState currentState = getLatestState(verificationSubmission);
		if (currentState==null) return null;
		if (currentState.getState()!=VerificationStateEnum.APPROVED) return null;
		return currentState.getCreatedOn();
		
	}
}
