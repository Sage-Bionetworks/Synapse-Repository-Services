package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.ACTApprovalStatus;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.TermsOfUseAccessRequirementStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class DataAccessSubmissionManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private ResearchProjectDAO mockResearchProjectDao;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private DataAccessRequestDAO mockDataAccessRequestDao;
	@Mock
	private DataAccessSubmissionDAO mockDataAccessSubmissionDao;
	@Mock
	private GroupMembersDAO mockGroupMembersDao;
	@Mock
	private VerificationDAO mockVerificationDao;
	@Mock
	private AccessApprovalDAO mockAccessApprovalDao;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ACTAccessRequirement mockAccessRequirement;
	@Mock
	private ResearchProject mockResearchProject;
	@Mock
	private SubscriptionDAO mockSubscriptionDao;
	@Mock
	private ACTAccessRequirementStatus mockStatus;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;

	private DataAccessSubmissionManager manager;
	private DataAccessRenewal request;
	private String userId;
	private Long userIdLong;
	private String requestId;
	private String researchProjectId;
	private String accessRequirementId;
	private String ducFileHandleId;
	private String irbFileHandleId;
	private String attachmentId;
	private List<String> accessors;
	private String publication;
	private String summaryOfUse;
	private String submissionId;
	private String etag;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new DataAccessSubmissionManagerImpl();
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "researchProjectDao", mockResearchProjectDao);
		ReflectionTestUtils.setField(manager, "accessRequirementDao", mockAccessRequirementDao);
		ReflectionTestUtils.setField(manager, "dataAccessRequestDao", mockDataAccessRequestDao);
		ReflectionTestUtils.setField(manager, "dataAccessSubmissionDao", mockDataAccessSubmissionDao);
		ReflectionTestUtils.setField(manager, "groupMembersDao", mockGroupMembersDao);
		ReflectionTestUtils.setField(manager, "verificationDao", mockVerificationDao);
		ReflectionTestUtils.setField(manager, "accessApprovalDao", mockAccessApprovalDao);
		ReflectionTestUtils.setField(manager, "subscriptionDao", mockSubscriptionDao);
		ReflectionTestUtils.setField(manager, "transactionalMessenger", mockTransactionalMessenger);

		userId = "1";
		userIdLong = 1L;
		requestId = "2";
		researchProjectId = "3";
		accessRequirementId = "4";
		ducFileHandleId = "5";
		irbFileHandleId = "6";
		attachmentId = "7";
		publication = "publication";
		summaryOfUse = "summaryOfUse";
		submissionId = "8";
		etag = "etag";
		accessors = Arrays.asList(userId);

		request = new DataAccessRenewal();
		request.setId(requestId);
		request.setResearchProjectId(researchProjectId);
		request.setAccessRequirementId(accessRequirementId);
		request.setDucFileHandleId(ducFileHandleId);
		request.setIrbFileHandleId(irbFileHandleId);
		request.setAttachments(Arrays.asList(attachmentId));
		request.setAccessors(accessors);
		request.setPublication(publication);
		request.setSummaryOfUse(summaryOfUse);
		request.setEtag(etag);

		when(mockDataAccessRequestDao.get(requestId)).thenReturn(request);
		when(mockUser.getId()).thenReturn(1L);
		when(mockResearchProjectDao.get(researchProjectId)).thenReturn(mockResearchProject);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		when(mockDataAccessSubmissionDao.hasSubmissionWithState(
				userId, accessRequirementId, DataAccessSubmissionState.SUBMITTED))
				.thenReturn(false);
		when(mockAccessRequirement.getIsDUCRequired()).thenReturn(true);
		when(mockAccessRequirement.getIsIRBApprovalRequired()).thenReturn(true);
		when(mockAccessRequirement.getAreOtherAttachmentsRequired()).thenReturn(true);
		when(mockAccessRequirement.getIsCertifiedUserRequired()).thenReturn(true);
		when(mockAccessRequirement.getIsValidatedProfileRequired()).thenReturn(true);
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		when(mockGroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				new HashSet<String>(accessors)))
				.thenReturn(true);
		when(mockVerificationDao.haveValidatedProfiles(new HashSet<String>(accessors)))
				.thenReturn(true);

		when(mockDataAccessSubmissionDao.createSubmission(any(DataAccessSubmission.class)))
				.thenReturn(mockStatus);
		when(mockStatus.getSubmissionId()).thenReturn(submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullUserInfo() {
		manager.create(null, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullRequestID() {
		manager.create(mockUser, null, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullEtag() {
		manager.create(mockUser, requestId, null);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithOutdatedEtag() {
		when(mockDataAccessRequestDao.get(requestId)).thenThrow(new NotFoundException());
		manager.create(mockUser, requestId, "outdated etag");
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNonExistRequest() {
		when(mockDataAccessRequestDao.get(requestId)).thenThrow(new NotFoundException());
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNotExistResearchProject() {
		when(mockResearchProjectDao.get(researchProjectId)).thenThrow(new NotFoundException());
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessRequirementID() {
		request.setAccessRequirementId(null);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithSubmittedSubmission() {
		when(mockDataAccessSubmissionDao.hasSubmissionWithState(
				userId, accessRequirementId, DataAccessSubmissionState.SUBMITTED))
				.thenReturn(true);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNotExistAccessRequirement() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenThrow(new NotFoundException());
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNonACTAccessRequirement() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithDUCRequired() {
		request.setDucFileHandleId(null);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithIRBRequired() {
		request.setIrbFileHandleId(null);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithAttachmentsRequiredAndNullList() {
		request.setAttachments(null);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithAttachmentsRequiredAndEmptyList() {
		request.setAttachments(new LinkedList<String>());
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessors() {
		request.setAccessors(null);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithEmptyAccessorList() {
		request.setAccessors(new LinkedList<String>());
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNonCertifiedUser() {
		when(mockGroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				new HashSet<String>(accessors)))
				.thenReturn(false);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNonValidatedProfile() {
		when(mockVerificationDao.haveValidatedProfiles(new HashSet<String>(accessors)))
				.thenReturn(false);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNotRequireRenewal() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(false);
		manager.create(mockUser, requestId, etag);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithSubmitterIsNotAccessor() {
		when(mockUser.getId()).thenReturn(2L);
		manager.create(mockUser, requestId, etag);
	}

	@Test
	public void testCreate() {
		
		manager.create(mockUser, requestId, etag);
		ArgumentCaptor<DataAccessSubmission> submissionCaptor = ArgumentCaptor.forClass(DataAccessSubmission.class);
		verify(mockDataAccessSubmissionDao).createSubmission(submissionCaptor.capture());
		DataAccessSubmission captured = submissionCaptor.getValue();
		assertNotNull(captured);
		assertNotNull(captured.getSubmittedOn());
		assertEquals(userId, captured.getSubmittedBy());
		assertNotNull(captured.getModifiedOn());
		assertEquals(userId, captured.getModifiedBy());
		assertEquals(mockResearchProject, captured.getResearchProjectSnapshot());
		assertEquals(requestId, captured.getDataAccessRequestId());
		assertEquals(accessRequirementId, captured.getAccessRequirementId());
		assertEquals(ducFileHandleId, captured.getDucFileHandleId());
		assertEquals(irbFileHandleId, captured.getIrbFileHandleId());
		assertEquals(Arrays.asList(attachmentId), captured.getAttachments());
		assertEquals(accessors, captured.getAccessors());
		assertTrue(captured.getIsRenewalSubmission());
		assertEquals(publication, captured.getPublication());
		assertEquals(summaryOfUse, captured.getSummaryOfUse());
		assertEquals(DataAccessSubmissionState.SUBMITTED, captured.getState());
		verify(mockSubscriptionDao).create(userId, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(eq(submissionId),
				eq(ObjectType.DATA_ACCESS_SUBMISSION), anyString(), eq(ChangeType.CREATE), eq(userIdLong));
	}

	@Test
	public void testCreateWithNonRenewal() {
		DataAccessRequest request = new DataAccessRequest();
		request.setId(requestId);
		request.setResearchProjectId(researchProjectId);
		request.setAccessRequirementId(accessRequirementId);
		request.setDucFileHandleId(ducFileHandleId);
		request.setIrbFileHandleId(irbFileHandleId);
		request.setAttachments(Arrays.asList(attachmentId));
		request.setAccessors(accessors);
		request.setEtag(etag);
		when(mockDataAccessRequestDao.get(requestId)).thenReturn(request);
		manager.create(mockUser, requestId, etag);
		ArgumentCaptor<DataAccessSubmission> submissionCaptor = ArgumentCaptor.forClass(DataAccessSubmission.class);
		verify(mockDataAccessSubmissionDao).createSubmission(submissionCaptor.capture());
		DataAccessSubmission captured = submissionCaptor.getValue();
		assertNotNull(captured);
		assertNotNull(captured.getSubmittedOn());
		assertEquals(userId, captured.getSubmittedBy());
		assertEquals(mockResearchProject, captured.getResearchProjectSnapshot());
		assertEquals(requestId, captured.getDataAccessRequestId());
		assertEquals(accessRequirementId, captured.getAccessRequirementId());
		assertEquals(ducFileHandleId, captured.getDucFileHandleId());
		assertEquals(irbFileHandleId, captured.getIrbFileHandleId());
		assertEquals(Arrays.asList(attachmentId), captured.getAttachments());
		assertEquals(accessors, captured.getAccessors());
		assertFalse(captured.getIsRenewalSubmission());
		assertNull(captured.getPublication());
		assertNull(captured.getSummaryOfUse());
		assertEquals(DataAccessSubmissionState.SUBMITTED, captured.getState());
		verify(mockSubscriptionDao).create(userId, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(eq(submissionId),
				eq(ObjectType.DATA_ACCESS_SUBMISSION), anyString(), eq(ChangeType.CREATE), eq(userIdLong));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelNullUserInfo() {
		manager.cancel(null, submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelNullSubmissionId() {
		manager.cancel(mockUser, null);
	}

	@Test (expected = NotFoundException.class)
	public void testCancelWithNotFoundSubmission() {
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenThrow(new NotFoundException());
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCancelSubmissionUserHasNotSubmitted() {
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy("111");
		submission.setState(DataAccessSubmissionState.SUBMITTED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelApprovedSubmission() {
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.APPROVED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelRejectedSubmission() {
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.REJECTED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelCanceledSubmission() {
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.CANCELLED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test
	public void testCancel() {
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.SUBMITTED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		ACTAccessRequirementStatus status = new ACTAccessRequirementStatus();
		when(mockDataAccessSubmissionDao.cancel(eq(submissionId), eq(userId), anyLong(), anyString()))
				.thenReturn(status);
		assertEquals(status, manager.cancel(mockUser, submissionId));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusWithNullUserInfo() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		manager.updateStatus(null, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusWithRequest() {
		manager.updateStatus(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusWithNullSubmissionId() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setNewState(DataAccessSubmissionState.APPROVED);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusWithNullNewState() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusWithCancelledState() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.CANCELLED);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusWithSubmittedState() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.SUBMITTED);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateStatusUnauthorized() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(false);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateStatusSubmissionNotFound() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenThrow(new NotFoundException());
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusCancelledSubmission() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.CANCELLED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusRejectedSubmission() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.REJECTED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusApprovedSubmission() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.APPROVED);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.updateStatus(mockUser, request);
	}

	@Test
	public void testUpdateStatusRejected() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.REJECTED);
		String reason = "rejectedReason";
		request.setRejectedReason(reason);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.SUBMITTED);
		submission.setAccessRequirementId(accessRequirementId);
		submission.setAccessors(Arrays.asList(userId));
		submission.setEtag(etag);
		submission.setId(submissionId);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		when(mockDataAccessSubmissionDao.updateSubmissionStatus(eq(submissionId),
				eq(DataAccessSubmissionState.REJECTED), eq(reason), eq(userId),
				anyLong())).thenReturn(submission);
		assertEquals(submission, manager.updateStatus(mockUser, request));
		verify(mockTransactionalMessenger).sendMessageAfterCommit(submissionId, ObjectType.DATA_ACCESS_SUBMISSION_STATUS, etag, ChangeType.UPDATE, userIdLong);
	}

	@Test
	public void testUpdateStatusApproved() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(DataAccessSubmissionState.APPROVED);
		String reason = "rejectedReason";
		request.setRejectedReason(reason);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		DataAccessSubmission submission = new DataAccessSubmission();
		submission.setSubmittedBy(userId);
		submission.setState(DataAccessSubmissionState.SUBMITTED);
		submission.setAccessRequirementId(accessRequirementId);
		submission.setAccessors(Arrays.asList(userId));
		submission.setEtag(etag);
		submission.setId(submissionId);
		when(mockDataAccessSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		when(mockDataAccessSubmissionDao.updateSubmissionStatus(eq(submissionId),
				eq(DataAccessSubmissionState.APPROVED), eq(reason), eq(userId),
				anyLong())).thenReturn(submission);
		assertEquals(submission, manager.updateStatus(mockUser, request));
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(mockAccessApprovalDao).createBatch(captor.capture());
		List<AccessApproval> approvals = captor.getValue();
		assertEquals(1, approvals.size());
		assertTrue(approvals.get(0) instanceof ACTAccessApproval);
		ACTAccessApproval approval = (ACTAccessApproval) approvals.get(0);
		assertEquals(userId, approval.getAccessorId());
		assertEquals(userId, approval.getCreatedBy());
		assertNotNull(approval.getCreatedOn());
		assertEquals(userId, approval.getModifiedBy());
		assertNotNull(approval.getModifiedOn());
		assertEquals(accessRequirementId, approval.getRequirementId().toString());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(submissionId, ObjectType.DATA_ACCESS_SUBMISSION_STATUS, etag, ChangeType.UPDATE, userIdLong);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListSubmissionsWithNullUserInfo() {
		manager.listSubmission(null, new DataAccessSubmissionPageRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListSubmissionsWithNullRequest() {
		manager.listSubmission(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListSubmissionsWithNullAccessRequirementId() {
		manager.listSubmission(mockUser, new DataAccessSubmissionPageRequest());
	}

	@Test (expected = UnauthorizedException.class)
	public void testListSubmissionsUnauthorized() {
		DataAccessSubmissionPageRequest request = new DataAccessSubmissionPageRequest();
		request.setAccessRequirementId(accessRequirementId);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(false);
		manager.listSubmission(mockUser, request);
	}

	@Test
	public void testListSubmissionsAuthorized() {
		DataAccessSubmissionPageRequest request = new DataAccessSubmissionPageRequest();
		request.setAccessRequirementId(accessRequirementId);
		request.setFilterBy(DataAccessSubmissionState.SUBMITTED);
		request.setOrderBy(DataAccessSubmissionOrder.CREATED_ON);
		request.setIsAscending(true);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		List<DataAccessSubmission> list = new LinkedList<DataAccessSubmission>();
		when(mockDataAccessSubmissionDao.getSubmissions(accessRequirementId,
				DataAccessSubmissionState.SUBMITTED, DataAccessSubmissionOrder.CREATED_ON,
				true, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET)).thenReturn(list);
		DataAccessSubmissionPage page = manager.listSubmission(mockUser, request);
		assertNotNull(page);
		assertEquals(page.getResults(), list);

		verify(mockDataAccessSubmissionDao).getSubmissions(accessRequirementId,
				DataAccessSubmissionState.SUBMITTED, DataAccessSubmissionOrder.CREATED_ON,
				true, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementStatusWithNullUser() {
		manager.getAccessRequirementStatus(null, accessRequirementId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementStatusWithNullAccessRequirementId() {
		manager.getAccessRequirementStatus(mockUser, null);
	}

	@Test (expected = NotFoundException.class)
	public void testGetAccessRequirementStatusWithNonExistingAccessRequirement() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenThrow(new NotFoundException());
		manager.getAccessRequirementStatus(mockUser, accessRequirementId);
	}

	@Test
	public void testGetAccessRequirementStatusToUNotApproved() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(TermsOfUseAccessRequirement.class.getName());
		when(mockAccessApprovalDao.getForAccessRequirementsAndPrincipals(
				Arrays.asList(accessRequirementId), Arrays.asList(userId)))
			.thenReturn(new LinkedList<AccessApproval>());
		AccessRequirementStatus status = manager.getAccessRequirementStatus(mockUser, accessRequirementId);
		assertNotNull(status);
		assertTrue(status instanceof TermsOfUseAccessRequirementStatus);
		TermsOfUseAccessRequirementStatus touStatus = (TermsOfUseAccessRequirementStatus) status;
		assertEquals(accessRequirementId, touStatus.getAccessRequirementId());
		assertFalse(touStatus.getIsApproved());
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockAccessApprovalDao).getForAccessRequirementsAndPrincipals(
				Arrays.asList(accessRequirementId), Arrays.asList(userId));
	}

	@Test
	public void testGetAccessRequirementStatusToUApproved() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(TermsOfUseAccessRequirement.class.getName());
		AccessApproval approval = new TermsOfUseAccessApproval();
		approval.setAccessorId(userId);
		approval.setRequirementId(Long.parseLong(accessRequirementId));
		when(mockAccessApprovalDao.getForAccessRequirementsAndPrincipals(
			anyCollection(), anyCollection())).thenReturn(Arrays.asList(approval));
		AccessRequirementStatus status = manager.getAccessRequirementStatus(mockUser, accessRequirementId);
		assertNotNull(status);
		assertTrue(status instanceof TermsOfUseAccessRequirementStatus);
		TermsOfUseAccessRequirementStatus touStatus = (TermsOfUseAccessRequirementStatus) status;
		assertEquals(accessRequirementId, touStatus.getAccessRequirementId());
		assertTrue(touStatus.getIsApproved());
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockAccessApprovalDao).getForAccessRequirementsAndPrincipals(
				Arrays.asList(accessRequirementId), Arrays.asList(userId));
	}

	@Test (expected = IllegalStateException.class)
	public void testGetAccessRequirementStatusWith2Approvals() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(TermsOfUseAccessRequirement.class.getName());
		AccessApproval approval = new TermsOfUseAccessApproval();
		approval.setAccessorId(userId);
		approval.setRequirementId(Long.parseLong(accessRequirementId));
		when(mockAccessApprovalDao.getForAccessRequirementsAndPrincipals(
			anyCollection(), anyCollection())).thenReturn(Arrays.asList(approval, approval));
		manager.getAccessRequirementStatus(mockUser, accessRequirementId);
	}

	@Test (expected = IllegalStateException.class)
	public void testGetAccessRequirementStatusWithWrongApprovalType() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(TermsOfUseAccessRequirement.class.getName());
		AccessApproval approval = new ACTAccessApproval();
		approval.setAccessorId(userId);
		approval.setRequirementId(Long.parseLong(accessRequirementId));
		when(mockAccessApprovalDao.getForAccessRequirementsAndPrincipals(
			anyCollection(), anyCollection())).thenReturn(Arrays.asList(approval));
		manager.getAccessRequirementStatus(mockUser, accessRequirementId);
	}

	@Test (expected = IllegalStateException.class)
	public void testGetAccessRequirementStatusWithWrongUserId() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(TermsOfUseAccessRequirement.class.getName());
		AccessApproval approval = new TermsOfUseAccessApproval();
		approval.setAccessorId(userId+" ");
		approval.setRequirementId(Long.parseLong(accessRequirementId));
		when(mockAccessApprovalDao.getForAccessRequirementsAndPrincipals(
			anyCollection(), anyCollection())).thenReturn(Arrays.asList(approval));
		manager.getAccessRequirementStatus(mockUser, accessRequirementId);
	}

	@Test (expected = IllegalStateException.class)
	public void testGetAccessRequirementStatusWithWrongRequirementId() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(TermsOfUseAccessRequirement.class.getName());
		AccessApproval approval = new TermsOfUseAccessApproval();
		approval.setAccessorId(userId);
		approval.setRequirementId(Long.parseLong(accessRequirementId)+1);
		when(mockAccessApprovalDao.getForAccessRequirementsAndPrincipals(
			anyCollection(), anyCollection())).thenReturn(Arrays.asList(approval));
		manager.getAccessRequirementStatus(mockUser, accessRequirementId);
	}

	@Test
	public void testGetAccessRequirementStatusACT() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(ACTAccessRequirement.class.getName());
		ACTAccessRequirementStatus status = new ACTAccessRequirementStatus();
		when(mockDataAccessSubmissionDao.getStatusByRequirementIdAndPrincipalId(accessRequirementId, userId))
			.thenReturn(status);
		assertEquals(status, manager.getAccessRequirementStatus(mockUser, accessRequirementId));
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockDataAccessSubmissionDao).getStatusByRequirementIdAndPrincipalId(accessRequirementId, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementStatusNotSupportType() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn("not supported type");
		manager.getAccessRequirementStatus(mockUser, accessRequirementId);
	}
}
