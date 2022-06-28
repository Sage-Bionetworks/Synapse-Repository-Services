package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_SETTINGS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.REVIEW_SUBMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.message.PrincipalNameProvider;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.MessageToUser;

@ExtendWith(MockitoExtension.class)
public class DataAccessSubmissionNotificationManagerImplTest {

	@Mock
	private AccessControlListDAO mockAclDao;
	@Mock
	private TemplatedMessageSender mockTemplatedMessageSender;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private SubmissionDAO mockSubmissionDao;
	@Mock
	private PrincipalNameProvider mockUserNameProvider;
	
	@Spy
	@InjectMocks
	private DataAccessSubmissionNotificationManagerImpl managerSpy;

	@Test
	public void testSendNotificationToReviewers() {
		String dataAccessSubmissionId = "111";
		String accessRequirmentId = "222";
		Long[] users = new Long[]{1L,2L,3L,4L};

		Submission submission = new Submission().setId(dataAccessSubmissionId).setState(SubmissionState.SUBMITTED)
				.setAccessRequirementId(accessRequirmentId).setSubmittedBy(users[0].toString());

		when(mockSubmissionDao.getSubmission(any())).thenReturn(submission);
		
		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(Set.of(
				createResourceAccess(users[1], REVIEW_SUBMISSIONS, CHANGE_SETTINGS),
				createResourceAccess(TeamConstants.ACT_TEAM_ID, REVIEW_SUBMISSIONS),
				createResourceAccess(users[2], UPDATE),
				createResourceAccess(users[3], REVIEW_SUBMISSIONS)
		));
		when(mockAclDao.getAcl(any(), any())).thenReturn(Optional.of(acl));
		
		doNothing().when(managerSpy).sendNotificationMessageToReviewer(any(), any(), any());
		
		// call under test
		managerSpy.sendNotificationToReviewers(dataAccessSubmissionId);
		
		verify(mockSubmissionDao).getSubmission(dataAccessSubmissionId);
		verify(mockAclDao).getAcl(accessRequirmentId, ObjectType.ACCESS_REQUIREMENT);
		
		verify(managerSpy, times(2)).sendNotificationMessageToReviewer(any(), any(), any());
		verify(managerSpy).sendNotificationMessageToReviewer(users[1], dataAccessSubmissionId, users[0]);
		verify(managerSpy).sendNotificationMessageToReviewer(users[3], dataAccessSubmissionId, users[0]);	
	}
	
	@Test
	public void testSendNotificationToReviewersWithNullId() {
		String dataAccessSubmissionId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.sendNotificationToReviewers(dataAccessSubmissionId);
		});
	}
	
	@Test
	public void testSendNotificationToReviewersWithStateApproved() {
		String dataAccessSubmissionId = "111";
		String accessRequirmentId = "222";
		Long[] users = new Long[]{1L,2L,3L,4L};

		Submission submission = new Submission().setId(dataAccessSubmissionId).setState(SubmissionState.APPROVED)
				.setAccessRequirementId(accessRequirmentId).setSubmittedBy(users[0].toString());

		when(mockSubmissionDao.getSubmission(any())).thenReturn(submission);
		
		// call under test
		managerSpy.sendNotificationToReviewers(dataAccessSubmissionId);
		
		verifyZeroInteractions(mockAclDao);
		verify(mockSubmissionDao).getSubmission(dataAccessSubmissionId);
		verify(managerSpy, never()).sendNotificationMessageToReviewer(any(), any(), any());
	}
	
	@Test
	public void testSendNotificationToReviewersWithNoAcl() {
		String dataAccessSubmissionId = "111";
		String accessRequirmentId = "222";
		Long[] users = new Long[]{1L,2L,3L,4L};

		Submission submission = new Submission().setId(dataAccessSubmissionId).setState(SubmissionState.SUBMITTED)
				.setAccessRequirementId(accessRequirmentId).setSubmittedBy(users[0].toString());

		when(mockSubmissionDao.getSubmission(any())).thenReturn(submission);			
		when(mockAclDao.getAcl(any(), any())).thenReturn(Optional.empty());

		// call under test
		managerSpy.sendNotificationToReviewers(dataAccessSubmissionId);
		
		verify(mockSubmissionDao).getSubmission(dataAccessSubmissionId);
		verify(mockAclDao).getAcl(accessRequirmentId, ObjectType.ACCESS_REQUIREMENT);
		verify(managerSpy, never()).sendNotificationMessageToReviewer(any(), any(), any());	
	}
	
	@Test
	public void testSendNotificationToReviewer() {
		String dataAccessSubmissionId = "111";
		Long reviewer = 1L;
		Long submittedBy = 2L;
		UserInfo messageSender = new UserInfo(false, 3L);
		String reviewerName = "reviewer";
		String submittedByName = "submitter";
		
		when(mockUserManager.getUserInfo(any())).thenReturn(messageSender);
		when(mockTemplatedMessageSender.sendMessage(any())).thenReturn(new MessageToUser().setId("222"));
		
		when(mockUserNameProvider.getPrincipalName(reviewer)).thenReturn(reviewerName);
		when(mockUserNameProvider.getPrincipalName(submittedBy)).thenReturn(submittedByName);
		
		// call under test
		managerSpy.sendNotificationMessageToReviewer(reviewer, dataAccessSubmissionId, submittedBy);
		
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId());
		
		Map<String, Object> expectedContext = Map.of(
				"reviewerName", reviewerName,
				"submittedByName", submittedByName,
				"dataAccessSubmissionId", dataAccessSubmissionId
		);
		MessageTemplate expected = MessageTemplate.builder().withContext(expectedContext)
				.withMessageBodyMimeType("text/html").withRecipients(Set.of(reviewer.toString()))
				.withSender(messageSender).withSubject("[Time Sensitive] Request for access to data")
				.withTemplateCharSet(StandardCharsets.UTF_8)
				.withTemplateFile("message/DataAccessSubmissionNotificationTemplate.html.vtl").build();

		verify(mockTemplatedMessageSender).sendMessage(expected);
		verify(mockUserNameProvider).getPrincipalName(reviewer);
		verify(mockUserNameProvider).getPrincipalName(submittedBy);

	}
}
