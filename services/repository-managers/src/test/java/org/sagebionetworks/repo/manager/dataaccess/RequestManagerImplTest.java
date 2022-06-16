package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
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
	private ManagedACTAccessRequirement mockAccessRequirement;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	
	@InjectMocks
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


	@BeforeEach
	public void before() {
		userId = "1";
		accessRequirementId = "2";
		researchProjectId = "3";
		requestId = "4";
		createdOn = new Date();
		modifiedOn = new Date();
		etag = "etag";
		request = createNewRequest();
		renewal = manager.createRenewalFromApprovedRequest(request);

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
		dto.setAccessorChanges(new LinkedList<AccessorChange>());
		return dto;
	}

	@Test
	public void testCreateWithNullUserInfo() {
		assertThrows(IllegalArgumentException.class, ()->{
			manager.create(null, createNewRequest());
		});
	}

	@Test
	public void testCreateWithNullRequest() {
		assertThrows(IllegalArgumentException.class, ()->{
			manager.create(mockUser, null);
		});
	}

	@Test
	public void testCreateWithNullAccessRequirementId() {
		Request toCreate = createNewRequest();
		toCreate.setAccessRequirementId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			manager.create(mockUser, toCreate);
		});

	}

	@Test
	public void testCreateWithNullResearchProjectId() {
		Request toCreate = createNewRequest();
		toCreate.setResearchProjectId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			manager.create(mockUser, toCreate);
		});
	}

	@Test
	public void testCreateWithNotACTAccessRequirementId() {
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		assertThrows(IllegalArgumentException.class, ()->{
			manager.create(mockUser, createNewRequest());
		});
	}

	@Test
	public void testCreateWithMoreThanMaxAccessorChanges() {
		request = createNewRequest();
		List<AccessorChange> mockAccessorChanges = Mockito.mock(List.class);
		when(mockAccessorChanges.isEmpty()).thenReturn(false);
		when(mockAccessorChanges.size()).thenReturn(RequestManagerImpl.MAX_ACCESSORS+1);
		request.setAccessorChanges(mockAccessorChanges );
		assertThrows(IllegalArgumentException.class, ()->{
			manager.create(mockUser, request);
		});
	}

	@Test
	public void testCreate() {
		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.create(any(Request.class))).thenReturn(request);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		
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

	@Test
	public void testGetRequestForUpdateWithNullUserInfo() {

		assertThrows(IllegalArgumentException.class, ()->{
			manager.getRequestForUpdate(null, accessRequirementId);
		});
	}

	@Test
	public void testGetRequestForUpdateWithNullAccessRequirementId() {
		assertThrows(IllegalArgumentException.class, ()->{
			manager.getRequestForUpdate(mockUser, null);
		});
	}

	@Test
	public void testGetRequestForUpdate() {
		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.getUserOwnCurrentRequest(accessRequirementId, userId)).thenReturn(request);
	
		assertEquals(request, manager.getRequestForUpdate(mockUser, accessRequirementId));
		verify(mockRequestDao).getUserOwnCurrentRequest(accessRequirementId, userId);
	}

	@Test
	public void testGetForUpdateNotFound() {
		when(mockRequestDao.getUserOwnCurrentRequest(any(), any())).thenThrow(new NotFoundException(""));
		request = (Request) manager.getRequestForUpdate(mockUser, accessRequirementId);
		assertNotNull(request);
		assertEquals(accessRequirementId, request.getAccessRequirementId());
		assertEquals(Request.class.getName(), request.getConcreteType());
	}

	/**
	 * For this case the current request is a request (not a renewal).
	 */
	@Test
	public void testUpdateApprovedRequestCurrentRequest() {
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		// call under test
		manager.updateApprovedRequest(requestId);
		verify(mockRequestDao).getForUpdate(requestId);
		ArgumentCaptor<RequestInterface> updateCapture = ArgumentCaptor.forClass(RequestInterface.class);
		verify(mockRequestDao).update(updateCapture.capture());
		
		RequestInterface requesInt = updateCapture.getValue();
		assertNotNull(requesInt);
		assertTrue(requesInt instanceof Renewal);
		Renewal renewal = (Renewal) requesInt;
		assertEquals(requestId, renewal.getId());
		assertEquals(userId, renewal.getCreatedBy());
		assertEquals(createdOn, renewal.getCreatedOn());
		assertEquals(userId, renewal.getModifiedBy());
		// modified on should not be changed.
		assertEquals(modifiedOn, renewal.getModifiedOn());
		assertEquals(etag, renewal.getEtag());
		assertEquals(accessRequirementId, renewal.getAccessRequirementId());
		assertEquals(researchProjectId, renewal.getResearchProjectId());
		assertEquals(request.getDucFileHandleId(), renewal.getDucFileHandleId());
		assertEquals(request.getIrbFileHandleId(), renewal.getIrbFileHandleId());
		assertEquals(request.getAttachments(), renewal.getAttachments());
		assertNull(renewal.getSummaryOfUse());
		assertNull(renewal.getPublication());
	}
	
	/**
	 * For this case the current request is a renewal.
	 */
	@Test
	public void testUpdateApprovedRequestCurrentRenewal() {
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(renewal);
		// call under test
		manager.updateApprovedRequest(requestId);
		verify(mockRequestDao).getForUpdate(requestId);
		ArgumentCaptor<RequestInterface> updateCapture = ArgumentCaptor.forClass(RequestInterface.class);
		verify(mockRequestDao).update(updateCapture.capture());
		
		RequestInterface requesInt = updateCapture.getValue();
		assertNotNull(requesInt);
		assertTrue(requesInt instanceof Renewal);
	}

	@Test
	public void testGetForUpdateAlreadyHasRenewal() {
		when(mockRequestDao.getUserOwnCurrentRequest(any(), any())).thenReturn(renewal);
		assertEquals(renewal, manager.getRequestForUpdate(mockUser, accessRequirementId));
		verifyZeroInteractions(mockAccessRequirement);
	}

	@Test
	public void testUpdateWithNullUserInfo() {
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(null, createNewRequest());
		});
	}

	@Test
	public void testUpdatWeithNullRequest() {
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, null);
		});
	}

	@Test
	public void testUpdateWithNullAccessRequirementId() {
		Request toUpdate = createNewRequest();
		toUpdate.setAccessRequirementId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateWithNullResearchProjectId() {
		Request toUpdate = createNewRequest();
		toUpdate.setResearchProjectId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateNotFound() {
		Request toUpdate = createNewRequest();
		when(mockRequestDao.getForUpdate(anyString())).thenThrow(new NotFoundException(""));
		assertThrows(NotFoundException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateResearchProjectId() {
		
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		
		Request toUpdate = createNewRequest();
		toUpdate.setResearchProjectId("222");
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateCreatedBy() {
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		
		Request toUpdate = createNewRequest();
		toUpdate.setCreatedBy("333");
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateCreatedOn() {
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		
		Request toUpdate = createNewRequest();
		toUpdate.setCreatedOn(new Date(0L));
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateAccessRequirementId() {
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		
		Request toUpdate = createNewRequest();
		toUpdate.setAccessRequirementId("444");
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateWithOutdatedEtag() {
		
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		
		Request toUpdate = createNewRequest();
		toUpdate.setEtag("oldEtag");
		assertThrows(ConflictingUpdateException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateUnauthorized() {
		
		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		
		Request toUpdate = createNewRequest();
		when(mockUser.getId()).thenReturn(555L);
		assertThrows(UnauthorizedException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdateWithSubmittedSubmission() {
		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);

		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.SUBMITTED)).thenReturn(true);
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, request);
		});
	}

	@Test
	public void testUpdateWithMoreThanMaxAccessorChanges() {
		Renewal toUpdate = RequestManagerImpl.createRenewalFromApprovedRequest(request);
		List<AccessorChange> mockAccessorChanges = Mockito.mock(List.class);
		when(mockAccessorChanges.isEmpty()).thenReturn(false);
		when(mockAccessorChanges.size()).thenReturn(RequestManagerImpl.MAX_ACCESSORS+1);
		toUpdate.setAccessorChanges(mockAccessorChanges );
		assertThrows(IllegalArgumentException.class, ()->{
			manager.update(mockUser, toUpdate);
		});
	}

	@Test
	public void testUpdate() {
		
		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		when(mockRequestDao.update(any())).thenReturn(request);
		
		when(mockSubmissionDao.hasSubmissionWithState(any(), any(), any())).thenReturn(false);
		Renewal toUpdate = RequestManagerImpl.createRenewalFromApprovedRequest(request);
		toUpdate.setDucFileHandleId("777");
		// call under test.
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
	public void testCreateRenewalFromApprovedRequest() {
		Request request = createNewRequest();
		AccessorChange change1 = new AccessorChange();
		change1.setUserId("1");
		change1.setType(AccessType.GAIN_ACCESS);
		AccessorChange change2 = new AccessorChange();
		change2.setUserId("2");
		change2.setType(AccessType.RENEW_ACCESS);
		AccessorChange change3 = new AccessorChange();
		change3.setUserId("3");
		change3.setType(AccessType.REVOKE_ACCESS);
		request.setAccessorChanges(Arrays.asList(change1, change2, change3));
		request.setDucFileHandleId("ducFileHandleId");
		Renewal renewal = RequestManagerImpl.createRenewalFromApprovedRequest(request);
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
		assertNull(renewal.getSummaryOfUse());
		assertNull(renewal.getPublication());
		change1.setType(AccessType.RENEW_ACCESS);
		assertEquals(renewal.getAccessorChanges(), Arrays.asList(change1, change2));
	}

	@Test
	public void testCreateOrUpdateWithNullRequest() {
		assertThrows(IllegalArgumentException.class, ()->{
			manager.createOrUpdate(mockUser, null);
		});
	}

	@Test
	public void testCreateOrUpdateWithRenewalNullId() {
		assertThrows(IllegalArgumentException.class, ()->{
			manager.createOrUpdate(mockUser, new Renewal());
		});
	}

	@Test
	public void testCreateOrUpdateWithNullId() {
		
		when(mockUser.getId()).thenReturn(1L);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		Request daoRequest = createNewRequest();
		when(mockRequestDao.create(any())).thenReturn(daoRequest);
		
		request = createNewRequest();
		request.setId(null);
		assertEquals(daoRequest, manager.createOrUpdate(mockUser, request));
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(mockRequestDao).create(captor.capture());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(requestId, ObjectType.DATA_ACCESS_REQUEST, ChangeType.CREATE);
		Request toCreate = captor.getValue();
		assertEquals(userId, toCreate.getCreatedBy());
		assertEquals(userId, toCreate.getModifiedBy());
	}

	@Test
	public void testCreateOrUpdateWithId() {
		
		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.getForUpdate(requestId)).thenReturn(request);
		when(mockRequestDao.update(any(RequestInterface.class))).thenReturn(request);
		when(mockSubmissionDao.hasSubmissionWithState(userId, accessRequirementId, SubmissionState.SUBMITTED)).thenReturn(false);
		
		Request toUpdate = createNewRequest();
		toUpdate.setDucFileHandleId("777");
		assertEquals(request, manager.createOrUpdate(mockUser, toUpdate));
		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(mockRequestDao).update(captor.capture());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(requestId, ObjectType.DATA_ACCESS_REQUEST, ChangeType.UPDATE);
		Request updated = captor.getValue();
		assertEquals(requestId, updated.getId());
		assertEquals(userId, updated.getCreatedBy());
		assertEquals(userId, updated.getModifiedBy());
		assertEquals("777", updated.getDucFileHandleId());
	}
}
