package org.sagebionetworks.repo.web.service.verification;

import java.util.List;

import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.verification.VerificationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerificationServiceImpl implements VerificationService {
	
	private VerificationManager verificationManager;

	private UserManager userManager;
	
	private NotificationManager notificationManager;

	@Autowired
	public VerificationServiceImpl(VerificationManager verificationManager, UserManager userManager,
			NotificationManager notificationManager) {
		this.verificationManager = verificationManager;
		this.userManager = userManager;
		this.notificationManager = notificationManager;
	}

	@Override
	public VerificationSubmission createVerificationSubmission(Long userId,
			VerificationSubmission verificationSubmission,
			String notificationUnsubscribeEndpoint) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		VerificationSubmission result = verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
		
		List<MessageToUserAndBody> createNotifications = verificationManager.
				createSubmissionNotification(result, notificationUnsubscribeEndpoint);
		
		notificationManager.sendNotifications(userInfo, createNotifications);
		
		return result;
	}
	
	@Override
	public void deleteVerificationSubmission(Long userId, Long verificationId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		verificationManager.deleteVerificationSubmission(userInfo, verificationId);
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
			long verificationSubmissionId, VerificationState newState,
			String notificationUnsubscribeEndpoint) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		verificationManager.changeSubmissionState(userInfo, verificationSubmissionId, newState);
		List<MessageToUserAndBody> createNotifications = verificationManager.createStateChangeNotification(verificationSubmissionId, newState, notificationUnsubscribeEndpoint);
		notificationManager.sendNotifications(userInfo, createNotifications);
	}

}
