package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessSubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class DataAccessSubmissionManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private IdGenerator mockIdGenerator;
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
	private UserInfo mockUser;
	@Mock
	private ACTAccessRequirement mockAccessRequirement;
	@Mock
	private ResearchProject mockResearchProject;

	private DataAccessSubmissionManager manager;
	private DataAccessRenewal request;
	private String userId;
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

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new DataAccessSubmissionManagerImpl();
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(manager, "researchProjectDao", mockResearchProjectDao);
		ReflectionTestUtils.setField(manager, "accessRequirementDao", mockAccessRequirementDao);
		ReflectionTestUtils.setField(manager, "dataAccessRequestDao", mockDataAccessRequestDao);
		ReflectionTestUtils.setField(manager, "dataAccessSubmissionDao", mockDataAccessSubmissionDao);
		ReflectionTestUtils.setField(manager, "groupMembersDao", mockGroupMembersDao);
		ReflectionTestUtils.setField(manager, "verificationDao", mockVerificationDao);

		userId = "1";
		requestId = "2";
		researchProjectId = "3";
		accessRequirementId = "4";
		ducFileHandleId = "5";
		irbFileHandleId = "6";
		attachmentId = "7";
		publication = "publication";
		summaryOfUse = "summaryOfUse";
		submissionId = "8";
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
		when(mockIdGenerator.generateNewId(TYPE.DATA_ACCESS_SUBMISSION_ID)).thenReturn(8L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullUserInfo() {
		manager.create(null, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullRequestID() {
		manager.create(mockUser, null);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNonExistRequest() {
		when(mockDataAccessRequestDao.get(requestId)).thenThrow(new NotFoundException());
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullResearchProjectID() {
		request.setResearchProjectId(null);
		manager.create(mockUser, requestId);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNotExistResearchProject() {
		when(mockResearchProjectDao.get(researchProjectId)).thenThrow(new NotFoundException());
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessRequirementID() {
		request.setAccessRequirementId(null);
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithSubmittedSubmission() {
		when(mockDataAccessSubmissionDao.hasSubmissionWithState(
				userId, accessRequirementId, DataAccessSubmissionState.SUBMITTED))
				.thenReturn(true);
		manager.create(mockUser, requestId);
	}

	@Test (expected = NotFoundException.class)
	public void testCreateWithNotExistAccessRequirement() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenThrow(new NotFoundException());
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNonACTAccessRequirement() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithDUCRequired() {
		request.setDucFileHandleId(null);
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithIRBRequired() {
		request.setIrbFileHandleId(null);
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithAttachmentsRequiredAndNullList() {
		request.setAttachments(null);
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithAttachmentsRequiredAndEmptyList() {
		request.setAttachments(new LinkedList<String>());
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessors() {
		request.setAccessors(null);
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithEmptyAccessorList() {
		request.setAccessors(new LinkedList<String>());
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNonCertifiedUser() {
		when(mockGroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				new HashSet<String>(accessors)))
				.thenReturn(false);
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNonValidatedProfile() {
		when(mockVerificationDao.haveValidatedProfiles(new HashSet<String>(accessors)))
				.thenReturn(false);
		manager.create(mockUser, requestId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNotRequireRenewal() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(false);
		manager.create(mockUser, requestId);
	}

	@Test
	public void testCreate() {
		manager.create(mockUser, requestId);
		ArgumentCaptor<DataAccessSubmission> captor = ArgumentCaptor.forClass(DataAccessSubmission.class);
		verify(mockDataAccessSubmissionDao).create(captor.capture());
		DataAccessSubmission captured = captor.getValue();
		assertNotNull(captured);
		assertEquals(submissionId, captured.getId());
		assertNotNull(captured.getEtag());
		assertNotNull(captured.getSubmittedOn());
		assertEquals(userId, captured.getSubmittedBy());
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
		when(mockDataAccessRequestDao.get(requestId)).thenReturn(request);
		manager.create(mockUser, requestId);
		ArgumentCaptor<DataAccessSubmission> captor = ArgumentCaptor.forClass(DataAccessSubmission.class);
		verify(mockDataAccessSubmissionDao).create(captor.capture());
		DataAccessSubmission captured = captor.getValue();
		assertNotNull(captured);
		assertEquals(submissionId, captured.getId());
		assertNotNull(captured.getEtag());
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
	}

}
