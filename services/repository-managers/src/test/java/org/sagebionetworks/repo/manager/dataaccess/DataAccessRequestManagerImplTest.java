package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class DataAccessRequestManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private DataAccessRequestDAO mockDataAccessRequestDao;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ACTAccessRequirement mockAccessRequirement;

	private DataAccessRequestManagerImpl manager;
	private String accessRequirementId;
	private String userId;
	private String researchProjectId;
	private String requestId;
	private Date createdOn;
	private Date modifiedOn;
	private String etag;
	private DataAccessRequest request;
	private DataAccessRenewal renewal;


	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		manager = new DataAccessRequestManagerImpl();
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(manager, "accessRequirementDao", mockAccessRequirementDao);
		ReflectionTestUtils.setField(manager, "dataAccessRequestDao", mockDataAccessRequestDao);

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
		when(mockIdGenerator.generateNewId(TYPE.DATA_ACCESS_REQUEST_ID)).thenReturn(4L);
		when(mockDataAccessRequestDao.create(any(DataAccessRequest.class))).thenReturn(request);
		when(mockDataAccessRequestDao.getUserOwnCurrentRequest(accessRequirementId, userId)).thenReturn(request);
		when(mockDataAccessRequestDao.getForUpdate(requestId)).thenReturn(request);
		when(mockDataAccessRequestDao.update(any(DataAccessRequestInterface.class))).thenReturn(request);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
	}

	private DataAccessRequest createNewRequest() {
		DataAccessRequest dto = new DataAccessRequest();
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
	public void testCreateWithNullDataAccessRequest() {
		manager.create(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullAccessRequirementId() {
		DataAccessRequest toCreate = createNewRequest();
		toCreate.setAccessRequirementId(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullResearchProjectId() {
		DataAccessRequest toCreate = createNewRequest();
		toCreate.setResearchProjectId(null);
		manager.create(mockUser, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNotACTAccessRequirementId() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		manager.create(null, createNewRequest());
	}

	@Test
	public void testCreate() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new ACTAccessRequirement());
		assertEquals(request, manager.create(mockUser, createNewRequest()));
		ArgumentCaptor<DataAccessRequest> captor = ArgumentCaptor.forClass(DataAccessRequest.class);
		verify(mockDataAccessRequestDao).create(captor.capture());
		DataAccessRequest toCreate = captor.getValue();
		assertEquals(requestId, toCreate.getId());
		assertEquals(userId, toCreate.getCreatedBy());
		assertEquals(userId, toCreate.getModifiedBy());
	}

	@Test
	public void testPrepareUpdateFields() {
		String modifiedBy = "111";
		DataAccessRequest prepared = (DataAccessRequest) manager.prepareUpdateFields(request, modifiedBy);
		assertEquals(modifiedBy, prepared.getModifiedBy());
		assertFalse(prepared.getEtag().equals(etag));
	}

	@Test
	public void testPrepareCreationFields() {
		String createdBy = "222";
		DataAccessRequest prepared = (DataAccessRequest) manager.prepareCreationFields(request, createdBy);
		assertEquals(createdBy, prepared.getModifiedBy());
		assertEquals(createdBy, prepared.getCreatedBy());
		assertFalse(prepared.getEtag().equals(etag));
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
		when(mockDataAccessRequestDao.getUserOwnCurrentRequest(anyString(), anyString())).thenThrow(new NotFoundException());
		manager.getUserOwnCurrentRequest(mockUser, accessRequirementId);
	}

	@Test
	public void testGet() {
		assertEquals(request, manager.getUserOwnCurrentRequest(mockUser, accessRequirementId));
		verify(mockDataAccessRequestDao).getUserOwnCurrentRequest(accessRequirementId, userId);
	}

	@Test
	public void testGetForUpdateNotFound() {
		when(mockDataAccessRequestDao.getUserOwnCurrentRequest(accessRequirementId, userId)).thenThrow(new NotFoundException());
		request = (DataAccessRequest) manager.getDataAccessRequestForUpdate(mockUser, accessRequirementId);
		assertNotNull(request);
		assertEquals(accessRequirementId, request.getAccessRequirementId());
		assertEquals(DataAccessRequest.class.getName(), request.getConcreteType());
	}

	@Test
	public void testGetForUpdateNotRequireRenewal() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(false);
		assertEquals(request, manager.getDataAccessRequestForUpdate(mockUser, accessRequirementId));
	}

	@Test
	public void testGetForUpdateRequireRenewal() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		DataAccessRenewal renewal = (DataAccessRenewal) manager.getDataAccessRequestForUpdate(mockUser, accessRequirementId);
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
		when(mockDataAccessRequestDao.getUserOwnCurrentRequest(accessRequirementId, userId)).thenReturn(renewal);
		assertEquals(renewal, manager.getDataAccessRequestForUpdate(mockUser, accessRequirementId));
		verifyZeroInteractions(mockAccessRequirement);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullUserInfo() {
		manager.update(null, createNewRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdatWeithNullDataAccessRequest() {
		manager.update(mockUser, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullAccessRequirementId() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setAccessRequirementId(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullResearchProjectId() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setResearchProjectId(null);
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateNotFound() {
		DataAccessRequest toUpdate = createNewRequest();
		when(mockDataAccessRequestDao.getForUpdate(anyString())).thenThrow(new NotFoundException());
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateResearchProjectId() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setResearchProjectId("222");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateCreatedBy() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setCreatedBy("333");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateCreatedOn() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setCreatedOn(new Date(0L));
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateAccessRequirementId() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setAccessRequirementId("444");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = ConflictingUpdateException.class)
	public void testUpdateWithOutdatedEtag() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setEtag("oldEtag");
		manager.update(mockUser, toUpdate);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateUnauthorized() {
		DataAccessRequest toUpdate = createNewRequest();
		when(mockUser.getId()).thenReturn(555L);
		manager.update(mockUser, toUpdate);
	}

	@Test
	public void testUpdate() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setDucFileHandleId("777");
		assertEquals(request, manager.update(mockUser, toUpdate));
		ArgumentCaptor<DataAccessRequest> captor = ArgumentCaptor.forClass(DataAccessRequest.class);
		verify(mockDataAccessRequestDao).update(captor.capture());
		DataAccessRequest updated = captor.getValue();
		assertEquals(requestId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals("777", updated.getDucFileHandleId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateDataAccessRenewalNotRequired() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(false);
		DataAccessRenewal toUpdate = manager.createRenewalFromRequest(request);
		manager.update(mockUser, toUpdate);
	}

	@Test
	public void testUpdateDataAccessRenewalRequired() {
		when(mockAccessRequirement.getIsAnnualReviewRequired()).thenReturn(true);
		DataAccessRenewal toUpdate = manager.createRenewalFromRequest(request);
		toUpdate.setDucFileHandleId("777");
		assertEquals(request, manager.update(mockUser, toUpdate));
		ArgumentCaptor<DataAccessRenewal> captor = ArgumentCaptor.forClass(DataAccessRenewal.class);
		verify(mockDataAccessRequestDao).update(captor.capture());
		DataAccessRenewal updated = captor.getValue();
		assertEquals(requestId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals("777", updated.getDucFileHandleId());
	}

	@Test
	public void testCreateRenewalFromRequest() {
		DataAccessRequest request = createNewRequest();
		request.setDucFileHandleId("ducFileHandleId");
		DataAccessRenewal renewal = manager.createRenewalFromRequest(request);
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
	public void testCreateOrUpdateWithDataAccessRenewalNullId() {
		manager.createOrUpdate(mockUser, new DataAccessRenewal());
	}

	@Test
	public void testCreateOrUpdateWithNullId() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new ACTAccessRequirement());
		request.setId(null);
		assertEquals(request, manager.createOrUpdate(mockUser, request));
		ArgumentCaptor<DataAccessRequest> captor = ArgumentCaptor.forClass(DataAccessRequest.class);
		verify(mockDataAccessRequestDao).create(captor.capture());
		DataAccessRequest toCreate = captor.getValue();
		assertEquals(requestId, toCreate.getId());
		assertEquals(userId, toCreate.getCreatedBy());
		assertEquals(userId, toCreate.getModifiedBy());
	}

	@Test
	public void testCreateOrUpdateWithId() {
		DataAccessRequest toUpdate = createNewRequest();
		toUpdate.setDucFileHandleId("777");
		assertEquals(request, manager.createOrUpdate(mockUser, toUpdate));
		ArgumentCaptor<DataAccessRequest> captor = ArgumentCaptor.forClass(DataAccessRequest.class);
		verify(mockDataAccessRequestDao).update(captor.capture());
		DataAccessRequest updated = captor.getValue();
		assertEquals(requestId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals("777", updated.getDucFileHandleId());
	}
}
