package org.sagebionetworks.repo.manager;

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
		if (verificationSubmission==null) return false;
		VerificationState currentState = getLatestState(verificationSubmission);
		if (currentState==null) return false;
		return currentState.getState()==VerificationStateEnum.APPROVED;
	}
	
	public static Date getApprovalDate(VerificationSubmission verificationSubmission) {
		if (verificationSubmission==null) return null;
		VerificationState currentState = getLatestState(verificationSubmission);
		if (currentState==null) return null;
		return currentState.getCreatedOn();
		
	}
}
