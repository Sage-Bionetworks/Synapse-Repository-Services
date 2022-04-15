package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

@ExtendWith(MockitoExtension.class)
public class DataAccessAuthorizationManagerUnitTest {
	
	@Mock
	private VerificationDAO mockVerificationDao;
	
	@Mock
	private AccessControlListDAO mockAclDao;
	
	@Mock
	private RequestDAO mockRequestDao;
	
	@Mock
	private SubmissionDAO mockSubmissionDao;
	
	@InjectMocks
	private DataAccessAuthorizationManagerImpl manager;
	
	private DataAccessAuthorizationManagerImpl managerSpy;
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
		user = new UserInfo(false, 123L);
		managerSpy = Mockito.spy(manager);
	}
	
	@Test
	public void testCheckDownloadAccessForAccessRequirement() {
		String accessRequirementId = "123";
		
		doReturn(AuthorizationStatus.authorized()).when(managerSpy).canReviewAccessRequirementSubmissions(any(), any());
		
		// Call under test
		AuthorizationStatus result = managerSpy.checkDownloadAccessForAccessRequirement(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verify(managerSpy).canReviewAccessRequirementSubmissions(user, accessRequirementId);
	}
	
	@Test
	public void testCheckDownloadAccessForAccessRequirementWithNonReviewer() {
		String accessRequirementId = "123";
		
		doReturn(AuthorizationStatus.accessDenied("Nope")).when(managerSpy).canReviewAccessRequirementSubmissions(any(), any());
		
		// Call under test
		AuthorizationStatus result = managerSpy.checkDownloadAccessForAccessRequirement(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.accessDenied("The user does not have download access."), result);
		
		verify(managerSpy).canReviewAccessRequirementSubmissions(user, accessRequirementId);
	}
	
	@Test
	public void testCanDownloadRequestFiles() {
		
		String requestId = "456";
		String accessRequirementId = "123";
		
		doReturn(AuthorizationStatus.authorized()).when(managerSpy).checkDownloadAccessForAccessRequirement(any(), any());
		when(mockRequestDao.getAccessRequirementId(any())).thenReturn(accessRequirementId);
		
		// Call under test
		managerSpy.canDownloadRequestFiles(user, requestId);
		
		verify(mockRequestDao).getAccessRequirementId(requestId);
		verify(managerSpy).checkDownloadAccessForAccessRequirement(user, accessRequirementId);

	}
	
	@Test
	public void testCanDownloadRequestFilesWithNoUser() {
		
		user = null;
		String requestId = "456";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			managerSpy.canDownloadRequestFiles(user, requestId);
		}).getMessage();
		
		assertEquals("userInfo is required.", message);
		
		verifyZeroInteractions(mockRequestDao);

	}
	
	@Test
	public void testCanDownloadRequestFilesWithNoRequestId() {
		
		String requestId = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			managerSpy.canDownloadRequestFiles(user, requestId);
		}).getMessage();
		
		assertEquals("requestId is required.", message);
		
		verifyZeroInteractions(mockRequestDao);

	}

	@Test
	public void testCanDownloadSubmissionFiles() {
		
		String submissionId = "456";
		String accessRequirementId = "123";
		
		doReturn(AuthorizationStatus.authorized()).when(managerSpy).checkDownloadAccessForAccessRequirement(any(), any());
		when(mockSubmissionDao.getAccessRequirementId(any())).thenReturn(accessRequirementId);
		
		// Call under test
		managerSpy.canDownloadSubmissionFiles(user, submissionId);
		
		verify(mockSubmissionDao).getAccessRequirementId(submissionId);
		verify(managerSpy).checkDownloadAccessForAccessRequirement(user, accessRequirementId);

	}
	
	@Test
	public void testCanDownloadSubmissionFilesWithNoUser() {
		
		user = null;
		String submissionId = "456";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			managerSpy.canDownloadSubmissionFiles(user, submissionId);
		}).getMessage();
		
		assertEquals("userInfo is required.", message);
		
		verifyZeroInteractions(mockRequestDao);

	}
	
	@Test
	public void testCanDownloadSubmissionFilesWithNoRequestId() {
		
		String submissionId = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			managerSpy.canDownloadSubmissionFiles(user, submissionId);
		}).getMessage();
		
		assertEquals("submissionId is required.", message);
		
		verifyZeroInteractions(mockRequestDao);

	}
	
	@Test
	public void testCanReviewAccessRequirementSubmissions() {
		
		String accessRequirementId = "123";
		VerificationSubmission verificationSubmission = new VerificationSubmission()
				.setStateHistory(Collections.singletonList(new VerificationState().setState(VerificationStateEnum.APPROVED)));

		when(mockVerificationDao.getCurrentVerificationSubmissionForUser(anyLong())).thenReturn(verificationSubmission);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		AuthorizationStatus result = manager.canReviewAccessRequirementSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verify(mockVerificationDao).getCurrentVerificationSubmissionForUser(user.getId());
		verify(mockAclDao).canAccess(user, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS);
	}

	@Test
	public void testCanReviewAccessRequirementSubmissionsWithNoApprovedUserValidation() {
		
		String accessRequirementId = "123";
		
		for (VerificationStateEnum state : VerificationStateEnum.values()) {
			
			if (state == VerificationStateEnum.APPROVED) {
				// Tested above
				continue;
			}
			
			Mockito.reset(mockVerificationDao, mockAclDao);
			
			VerificationSubmission verificationSubmission = getVerfificationSubmission(state);

			when(mockVerificationDao.getCurrentVerificationSubmissionForUser(anyLong())).thenReturn(verificationSubmission);
			
			// Call under test
			AuthorizationStatus result = manager.canReviewAccessRequirementSubmissions(user, accessRequirementId);
			
			assertEquals(AuthorizationStatus.accessDenied("The user must be validated in order to review data access submissions."), result);
			
			verify(mockVerificationDao).getCurrentVerificationSubmissionForUser(user.getId());
			verifyZeroInteractions(mockAclDao);
			
		}
	}
	
	@Test
	public void testCanReviewAccessRequirementSubmissionsWithACTMember() {
		
		user.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.ACCESS_AND_COMPLIANCE_GROUP.getPrincipalId()));
		
		String accessRequirementId = "123";
		
		// Call under test
		AuthorizationStatus result = manager.canReviewAccessRequirementSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verifyZeroInteractions(mockVerificationDao);
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testCanReviewAccessRequirementSubmissionsWithAdmin() {
		
		user = new UserInfo(true);
		
		String accessRequirementId = "123";
		
		// Call under test
		AuthorizationStatus result = manager.canReviewAccessRequirementSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verifyZeroInteractions(mockVerificationDao);
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testCanReviewAccessRequirementSubmissionsWithNoReviewerPermission() {
		
		String accessRequirementId = "123";
		
		VerificationSubmission verificationSubmission = getVerfificationSubmission(VerificationStateEnum.APPROVED);

		when(mockVerificationDao.getCurrentVerificationSubmissionForUser(anyLong())).thenReturn(verificationSubmission);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("Nope"));
		
		// Call under test
		AuthorizationStatus result = manager.canReviewAccessRequirementSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.accessDenied("The user does not have permissions to review data access submissions for access requirement 123."), result);
		
		verify(mockVerificationDao).getCurrentVerificationSubmissionForUser(user.getId());
		verify(mockAclDao).canAccess(user, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS);
	}
	
	@Test
	public void testCanReviewAccessRequirementSubmissionsWithNoUserValidation() {
		
		String accessRequirementId = "123";
		VerificationSubmission verificationSubmission = null;

		when(mockVerificationDao.getCurrentVerificationSubmissionForUser(anyLong())).thenReturn(verificationSubmission);
		
		// Call under test
		AuthorizationStatus result = manager.canReviewAccessRequirementSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.accessDenied("The user must be validated in order to review data access submissions."), result);
		
		verify(mockVerificationDao).getCurrentVerificationSubmissionForUser(user.getId());
		verifyZeroInteractions(mockAclDao);
	}
	
	private VerificationSubmission getVerfificationSubmission(VerificationStateEnum state) {
		return new VerificationSubmission().setStateHistory(Collections.singletonList(new VerificationState().setState(state)));
	}
}
