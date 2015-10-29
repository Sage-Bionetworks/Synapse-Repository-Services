package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.VerificationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationServiceImpl implements VerificationService {
	
	@Autowired
	private VerificationManager verificationManager;

	@Autowired
	private UserManager userManager;

	public VerificationServiceImpl() {}

	// for testing
	public VerificationServiceImpl(VerificationManager verificationManager, UserManager userManager) {
		this.verificationManager=verificationManager;
		this.userManager = userManager;
	}

	@Override
	public VerificationSubmission createVerificationSubmission(Long userId,
			VerificationSubmission verificationSubmission) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		VerificationSubmission result = verificationManager.createVerificationSubmission(userInfo, verificationSubmission);
		// TODO notify ACT
		return result;
	}

	@Override
	public VerificationPagedResults listVerificationSubmissions(Long userId,
			List<VerificationStateEnum> currentVerificationState,
			Long verifiedUserId, long limit, long offset) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return verificationManager.listVerificationSubmissions(userInfo, currentVerificationState, verifiedUserId, limit, offset);
	}

	@Override
	public void changeSubmissionState(Long userId,
			long verificationSubmissionId, VerificationState newState) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		verificationManager.changeSubmissionState(userInfo, verificationSubmissionId, newState);
		// TODO notify user who is the subject of the verification submission, including the 'reason'
	}

}
