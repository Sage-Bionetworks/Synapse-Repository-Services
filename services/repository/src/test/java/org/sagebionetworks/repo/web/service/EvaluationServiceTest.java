package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.model.EvaluationRoundListRequest;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.query.QueryDAO;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceTest {

	@Mock
	private ServiceProvider mockServiceProvider;
	@Mock
	private EntityBundleService mockEntityBundleService;
	@Mock
	private EvaluationManager mockEvaluationManager;
	@Mock
	private SubmissionManager mockSubmissionManager;
	@Mock
	private EvaluationPermissionsManager mockEvaluationPermissionsManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private QueryDAO mockQueryDAO;
	@Mock
	private NotificationManager mockNotificationManager;
	@InjectMocks
	private EvaluationServiceImpl evaluationService;

	UserInfo userInfo;
	Long userId;
	private String evalId;
	private long limit;
	private long offset;

	@Test
	public void testCreateSubmission() throws Exception {
		userId = 111L;
		evalId = "evalId";
		limit = 11;
		offset = 0;
		String challengeEndpoint = "challengeEndpoint:";
		String notificationUnsubscribeEndpoint = "notificationUnsubscribeEndpoint:";
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton("222"));
		String content = "foo";
		List<MessageToUserAndBody> result = Collections.singletonList(new MessageToUserAndBody(mtu, content, "text/plain"));
		Submission submission = new Submission();
		when(mockSubmissionManager.createSubmission(eq(userInfo), eq(submission), anyString(), 
				anyString(), isNull())).thenReturn(submission);
		when(mockSubmissionManager.createSubmissionNotifications(
				eq(userInfo), eq(submission), anyString(), 
				eq(challengeEndpoint), eq(notificationUnsubscribeEndpoint))).thenReturn(result);

		when(mockServiceProvider.getEntityBundleService()).thenReturn(mockEntityBundleService);
		evaluationService.createSubmission(userId, submission, "123", "987", challengeEndpoint,
				notificationUnsubscribeEndpoint);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockSubmissionManager).createSubmission(eq(userInfo), eq(submission), eq("123"), eq("987"), 
				isNull());
		verify(mockSubmissionManager).createSubmissionNotifications(
				eq(userInfo), any(Submission.class), eq("987"),
				eq(challengeEndpoint), eq(notificationUnsubscribeEndpoint));
		
		ArgumentCaptor<List> mtuArg = ArgumentCaptor.forClass(List.class);
		verify(mockNotificationManager).sendNotifications(eq(userInfo), mtuArg.capture());
		assertEquals(result, mtuArg.getValue());		
	}
	
	@Test
	public void testGetEvaluationsInRange() {
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		
		evaluationService.getEvaluations(userId, accessType, false, null, limit, offset);
		
		verify(mockEvaluationManager).getEvaluations(userInfo, accessType, false, null, limit, offset);
	}

	@Test
	public void testGetEvaluationsInRangeActiveOnly() {
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		
		evaluationService.getEvaluations(userId, accessType, true, null, limit, offset);
		
		verify(mockEvaluationManager).getEvaluations(userInfo, accessType, true, null, limit, offset);
	}

	@Test
	public void testGetEvaluationByContentSource() {
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		
		evaluationService.getEvaluationByContentSource(userId, "syn123", accessType, false, null, limit, offset);
		
		verify(mockEvaluationManager).getEvaluationsByContentSource(userInfo, "syn123", accessType, false, null, limit, offset);
	}

	@Test
	public void testGetEvaluationByContentSourceActiveOnly() {
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		
		evaluationService.getEvaluationByContentSource(userId, "syn123", accessType, true, null, limit, offset);
		
		verify(mockEvaluationManager).getEvaluationsByContentSource(userInfo, "syn123", accessType, true, null, limit, offset);
	}

	@Test
	public void testGetAllSubmissions() {
		List<Submission> expectedRes = new LinkedList<Submission>();
		when(mockSubmissionManager.getAllSubmissions(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getAllSubmissions(userId, evalId, SubmissionStatusEnum.OPEN, limit, offset);
		verify(mockSubmissionManager).getAllSubmissions(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset);
	}

	@Test
	public void testGetAllSubmissionBundles() {
		List<SubmissionBundle> expectedRes = new LinkedList<SubmissionBundle>();
		when(mockSubmissionManager.getAllSubmissionBundles(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getAllSubmissionBundles(userId, evalId, SubmissionStatusEnum.OPEN, limit, offset);
		verify(mockSubmissionManager).getAllSubmissionBundles(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset);
	}

	@Test
	public void testGetAllSubmissionStatuses() {
		List<SubmissionStatus> expectedRes = new LinkedList<SubmissionStatus>();
		when(mockSubmissionManager.getAllSubmissionStatuses(userInfo, evalId, SubmissionStatusEnum.OPEN, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getAllSubmissionStatuses(userId, evalId, SubmissionStatusEnum.OPEN, limit, offset);
		verify(mockSubmissionManager).getAllSubmissionStatuses(userInfo, evalId, SubmissionStatusEnum.OPEN, limit, offset);
	}

	@Test
	public void testgetMyOwnSubmissionsByEvaluation() {
		List<Submission> expectedRes = new LinkedList<Submission>();
		when(mockSubmissionManager.getMyOwnSubmissionsByEvaluation(userInfo, evalId, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getMyOwnSubmissionsByEvaluation(evalId, userId, limit, offset);
		verify(mockSubmissionManager).getMyOwnSubmissionsByEvaluation(userInfo, evalId, limit, offset);
	}

	@Test
	public void testgetMyOwnSubmissionsBundlesByEvaluation() {
		List<SubmissionBundle> expectedRes = new LinkedList<SubmissionBundle>();
		when(mockSubmissionManager.getMyOwnSubmissionBundlesByEvaluation(userInfo, evalId, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getMyOwnSubmissionBundlesByEvaluation(evalId, userId, limit, offset);
		verify(mockSubmissionManager).getMyOwnSubmissionBundlesByEvaluation(userInfo, evalId, limit, offset);
	}
}
