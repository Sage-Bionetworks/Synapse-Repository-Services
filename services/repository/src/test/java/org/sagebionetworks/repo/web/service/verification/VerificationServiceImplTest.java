package org.sagebionetworks.repo.web.service.verification;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.verification.VerificationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.service.verification.VerificationServiceImpl;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceImplTest {
	@Mock
	private VerificationManager mockVerificationManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private NotificationManager mockNotificationManager;
	@InjectMocks
	private VerificationServiceImpl verificationService;

	private UserInfo userInfo; 
		
	private static final Long USER_ID = 111L;
	private static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT = "notificationUnsubscribeEndpoint:";

	@BeforeEach
	public void before() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);		
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(userInfo);
	}

	@Test
	public void testCreateVerificationSubmission() {
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		when(mockVerificationManager.createVerificationSubmission(userInfo, verificationSubmission)).
				thenReturn(verificationSubmission);
		MessageToUserAndBody mtub = new MessageToUserAndBody();
		List<MessageToUserAndBody> mtubList = Collections.singletonList(mtub);
		when(mockVerificationManager.createSubmissionNotification(verificationSubmission, 
				NOTIFICATION_UNSUBSCRIBE_ENDPOINT)).thenReturn(mtubList);
		
		VerificationSubmission created = verificationService.createVerificationSubmission(
				USER_ID, verificationSubmission,
			 NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		verify(mockVerificationManager).createVerificationSubmission(userInfo, verificationSubmission);
		verify(mockVerificationManager).createSubmissionNotification(verificationSubmission, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		verify(mockNotificationManager).sendNotifications(userInfo, mtubList);
		
		assertEquals(created, verificationSubmission);
	}
	
	@Test
	public void testDeleteVerificationSubmission() {
		long verificationId = 101L;
		verificationService.deleteVerificationSubmission(USER_ID, verificationId);
		verify(mockVerificationManager).deleteVerificationSubmission(userInfo, verificationId);
	}

	@Test
	public void testListVerificationSubmission() {
		List<VerificationStateEnum> states = Collections.singletonList(VerificationStateEnum.APPROVED);
		verificationService.listVerificationSubmissions(USER_ID, states, USER_ID, 10L, 0L);
		verify(mockVerificationManager).listVerificationSubmissions(userInfo, states, USER_ID, 10L, 0L);
	}
	
	@Test
	public void testChangeSubmissionState() {
		long verificationId = 101L;
		
		VerificationState newState = new VerificationState();
		MessageToUserAndBody mtub = new MessageToUserAndBody();
		List<MessageToUserAndBody> mtubList = Collections.singletonList(mtub);
		when(mockVerificationManager.createStateChangeNotification(verificationId, newState,
				NOTIFICATION_UNSUBSCRIBE_ENDPOINT)).thenReturn(mtubList);

		verificationService.changeSubmissionState(
				USER_ID, verificationId, newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		verify(mockVerificationManager).changeSubmissionState(userInfo, verificationId, newState);
		verify(mockVerificationManager).createStateChangeNotification(verificationId, newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		verify(mockNotificationManager).sendNotifications(userInfo, mtubList);

	}

}
