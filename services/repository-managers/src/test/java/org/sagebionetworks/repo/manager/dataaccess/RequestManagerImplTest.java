package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class RequestManagerImplTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private RequestDAO mockRequestDao;
	@Mock
	private SubmissionDAO mockSubmissionDao;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ACTAccessRequirement mockAccessRequirement;

	private RequestManagerImpl manager;
	private String accessRequirementId;
	private String userId;
	private String researchProjectId;
	private String requestId;
	private Date createdOn;
	private Date modifiedOn;
	private String etag;
	private Request request;
	private Renewal renewal;


	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new RequestManagerImpl();
		ReflectionTestUtils.setField(manager, "accessRequirementDao", mockAccessRequirementDao);
		ReflectionTestUtils.setField(manager, "requestDao", mockRequestDao);
		ReflectionTestUtils.setField(manager, "submissionDao", mockSubmissionDao);

		userId = "1";
		accessRequirementId = "2";
		researchProjectId = "3";
		requestId = "4";
		createdOn = new Date();
		modifiedOn = new Date();
		etag = "etag";
		request = createNewRequest();
		renewal = manager.createRenewalFromRequest(request);

		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.create(any(Request.class))).thenReturn(request);
		when(mockRequestDao.getUserOwnCurrentRequest(accessRequirementId, userId)).thenReturn(request);
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		when(mockRequestDao.update(any(RequestInterface.class))).thenReturn(request);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		when(mockAccessRequirement.getAcceptRequest()).thenReturn(true);
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.SUBMITTED)).thenReturn(false);
	}

	private Request createNewRequest() {
		Request dto = new Request();
		dto.setId(requestId);
		dto.setCreatedBy(userId);
		dto.setCreatedOn(createdOn);
		dto.setModifiedBy(userId);
		dto.setModifiedOn(modifiedOn);
		dto.setEtag(etag);
		dto.setAccessRequirementId(accessRequirementId);
		dto.setResearchProjectId(researchProjectId);
		return dto;
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullUserInfo() {
		manager.create(null, createNewRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullRequest() {
		manager.create(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessRequirementId() {
		Request toCreate = createNewRequest();
		toCreate.setAccessRequirementId(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullResearchProjectId() {
		Request toCreate = createNewRequest();
		toCreate.setResearchProjectId(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNotACTAccessRequirementId() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		manager.create(null, createNewRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithACTAccessRequirementNullAcceptRequest() {
		when(mockAccessRequirement.getAcceptRequest()).thenReturn(null);
		manager.create(mockUser, createNewRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithACTAccessRequirementDoesNotAcceptRequest() {
		when(mockAccessRequirement.getAcceptRequest()).thenReturn(false);
		manager.create(mockUser, createNewRequest());
	}

	@Test
	public void testCreate() {
		assertEquals(request, manager.create(mockUser, createNewRequest()));
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(mockRequestDao).create(captor.capture());
		Request toCreate = captor.getValue();
		assertEquals(requestId, toCreate.getId());
		assertEquals(userId, toCreate.getCreatedBy());
		assertEquals(userId, toCreate.getModifiedBy());
	}

	@Test
	public void testPrepareUpdateFields() {
		String modifiedBy = "111";
		Request prepared = (Request) manager.prepareUpdateFields(request, modifiedBy);
		assertEquals(modifiedBy, prepared.getModifiedBy());
	}

	@Test
	public void testPrepareCreationFields() {
		String createdBy = "222";
		Request prepared = (Request) manager.prepareCreationFields(request, createdBy);
		assertEquals(createdBy, prepared.getModifiedBy());
		assertEquals(createdBy, prepared.getCreatedBy());
		assertEquals(requestId, prepared.getId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetWithNullUserInfo() {
		manager.getUserOwnCurrentRequest(null, accessRequirementId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetWithNullAccessRequirementId() {
		manager.getUserOwnCurrentRequest(mockUser, null);
	}

	@Test (expected = NotFoundException.class)
	public void testGetNotFound() {
		when(mockRequestDao.getUserOwnCurrentRequest(anyString(), anyString())).thenThrow(new NotFoundException());
		manager.getUserOwnCurrentRequest(mockUser, accessRequirementId);
	}

	@Test
	public void testGet() {
		assertEquals(request, manager.getUserOwnCurrentRequest(mockUser, accessRequirementId));
		verify(mockRequestDao).getUserOwnCurrentRequest(accessRequirementId, userId);
	}

	@Test
	public void testGetForUpdateNotFound() {
		when(mockRequestDao.getUserOwnCurrentRequest(accessRequirementId, userId)).thenThrow(new NotFoundException());
		request = (Request) manager.getRequestForUpdate(mockUser, accessRequirementId);
		assertNotNull(request);
		assertEquals(accessRequirementId, request.getAccessRequirementId());
		assertEquals(Request.class.getName(), request.getConcreteType());
	}

	@Test
	public void testGetForUpdateNotRequireRenewal() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(false);
		assertEquals(request, manager.getRequestForUpdate(mockUser, accessRequirementId));
	}

	@Test
	public void testGetForUpdateRequireRenewalDoesNotHasApprovedSubmission() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.APPROVED)).thenReturn(false);
		assertEquals(request, manager.getRequestForUpdate(mockUser, accessRequirementId));
	}

	@Test
	public void testGetForUpdateRequireRenewalHasApprovedSubmission() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.APPROVED)).thenReturn(true);
		Renewal renewal = (Renewal) manager.getRequestForUpdate(mockUser, accessRequirementId);
		assertEquals(requestId, renewal.getId());
		assertEquals(userId, renewal.getCreatedBy());
		assertEquals(createdOn, renewal.getCreatedOn());
		assertEquals(userId, renewal.getModifiedBy());
		assertEquals(modifiedOn, renewal.getModifiedOn());
		assertEquals(etag, renewal.getEtag());
		assertEquals(accessRequirementId, renewal.getAccessRequirementId());
		assertEquals(researchProjectId, renewal.getResearchProjectId());
		assertEquals(request.getDucFileHandleId(), renewal.getDucFileHandleId());
		assertEquals(request.getIrbFileHandleId(), renewal.getIrbFileHandleId());
		assertEquals(request.getAttachments(), renewal.getAttachments());
		assertEquals(request.getAccessors(), renewal.getAccessors());
		assertNull(renewal.getSummaryOfUse());
		assertNull(renewal.getPublication());
	}

	@Test
	public void testGetForUpdateAlreadyHasRenewal() {
		when(mockRequestDao.getUserOwnCurrentRequest(accessRequirementId, userId)).thenReturn(renewal);
		assertEquals(renewal, manager.getRequestForUpdate(mockUser, accessRequirementId));
		verifyZeroInteractions(mockAccessRequirement);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullUserInfo() {
		manager.update(null, createNewRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdatWeithNullRequest() {
		manager.update(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullAccessRequirementId() {
		Request toUpdate = createNewRequest();
		toUpdate.setAccessRequirementId(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullResearchProjectId() {
		Request toUpdate = createNewRequest();
		toUpdate.setResearchProjectId(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateNotFound() {
		Request toUpdate = createNewRequest();
		when(mockRequestDao.getForUpdate(anyString())).thenThrow(new NotFoundException());
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateResearchProjectId() {
		Request toUpdate = createNewRequest();
		toUpdate.setResearchProjectId("222");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateCreatedBy() {
		Request toUpdate = createNewRequest();
		toUpdate.setCreatedBy("333");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateCreatedOn() {
		Request toUpdate = createNewRequest();
		toUpdate.setCreatedOn(new Date(0L));
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateAccessRequirementId() {
		Request toUpdate = createNewRequest();
		toUpdate.setAccessRequirementId("444");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = ConflictingUpdateException.class)
	public void testUpdateWithOutdatedEtag() {
		Request toUpdate = createNewRequest();
		toUpdate.setEtag("oldEtag");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateUnauthorized() {
		Request toUpdate = createNewRequest();
		when(mockUser.getId()).thenReturn(555L);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithSubmittedSubmission() {
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.SUBMITTED)).thenReturn(true);
		manager.update(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateRenewalNotRequired() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(false);
		Renewal toUpdate = manager.createRenewalFromRequest(request);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateRenewalRequiredAndHasApprovedSubmission() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.APPROVED)).thenReturn(true);
		manager.update(mockUser, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateRenewalRequiredAndDoesNotHasApprovedSubmission() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.APPROVED)).thenReturn(false);
		Renewal toUpdate = manager.createRenewalFromRequest(request);
		manager.update(mockUser, toUpdate);
	}

	@Test
	public void testUpdate() {
		Request toUpdate = createNewRequest();
		toUpdate.setDucFileHandleId("777");
		assertEquals(request, manager.update(mockUser, toUpdate));
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(mockRequestDao).update(captor.capture());
		Request updated = captor.getValue();
		assertEquals(requestId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals("777", updated.getDucFileHandleId());
	}

	@Test
	public void testUpdateRenewalRequired() {
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.APPROVED)).thenReturn(true);
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		Renewal toUpdate = manager.createRenewalFromRequest(request);
		toUpdate.setDucFileHandleId("777");
		assertEquals(request, manager.update(mockUser, toUpdate));
		ArgumentCaptor<Renewal> captor = ArgumentCaptor.forClass(Renewal.class);
		verify(mockRequestDao).update(captor.capture());
		Renewal updated = captor.getValue();
		assertEquals(requestId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals("777", updated.getDucFileHandleId());
	}

	@Test
	public void testCreateRenewalFromRequest() {
		Request request = createNewRequest();
		request.setDucFileHandleId("ducFileHandleId");
		Renewal renewal = manager.createRenewalFromRequest(request);
		assertEquals(requestId, renewal.getId());
		assertEquals(userId, renewal.getCreatedBy());
		assertEquals(createdOn, renewal.getCreatedOn());
		assertEquals(userId, renewal.getModifiedBy());
		assertEquals(modifiedOn, renewal.getModifiedOn());
		assertEquals(etag, renewal.getEtag());
		assertEquals(accessRequirementId, renewal.getAccessRequirementId());
		assertEquals(researchProjectId, renewal.getResearchProjectId());
		assertEquals(request.getDucFileHandleId(), renewal.getDucFileHandleId());
		assertEquals(request.getIrbFileHandleId(), renewal.getIrbFileHandleId());
		assertEquals(request.getAttachments(), renewal.getAttachments());
		assertEquals(request.getAccessors(), renewal.getAccessors());
		assertNull(renewal.getSummaryOfUse());
		assertNull(renewal.getPublication());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateOrUpdateWithNullRequest() {
		manager.createOrUpdate(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateOrUpdateWithRenewalNullId() {
		manager.createOrUpdate(mockUser, new Renewal());
	}

	@Test
	public void testCreateOrUpdateWithNullId() {
		request.setId(null);
		assertEquals(request, manager.createOrUpdate(mockUser, request));
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(mockRequestDao).create(captor.capture());
		Request toCreate = captor.getValue();
		assertEquals(userId, toCreate.getCreatedBy());
		assertEquals(userId, toCreate.getModifiedBy());
	}

	@Test
	public void testCreateOrUpdateWithId() {
		Request toUpdate = createNewRequest();
		toUpdate.setDucFileHandleId("777");
		assertEquals(request, manager.createOrUpdate(mockUser, toUpdate));
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(mockRequestDao).update(captor.capture());
		Request updated = captor.getValue();
		assertEquals(requestId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals("777", updated.getDucFileHandleId());
	}
}
