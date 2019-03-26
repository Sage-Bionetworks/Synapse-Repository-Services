package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.manager.SubmissionManager;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.query.QueryDAO;

@RunWith(MockitoJUnitRunner.class)
public class EvaluationServiceTest {


	private EvaluationServiceImpl evaluationService;

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

	UserInfo userInfo;
	Long userId;
	private String evalId;
	private long limit;
	private long offset;


	@Before
	public void before() throws Exception {
		this.evaluationService = new EvaluationServiceImpl(
				mockServiceProvider,
				mockEvaluationManager,
				mockSubmissionManager,
				mockEvaluationPermissionsManager,
				mockUserManager,
				mockQueryDAO,
				mockNotificationManager);
	}

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
		evaluationService.createSubmission(userId, submission, "123", "987", null,
				challengeEndpoint, notificationUnsubscribeEndpoint);
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
	public void testGetAllSubmissions() {
		List<Submission> expectedRes = new LinkedList<Submission>();
		when(mockSubmissionManager.getAllSubmissions(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getAllSubmissions(userId, evalId, SubmissionStatusEnum.OPEN, limit, offset, null);
		verify(mockSubmissionManager).getAllSubmissions(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset);
	}

	@Test
	public void testGetAllSubmissionBundles() {
		List<SubmissionBundle> expectedRes = new LinkedList<SubmissionBundle>();
		when(mockSubmissionManager.getAllSubmissionBundles(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getAllSubmissionBundles(userId, evalId, SubmissionStatusEnum.OPEN, limit, offset, null);
		verify(mockSubmissionManager).getAllSubmissionBundles(userInfo, evalId,  SubmissionStatusEnum.OPEN, limit, offset);
	}

	@Test
	public void testGetAllSubmissionStatuses() {
		List<SubmissionStatus> expectedRes = new LinkedList<SubmissionStatus>();
		when(mockSubmissionManager.getAllSubmissionStatuses(userInfo, evalId, SubmissionStatusEnum.OPEN, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getAllSubmissionStatuses(userId, evalId, SubmissionStatusEnum.OPEN, limit, offset, null);
		verify(mockSubmissionManager).getAllSubmissionStatuses(userInfo, evalId, SubmissionStatusEnum.OPEN, limit, offset);
	}

	@Test
	public void testgetMyOwnSubmissionsByEvaluation() {
		List<Submission> expectedRes = new LinkedList<Submission>();
		when(mockSubmissionManager.getMyOwnSubmissionsByEvaluation(userInfo, evalId, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getMyOwnSubmissionsByEvaluation(evalId, userId, limit, offset, null);
		verify(mockSubmissionManager).getMyOwnSubmissionsByEvaluation(userInfo, evalId, limit, offset);
	}

	@Test
	public void testgetMyOwnSubmissionsBundlesByEvaluation() {
		List<SubmissionBundle> expectedRes = new LinkedList<SubmissionBundle>();
		when(mockSubmissionManager.getMyOwnSubmissionBundlesByEvaluation(userInfo, evalId, limit, offset)).thenReturn(expectedRes);
		// Call under test
		evaluationService.getMyOwnSubmissionBundlesByEvaluation(evalId, userId, limit, offset, null);
		verify(mockSubmissionManager).getMyOwnSubmissionBundlesByEvaluation(userInfo, evalId, limit, offset);
	}

}
