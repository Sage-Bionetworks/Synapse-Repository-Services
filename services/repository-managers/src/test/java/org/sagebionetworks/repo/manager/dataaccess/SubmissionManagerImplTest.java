package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.BasicAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.ManagedACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

public class SubmissionManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private ResearchProjectDAO mockResearchProjectDao;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private SubmissionDAO mockSubmissionDao;
	@Mock
	private AccessApprovalDAO mockAccessApprovalDao;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ManagedACTAccessRequirement mockAccessRequirement;
	@Mock
	private ResearchProject mockResearchProject;
	@Mock
	private SubscriptionDAO mockSubscriptionDao;
	@Mock
	private SubmissionStatus mockSubmissionStatus;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	private RequestManager mockRequestManager;

	private SubmissionManager manager;
	private Renewal request;
	private String userId;
	private Long userIdLong;
	private String requestId;
	private String researchProjectId;
	private String accessRequirementId;
	private Long accessRequirementVersion;
	private String ducFileHandleId;
	private String irbFileHandleId;
	private String attachmentId;
	private List<AccessorChange> accessors;
	private HashSet<String> accessorIds;
	private String publication;
	private String summaryOfUse;
	private String submissionId;
	private String etag;
	private Submission submission;
	private CreateSubmissionRequest csRequest;
	private String subjectId;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new SubmissionManagerImpl();
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "researchProjectDao", mockResearchProjectDao);
		ReflectionTestUtils.setField(manager, "accessRequirementDao", mockAccessRequirementDao);
		ReflectionTestUtils.setField(manager, "submissionDao", mockSubmissionDao);
		ReflectionTestUtils.setField(manager, "accessApprovalDao", mockAccessApprovalDao);
		ReflectionTestUtils.setField(manager, "subscriptionDao", mockSubscriptionDao);
		ReflectionTestUtils.setField(manager, "transactionalMessenger", mockTransactionalMessenger);
		ReflectionTestUtils.setField(manager, "requestManager", mockRequestManager);

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
		accessRequirementVersion = 9L;
		AccessorChange accesorChange = new AccessorChange();
		accesorChange.setUserId(userId);
		accesorChange.setType(AccessType.RENEW_ACCESS);
		accessors = Arrays.asList(accesorChange);

		accessorIds = Sets.newHashSet(userId);

		request = new Renewal();
		request.setId(requestId);
		request.setResearchProjectId(researchProjectId);
		request.setAccessRequirementId(accessRequirementId);
		request.setDucFileHandleId(ducFileHandleId);
		request.setIrbFileHandleId(irbFileHandleId);
		request.setAttachments(Arrays.asList(attachmentId));
		request.setAccessorChanges(accessors);
		request.setPublication(publication);
		request.setSummaryOfUse(summaryOfUse);
		request.setEtag(etag);

		when(mockRequestManager.getRequestForSubmission(requestId)).thenReturn(request);
		when(mockUser.getId()).thenReturn(1L);
		when(mockResearchProjectDao.get(researchProjectId)).thenReturn(mockResearchProject);
		when(mockResearchProject.getId()).thenReturn(researchProjectId);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		when(mockSubmissionDao.hasSubmissionWithState(
				userId, accessRequirementId, SubmissionState.SUBMITTED))
				.thenReturn(false);
		when(mockAccessRequirement.getIsDUCRequired()).thenReturn(true);
		when(mockAccessRequirement.getIsIRBApprovalRequired()).thenReturn(true);
		when(mockAccessRequirement.getAreOtherAttachmentsRequired()).thenReturn(true);
		when(mockAccessRequirement.getIsCertifiedUserRequired()).thenReturn(true);
		when(mockAccessRequirement.getIsValidatedProfileRequired()).thenReturn(true);
		when(mockAccessRequirement.getVersionNumber()).thenReturn(accessRequirementVersion);

		when(mockSubmissionDao.createSubmission(any(Submission.class)))
				.thenReturn(mockSubmissionStatus);
		when(mockSubmissionStatus.getSubmissionId()).thenReturn(submissionId);
		when(mockAccessApprovalDao.hasApprovalsSubmittedBy(accessorIds, userId, accessRequirementId)).thenReturn(true);

		submission = new Submission();
		submission.setRequestId(requestId);
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.SUBMITTED);
		submission.setAccessRequirementId(accessRequirementId);
		submission.setAccessorChanges(accessors);
		submission.setEtag(etag);
		submission.setId(submissionId);
		submission.setAccessRequirementVersion(accessRequirementVersion);
		submission.setResearchProjectSnapshot(mockResearchProject);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);

		subjectId = "syn987";
		csRequest = new CreateSubmissionRequest();
		csRequest.setRequestId(requestId);
		csRequest.setRequestEtag(etag);
		csRequest.setSubjectId(subjectId);
		csRequest.setSubjectType(RestrictableObjectType.ENTITY);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullUserInfo() {
		manager.create(null, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullRequesto() {
		manager.create(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullRequestID() {
		csRequest.setRequestId(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullEtag() {
		csRequest.setRequestEtag(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullSubjectId() {
		csRequest.setSubjectId(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullSubjectType() {
		csRequest.setSubjectType(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithOutdatedEtag() {
		csRequest.setRequestEtag("outdated etag");
		when(mockRequestManager.getRequestForSubmission(requestId)).thenThrow(new NotFoundException());
		manager.create(mockUser, csRequest);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNonExistRequest() {
		when(mockRequestManager.getRequestForSubmission(requestId)).thenThrow(new NotFoundException());
		manager.create(mockUser, csRequest);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNotExistResearchProject() {
		when(mockResearchProjectDao.get(researchProjectId)).thenThrow(new NotFoundException());
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessRequirementID() {
		request.setAccessRequirementId(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithSubmittedSubmission() {
		when(mockSubmissionDao.hasSubmissionWithState(
				userId, accessRequirementId, SubmissionState.SUBMITTED))
				.thenReturn(true);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNotExistAccessRequirement() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenThrow(new NotFoundException());
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNonACTAccessRequirement() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithDUCRequired() {
		request.setDucFileHandleId(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithIRBRequired() {
		request.setIrbFileHandleId(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithAttachmentsRequiredAndNullList() {
		request.setAttachments(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithAttachmentsRequiredAndEmptyList() {
		request.setAttachments(new LinkedList<String>());
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessors() {
		request.setAccessorChanges(null);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithEmptyAccessorList() {
		request.setAccessorChanges(new LinkedList<AccessorChange>());
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithAccessorRequirementNotSatisfied() {
		doThrow(new IllegalArgumentException()).when(mockAuthorizationManager).validateHasAccessorRequirement(mockAccessRequirement, accessorIds);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithSubmitterIsNotAccessor() {
		when(mockUser.getId()).thenReturn(2L);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithRenewAccessorUserDidNotSubmit() {
		when(mockAccessApprovalDao.hasApprovalsSubmittedBy(accessorIds, userId, accessRequirementId)).thenReturn(false);
		manager.create(mockUser, csRequest);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateListAccessorTwice() {
		AccessorChange accesorChange = new AccessorChange();
		accesorChange.setUserId(userId);
		accesorChange.setType(AccessType.GAIN_ACCESS);
		AccessorChange accesorChange2 = new AccessorChange();
		accesorChange2.setUserId(userId);
		accesorChange2.setType(AccessType.RENEW_ACCESS);
		accessors = Arrays.asList(accesorChange, accesorChange2);
		request.setAccessorChanges(accessors);
		manager.create(mockUser, csRequest);
	}

	@Test
	public void testCreate() {
		manager.create(mockUser, csRequest);
		ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
		verify(mockSubmissionDao).createSubmission(submissionCaptor.capture());
		Submission captured = submissionCaptor.getValue();
		assertNotNull(captured);
		assertNotNull(captured.getSubmittedOn());
		assertEquals(userId, captured.getSubmittedBy());
		assertNotNull(captured.getModifiedOn());
		assertEquals(userId, captured.getModifiedBy());
		assertEquals(mockResearchProject, captured.getResearchProjectSnapshot());
		assertEquals(requestId, captured.getRequestId());
		assertEquals(accessRequirementId, captured.getAccessRequirementId());
		assertEquals(ducFileHandleId, captured.getDucFileHandleId());
		assertEquals(irbFileHandleId, captured.getIrbFileHandleId());
		assertEquals(Arrays.asList(attachmentId), captured.getAttachments());
		assertEquals(accessors, captured.getAccessorChanges());
		assertTrue(captured.getIsRenewalSubmission());
		assertEquals(publication, captured.getPublication());
		assertEquals(summaryOfUse, captured.getSummaryOfUse());
		assertEquals(SubmissionState.SUBMITTED, captured.getState());
		verify(mockSubscriptionDao).create(userId, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(eq(submissionId),
				eq(ObjectType.DATA_ACCESS_SUBMISSION), anyString(), eq(ChangeType.CREATE), eq(userIdLong));
		verify(mockAuthorizationManager).validateHasAccessorRequirement(mockAccessRequirement, accessorIds);
	}

	@Test
	public void testCreateWithNonRenewal() {
		Request request = new Request();
		request.setId(requestId);
		request.setResearchProjectId(researchProjectId);
		request.setAccessRequirementId(accessRequirementId);
		request.setDucFileHandleId(ducFileHandleId);
		request.setIrbFileHandleId(irbFileHandleId);
		request.setAttachments(Arrays.asList(attachmentId));
		request.setAccessorChanges(accessors);
		request.setEtag(etag);
		when(mockRequestManager.getRequestForSubmission(requestId)).thenReturn(request);
		manager.create(mockUser, csRequest);
		ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
		verify(mockSubmissionDao).createSubmission(submissionCaptor.capture());
		Submission captured = submissionCaptor.getValue();
		assertNotNull(captured);
		assertNotNull(captured.getSubmittedOn());
		assertEquals(userId, captured.getSubmittedBy());
		assertEquals(mockResearchProject, captured.getResearchProjectSnapshot());
		assertEquals(requestId, captured.getRequestId());
		assertEquals(accessRequirementId, captured.getAccessRequirementId());
		assertEquals(ducFileHandleId, captured.getDucFileHandleId());
		assertEquals(irbFileHandleId, captured.getIrbFileHandleId());
		assertEquals(Arrays.asList(attachmentId), captured.getAttachments());
		assertEquals(accessors, captured.getAccessorChanges());
		assertFalse(captured.getIsRenewalSubmission());
		assertNull(captured.getPublication());
		assertNull(captured.getSummaryOfUse());
		assertEquals(SubmissionState.SUBMITTED, captured.getState());
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
		when(mockSubmissionDao.getForUpdate(submissionId)).thenThrow(new NotFoundException());
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = UnauthorizedException.class)
	public void testCancelSubmissionUserHasNotSubmitted() {
		Submission submission = new Submission();
		submission.setSubmittedBy("111");
		submission.setState(SubmissionState.SUBMITTED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelApprovedSubmission() {
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.APPROVED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelRejectedSubmission() {
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.REJECTED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCancelCanceledSubmission() {
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.CANCELLED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.cancel(mockUser, submissionId);
	}

	@Test
	public void testCancel() {
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.SUBMITTED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		when(mockSubmissionDao.cancel(eq(submissionId), eq(userId), anyLong(), anyString()))
				.thenReturn(mockSubmissionStatus);
		assertEquals(mockSubmissionStatus, manager.cancel(mockUser, submissionId));
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
		request.setNewState(SubmissionState.APPROVED);
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
		request.setNewState(SubmissionState.CANCELLED);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusWithSubmittedState() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.SUBMITTED);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateStatusUnauthorized() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(false);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateStatusSubmissionNotFound() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenThrow(new NotFoundException());
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusCancelledSubmission() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.CANCELLED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusRejectedSubmission() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.REJECTED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.updateStatus(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateStatusApprovedSubmission() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.APPROVED);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.APPROVED);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		manager.updateStatus(mockUser, request);
	}

	@Test
	public void testUpdateStatusRejected() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.REJECTED);
		String reason = "rejectedReason";
		request.setRejectedReason(reason);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		Submission submission = new Submission();
		submission.setSubmittedBy(userId);
		submission.setState(SubmissionState.SUBMITTED);
		submission.setAccessRequirementId(accessRequirementId);
		submission.setAccessorChanges(accessors);
		submission.setEtag(etag);
		submission.setId(submissionId);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		when(mockSubmissionDao.updateSubmissionStatus(eq(submissionId),
				eq(SubmissionState.REJECTED), eq(reason), eq(userId),
				anyLong())).thenReturn(submission);
		// call under test
		assertEquals(submission, manager.updateStatus(mockUser, request));
		verify(mockTransactionalMessenger).sendMessageAfterCommit(submissionId, ObjectType.DATA_ACCESS_SUBMISSION_STATUS, etag, ChangeType.UPDATE, userIdLong);
		verify(mockRequestManager, never()).updateApprovedRequest(anyString());
	}

	@Test
	public void testUpdateStatusApproved() {
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.APPROVED);
		AccessorChange accesorChange1 = new AccessorChange();
		accesorChange1.setUserId(userId);
		accesorChange1.setType(AccessType.RENEW_ACCESS);
		AccessorChange accesorChange2 = new AccessorChange();
		accesorChange2.setUserId("2");
		accesorChange2.setType(AccessType.REVOKE_ACCESS);
		AccessorChange accesorChange3 = new AccessorChange();
		accesorChange3.setUserId("3");
		accesorChange3.setType(AccessType.GAIN_ACCESS);
		accessors = Arrays.asList(accesorChange1, accesorChange2, accesorChange3);
		accessorIds = Sets.newHashSet(userId);
		String reason = "rejectedReason";
		request.setRejectedReason(reason);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		submission.setAccessorChanges(accessors);

		when(mockSubmissionDao.updateSubmissionStatus(eq(submissionId),
				eq(SubmissionState.APPROVED), eq(reason), eq(userId),
				anyLong())).thenReturn(submission);
		// call under test
		assertEquals(submission, manager.updateStatus(mockUser, request));

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(mockAccessApprovalDao).revokeBySubmitter(stringCaptor.capture(),
				stringCaptor.capture(), captor.capture(), stringCaptor.capture());
		List accessorsToRevoke = captor.getValue();
		assertNotNull(accessorsToRevoke);
		assertEquals(2, accessorsToRevoke.size());
		assertTrue(accessorsToRevoke.contains(userId));
		assertTrue(accessorsToRevoke.contains("2"));

		verify(mockAccessApprovalDao).createOrUpdateBatch(captor.capture());
		List<AccessApproval> approvals = captor.getValue();
		assertEquals(2, approvals.size());
		AccessApproval approval1 = approvals.get(0);
		assertEquals(userId, approval1.getAccessorId());
		assertEquals(userId, approval1.getCreatedBy());
		assertNotNull(approval1.getCreatedOn());
		assertEquals(userId, approval1.getModifiedBy());
		assertNotNull(approval1.getModifiedOn());
		assertNull(approval1.getExpiredOn());
		assertEquals(approval1.getState(), ApprovalState.APPROVED);
		assertEquals(accessRequirementId, approval1.getRequirementId().toString());
		AccessApproval approval3 = approvals.get(1);
		assertEquals("3", approval3.getAccessorId());
		assertEquals(userId, approval3.getCreatedBy());
		assertNotNull(approval3.getCreatedOn());
		assertEquals(userId, approval3.getModifiedBy());
		assertNotNull(approval3.getModifiedOn());
		assertNull(approval3.getExpiredOn());
		assertEquals(approval3.getState(), ApprovalState.APPROVED);
		assertEquals(accessRequirementId, approval3.getRequirementId().toString());

		verify(mockTransactionalMessenger).sendMessageAfterCommit(submissionId, ObjectType.DATA_ACCESS_SUBMISSION_STATUS, etag, ChangeType.UPDATE, userIdLong);
		verify(mockRequestManager).updateApprovedRequest(requestId);
	}

	@Test
	public void testUpdateStatusApprovedWithExpiration() {
		when(mockAccessRequirement.getExpirationPeriod()).thenReturn(30*24*60*60*1000L);
		SubmissionStateChangeRequest request = new SubmissionStateChangeRequest();
		request.setSubmissionId(submissionId);
		request.setNewState(SubmissionState.APPROVED);
		String reason = "rejectedReason";
		request.setRejectedReason(reason);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		submission.setAccessorChanges(accessors);
		when(mockSubmissionDao.getForUpdate(submissionId)).thenReturn(submission);
		when(mockSubmissionDao.updateSubmissionStatus(eq(submissionId),
				eq(SubmissionState.APPROVED), eq(reason), eq(userId),
				anyLong())).thenReturn(submission);
		// call under test
		assertEquals(submission, manager.updateStatus(mockUser, request));

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(mockAccessApprovalDao).revokeBySubmitter(stringCaptor.capture(),
				stringCaptor.capture(), captor.capture(), stringCaptor.capture());
		List accessorsToRevoke = captor.getValue();
		assertNotNull(accessorsToRevoke);
		assertEquals(1, accessorsToRevoke.size());
		assertTrue(accessorsToRevoke.contains(userId));

		verify(mockAccessApprovalDao).createOrUpdateBatch(captor.capture());
		List<AccessApproval> approvals = captor.getValue();
		assertEquals(1, approvals.size());
		AccessApproval approval = approvals.get(0);
		assertEquals(userId, approval.getAccessorId());
		assertEquals(userId, approval.getCreatedBy());
		assertNotNull(approval.getCreatedOn());
		assertEquals(userId, approval.getModifiedBy());
		assertNotNull(approval.getModifiedOn());
		assertNotNull(approval.getExpiredOn());
		assertEquals(approval.getState(), ApprovalState.APPROVED);
		assertEquals(accessRequirementId, approval.getRequirementId().toString());

		verify(mockTransactionalMessenger).sendMessageAfterCommit(submissionId, ObjectType.DATA_ACCESS_SUBMISSION_STATUS, etag, ChangeType.UPDATE, userIdLong);
		verify(mockRequestManager).updateApprovedRequest(requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListSubmissionsWithNullUserInfo() {
		manager.listSubmission(null, new SubmissionPageRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListSubmissionsWithNullRequest() {
		manager.listSubmission(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testListSubmissionsWithNullAccessRequirementId() {
		manager.listSubmission(mockUser, new SubmissionPageRequest());
	}

	@Test (expected = UnauthorizedException.class)
	public void testListSubmissionsUnauthorized() {
		SubmissionPageRequest request = new SubmissionPageRequest();
		request.setAccessRequirementId(accessRequirementId);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(false);
		manager.listSubmission(mockUser, request);
	}

	@Test
	public void testListSubmissionsAuthorized() {
		SubmissionPageRequest request = new SubmissionPageRequest();
		request.setAccessRequirementId(accessRequirementId);
		request.setFilterBy(SubmissionState.SUBMITTED);
		request.setOrderBy(SubmissionOrder.CREATED_ON);
		request.setIsAscending(true);
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		List<Submission> list = new LinkedList<Submission>();
		when(mockSubmissionDao.getSubmissions(accessRequirementId,
				SubmissionState.SUBMITTED, SubmissionOrder.CREATED_ON,
				true, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET)).thenReturn(list);
		// call under test
		SubmissionPage page = manager.listSubmission(mockUser, request);
		assertNotNull(page);
		assertEquals(page.getResults(), list);

		verify(mockSubmissionDao).getSubmissions(accessRequirementId,
				SubmissionState.SUBMITTED, SubmissionOrder.CREATED_ON,
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
		when(mockAccessApprovalDao.getActiveApprovalsForUser(
				accessRequirementId, userId))
			.thenReturn(new LinkedList<AccessApproval>());
		// call under test
		AccessRequirementStatus status = manager.getAccessRequirementStatus(mockUser, accessRequirementId);
		assertNotNull(status);
		assertTrue(status instanceof BasicAccessRequirementStatus);
		BasicAccessRequirementStatus basicStatus = (BasicAccessRequirementStatus) status;
		assertEquals(accessRequirementId, basicStatus.getAccessRequirementId());
		assertFalse(basicStatus.getIsApproved());
		assertNull(basicStatus.getExpiredOn());
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockAccessApprovalDao).getActiveApprovalsForUser(
				accessRequirementId, userId);
	}

	@Test
	public void testGetAccessRequirementStatusToUApproved() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(TermsOfUseAccessRequirement.class.getName());
		AccessApproval approval = new AccessApproval();
		approval.setAccessorId(userId);
		approval.setRequirementId(Long.parseLong(accessRequirementId));
		approval.setExpiredOn(new Date());
		when(mockAccessApprovalDao.getActiveApprovalsForUser(
				accessRequirementId, userId))
			.thenReturn(Arrays.asList(approval));
		AccessRequirementStatus status = manager.getAccessRequirementStatus(mockUser, accessRequirementId);
		assertNotNull(status);
		assertTrue(status instanceof BasicAccessRequirementStatus);
		BasicAccessRequirementStatus basicStatus = (BasicAccessRequirementStatus) status;
		assertEquals(accessRequirementId, basicStatus.getAccessRequirementId());
		assertTrue(basicStatus.getIsApproved());
		assertEquals(basicStatus.getExpiredOn(), approval.getExpiredOn());
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockAccessApprovalDao).getActiveApprovalsForUser(
				accessRequirementId, userId);
	}

	/*
	 * PLFM-4501
	 */
	@Test
	public void testGetAccessRequirementStatusSelfSignApproved() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(SelfSignAccessRequirement.class.getName());
		AccessApproval approval = new AccessApproval();
		approval.setAccessorId(userId);
		approval.setRequirementId(Long.parseLong(accessRequirementId));
		approval.setExpiredOn(new Date());
		when(mockAccessApprovalDao.getActiveApprovalsForUser(
				accessRequirementId, userId))
			.thenReturn(Arrays.asList(approval));
		AccessRequirementStatus status = manager.getAccessRequirementStatus(mockUser, accessRequirementId);
		assertNotNull(status);
		assertTrue(status instanceof BasicAccessRequirementStatus);
		BasicAccessRequirementStatus basicStatus = (BasicAccessRequirementStatus) status;
		assertEquals(accessRequirementId, basicStatus.getAccessRequirementId());
		assertTrue(basicStatus.getIsApproved());
		assertEquals(basicStatus.getExpiredOn(), approval.getExpiredOn());
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockAccessApprovalDao).getActiveApprovalsForUser(
				accessRequirementId, userId);
	}

	@Test
	public void testGetAccessRequirementStatusACTApproved() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(ACTAccessRequirement.class.getName());
		AccessApproval approval = new AccessApproval();
		approval.setAccessorId(userId);
		approval.setRequirementId(Long.parseLong(accessRequirementId));
		approval.setExpiredOn(new Date());
		when(mockAccessApprovalDao.getActiveApprovalsForUser(
				accessRequirementId, userId))
			.thenReturn(Arrays.asList(approval));
		AccessRequirementStatus status = manager.getAccessRequirementStatus(mockUser, accessRequirementId);
		assertNotNull(status);
		assertTrue(status instanceof BasicAccessRequirementStatus);
		BasicAccessRequirementStatus basicStatus = (BasicAccessRequirementStatus) status;
		assertEquals(accessRequirementId, basicStatus.getAccessRequirementId());
		assertTrue(basicStatus.getIsApproved());
		assertEquals(basicStatus.getExpiredOn(), approval.getExpiredOn());
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockAccessApprovalDao).getActiveApprovalsForUser(
				accessRequirementId, userId);
	}

	@Test
	public void testGetAccessRequirementStatusACT() {
		when(mockAccessRequirementDao.getConcreteType(accessRequirementId))
			.thenReturn(ManagedACTAccessRequirement.class.getName());
		when(mockSubmissionDao.getStatusByRequirementIdAndPrincipalId(accessRequirementId, userId))
			.thenReturn(mockSubmissionStatus);
		AccessRequirementStatus arStatus = manager.getAccessRequirementStatus(mockUser, accessRequirementId);
		assertNotNull(arStatus);
		assertEquals(accessRequirementId, arStatus.getAccessRequirementId());
		assertFalse(arStatus.getIsApproved());
		assertTrue(arStatus instanceof ManagedACTAccessRequirementStatus);
		assertNull(arStatus.getExpiredOn());
		ManagedACTAccessRequirementStatus actARStatus = (ManagedACTAccessRequirementStatus) arStatus;
		assertEquals(mockSubmissionStatus, actARStatus.getCurrentSubmissionStatus());
		verify(mockAccessRequirementDao).getConcreteType(accessRequirementId);
		verify(mockSubmissionDao).getStatusByRequirementIdAndPrincipalId(accessRequirementId, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetOpenSubmissionsWithNullUserInfo() {
		manager.getOpenSubmissions(null, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testGetOpenSubmissionsUnauthorized() {
		manager.getOpenSubmissions(mockUser, null);
	}

	@Test
	public void testGetOpenSubmissionsAuthorized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(mockUser)).thenReturn(true);
		List<OpenSubmission> list = new LinkedList<OpenSubmission>();
		when(mockSubmissionDao.getOpenSubmissions(NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET)).thenReturn(list );
		OpenSubmissionPage result = manager.getOpenSubmissions(mockUser, null);
		assertNotNull(result);
		assertEquals(list, result.getOpenSubmissionList());
		verify(mockSubmissionDao).getOpenSubmissions(NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetLatestExpirationDateWithNullList() {
		SubmissionManagerImpl.getLatestExpirationDate(null);
	}

	@Test
	public void testGetLatestExpirationDateWithEmptyList() {
		assertNull(SubmissionManagerImpl.getLatestExpirationDate(new LinkedList<AccessApproval>()));
	}

	@Test
	public void testGetLatestExpirationDateWithoutExpiration() {
		AccessApproval approval = new AccessApproval();
		assertNull(SubmissionManagerImpl.getLatestExpirationDate(Arrays.asList(approval)));
	}

	@Test
	public void testGetLatestExpirationDateWithOneExpiration() {
		AccessApproval approval = new AccessApproval();
		approval.setExpiredOn(new Date());
		assertEquals(approval.getExpiredOn(),
				SubmissionManagerImpl.getLatestExpirationDate(Arrays.asList(approval)));
	}

	@Test
	public void testGetLatestExpirationDateWithMultipleExpiration() {
		AccessApproval approval1 = new AccessApproval();
		approval1.setExpiredOn(new Date(1496703298000L) /* June 5th 2017 */);
		AccessApproval approval2 = new AccessApproval();
		approval2.setExpiredOn(new Date(1496357698000L) /* June 1st 2017 */);
		assertEquals(approval1.getExpiredOn(),
				SubmissionManagerImpl.getLatestExpirationDate(Arrays.asList(approval1, approval2)));
	}
}
