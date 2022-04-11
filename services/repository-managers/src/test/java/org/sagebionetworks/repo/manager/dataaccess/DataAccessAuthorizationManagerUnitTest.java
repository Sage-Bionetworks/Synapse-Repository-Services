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
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

@ExtendWith(MockitoExtension.class)
public class DataAccessAuthorizationManagerUnitTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
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
		
		doReturn(AuthorizationStatus.authorized()).when(managerSpy).canReviewSubmissions(any(), any());
		
		// Call under test
		AuthorizationStatus result = managerSpy.checkDownloadAccessForAccessRequirement(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verify(managerSpy).canReviewSubmissions(user, accessRequirementId);
	}
	
	@Test
	public void testCheckDownloadAccessForAccessRequirementWithNonReviewer() {
		String accessRequirementId = "123";
		
		doReturn(AuthorizationStatus.accessDenied("Nope")).when(managerSpy).canReviewSubmissions(any(), any());
		
		// Call under test
		AuthorizationStatus result = managerSpy.checkDownloadAccessForAccessRequirement(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.accessDenied("The user does not have download access."), result);
		
		verify(managerSpy).canReviewSubmissions(user, accessRequirementId);
	}
	
	@Test
	public void testCanDownloadRequestFiles() {
		
		String requestId = "456";
		String accessRequirementId = "123";
		
		doReturn(AuthorizationStatus.authorized()).when(managerSpy).checkDownloadAccessForAccessRequirement(any(), any());
		when(mockRequestDao.get(any())).thenReturn(new Request().setAccessRequirementId(accessRequirementId));
		
		// Call under test
		managerSpy.canDownloadRequestFiles(user, requestId);
		
		verify(mockRequestDao).get(requestId);
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
		when(mockSubmissionDao.getSubmission(any())).thenReturn(new Submission().setAccessRequirementId(accessRequirementId));
		
		// Call under test
		managerSpy.canDownloadSubmissionFiles(user, submissionId);
		
		verify(mockSubmissionDao).getSubmission(submissionId);
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
	public void testCanReviewSubmissionsWithACTMember() {
		
		user.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.ACCESS_AND_COMPLIANCE_GROUP.getPrincipalId()));
		
		String accessRequirementId = "123";
		
		// Call under test
		AuthorizationStatus result = manager.canReviewSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verifyZeroInteractions(mockUserProfileManager);
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testCanReviewSubmissionsWithAdmin() {
		
		user = new UserInfo(true);
		
		String accessRequirementId = "123";
		
		// Call under test
		AuthorizationStatus result = manager.canReviewSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verifyZeroInteractions(mockUserProfileManager);
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testCanReviewSubmissions() {
		
		String accessRequirementId = "123";
		VerificationSubmission verificationSubmission = new VerificationSubmission()
				.setStateHistory(Collections.singletonList(new VerificationState().setState(VerificationStateEnum.APPROVED)));

		when(mockUserProfileManager.getCurrentVerificationSubmission(anyLong())).thenReturn(verificationSubmission);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		AuthorizationStatus result = manager.canReviewSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.authorized(), result);
		
		verify(mockUserProfileManager).getCurrentVerificationSubmission(user.getId());
		verify(mockAclDao).canAccess(user, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS);
	}
	

	@Test
	public void testCanReviewSubmissionsWithNoApprovedUserValidation() {
		
		String accessRequirementId = "123";
		
		for (VerificationStateEnum state : VerificationStateEnum.values()) {
			
			if (state == VerificationStateEnum.APPROVED) {
				// Tested above
				continue;
			}
			
			Mockito.reset(mockUserProfileManager, mockAclDao);
			
			VerificationSubmission verificationSubmission = new VerificationSubmission()
					.setStateHistory(Collections.singletonList(new VerificationState().setState(state)));

			when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
			when(mockUserProfileManager.getCurrentVerificationSubmission(anyLong())).thenReturn(verificationSubmission);
			
			// Call under test
			AuthorizationStatus result = manager.canReviewSubmissions(user, accessRequirementId);
			
			assertEquals(AuthorizationStatus.accessDenied("The user must be validated in order to review data access submissions."), result);
			
			verify(mockAclDao).canAccess(user, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS);
			verify(mockUserProfileManager).getCurrentVerificationSubmission(user.getId());
			
		}
	}
	
	@Test
	public void testCanReviewSubmissionsWithNoReviewerPermission() {
		
		String accessRequirementId = "123";
		
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("Nope"));
		
		// Call under test
		AuthorizationStatus result = manager.canReviewSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.accessDenied("The user does not have permissions to review data access submissions for access requirement 123."), result);
		
		verifyZeroInteractions(mockUserProfileManager);
		verify(mockAclDao).canAccess(user, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS);
	}
	
	@Test
	public void testCanReviewSubmissionsWithNoUserValidation() {
		
		String accessRequirementId = "123";
		VerificationSubmission verificationSubmission = null;

		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockUserProfileManager.getCurrentVerificationSubmission(anyLong())).thenReturn(verificationSubmission);
		
		// Call under test
		AuthorizationStatus result = manager.canReviewSubmissions(user, accessRequirementId);
		
		assertEquals(AuthorizationStatus.accessDenied("The user must be validated in order to review data access submissions."), result);
		
		verify(mockUserProfileManager).getCurrentVerificationSubmission(user.getId());
		verify(mockAclDao).canAccess(user, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS);
	}
	
}
