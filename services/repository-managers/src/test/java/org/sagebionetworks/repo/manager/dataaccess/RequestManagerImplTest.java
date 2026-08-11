package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.docusign.EnvelopeStatusResult;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestList;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestListRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestStatusEnum;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestSummary;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUserInfo;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Signer;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
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
	private FileHandleAuthorizationManager mockFileHandleAuthorizationManager;
	@Mock
	private DocuSignClient mockDocuSignClient;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ManagedACTAccessRequirement mockAccessRequirement;
	
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
		verifyNoMoreInteractions(mockAccessRequirement);
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
		when(mockFileHandleAuthorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());

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
		when(mockFileHandleAuthorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());

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

	@Test
	public void testValidateRequestWithInvalidPIEmail() {
		Request toValidate = createNewRequest();
		PrincipalInvestigator pi = new PrincipalInvestigator();
		pi.setInstitutionalEmail("not-an-email");
		toValidate.setPrincipalInvestigator(pi);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.validateRequest(toValidate));
	}

	@Test
	public void testValidateRequestWithInvalidSOEmail() {
		Request toValidate = createNewRequest();
		SigningOfficial so = new SigningOfficial();
		so.setInstitutionalEmail("also not valid");
		toValidate.setSigningOfficial(so);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.validateRequest(toValidate));
	}

	@Test
	public void testValidateRequestWithValidEmails() {
		Request toValidate = createNewRequest();
		PrincipalInvestigator pi = new PrincipalInvestigator();
		pi.setInstitutionalEmail("pi@university.edu");
		toValidate.setPrincipalInvestigator(pi);
		SigningOfficial so = new SigningOfficial();
		so.setInstitutionalEmail("so@institution.org");
		toValidate.setSigningOfficial(so);

		// call under test — should not throw
		manager.validateRequest(toValidate);
	}

	@Test
	public void testValidateRequestWithNullEmails() {
		Request toValidate = createNewRequest();
		PrincipalInvestigator pi = new PrincipalInvestigator();
		pi.setInstitutionalEmail(null);
		toValidate.setPrincipalInvestigator(pi);
		SigningOfficial so = new SigningOfficial();
		so.setInstitutionalEmail(null);
		toValidate.setSigningOfficial(so);

		// call under test — null emails should be allowed
		manager.validateRequest(toValidate);
	}

	@Test
	public void testCreateWithUnauthorizedDucFileHandle() {
		when(mockFileHandleAuthorizationManager.canAccessRawFileHandleById(mockUser, "fh-duc"))
				.thenReturn(AuthorizationStatus.accessDenied("Not the owner"));
		Request toCreate = createNewRequest();
		toCreate.setId(null);
		toCreate.setDucFileHandleId("fh-duc");

		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> manager.createOrUpdate(mockUser, toCreate));

		assertEquals("Not the owner", ex.getMessage());
	}

	@Test
	public void testUpdateWithUnauthorizedIrbFileHandle() {
		when(mockFileHandleAuthorizationManager.canAccessRawFileHandleById(mockUser, "fh-irb"))
				.thenReturn(AuthorizationStatus.accessDenied("Not the owner"));
		Request toUpdate = createNewRequest();
		toUpdate.setIrbFileHandleId("fh-irb");

		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> manager.update(mockUser, toUpdate));

		assertEquals("Not the owner", ex.getMessage());
	}

	@Test
	public void testValidateEnvelopeCompletionWithCompletedEnvelope() {
		Request request = createNewRequest();
		request.setDucFileHandleId("fh-duc");
		request.setEDucSignatureEnvelopeId("env-1");
		EDucSignatureStatus status = new EDucSignatureStatus();
		status.setDucStatus(EDucStatusEnum.completed);
		when(mockDocuSignClient.getEnvelopeStatus("env-1"))
				.thenReturn(new EnvelopeStatusResult(status, List.of()));

		// call under test — should not throw
		manager.validateEnvelopeCompletion(request);
	}

	@Test
	public void testValidateEnvelopeCompletionWithIncompleteEnvelope() {
		Request request = createNewRequest();
		request.setDucFileHandleId("fh-duc");
		request.setEDucSignatureEnvelopeId("env-1");
		EDucSignatureStatus status = new EDucSignatureStatus();
		status.setDucStatus(EDucStatusEnum.sent);
		when(mockDocuSignClient.getEnvelopeStatus("env-1"))
				.thenReturn(new EnvelopeStatusResult(status, List.of()));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.validateEnvelopeCompletion(request));

		assertEquals("Cannot set ducFileHandleId: the eDUC envelope has not been completed.", ex.getMessage());
	}

	@Test
	public void testValidateEnvelopeCompletionWithNoDucFileHandle() {
		Request request = createNewRequest();
		request.setDucFileHandleId(null);
		request.setEDucSignatureEnvelopeId("env-1");

		// call under test — should not throw, no ducFileHandleId means nothing to validate
		manager.validateEnvelopeCompletion(request);
	}

	@Test
	public void testValidateEnvelopeCompletionWithNoEnvelope() {
		Request request = createNewRequest();
		request.setDucFileHandleId("fh-duc");
		request.setEDucSignatureEnvelopeId(null);

		// call under test — should not throw, traditional (non-eDUC) flow
		manager.validateEnvelopeCompletion(request);
	}

	@Test
	public void testGetRequestForSubmission() {
		when(mockRequestDao.get(requestId)).thenReturn(request);

		// call under test
		RequestInterface result = manager.getRequestForSubmission(requestId);

		assertEquals(request, result);
		verify(mockRequestDao).get(requestId);
	}

	@Test
	public void testGetRequestForSubmissionWithNullId() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getRequestForSubmission(null));
	}

	@Test
	public void testToAccessRequestStatus() {
		assertEquals(AccessRequestStatusEnum.submitted, RequestManagerImpl.toAccessRequestStatus(SubmissionState.SUBMITTED));
		assertEquals(AccessRequestStatusEnum.approved, RequestManagerImpl.toAccessRequestStatus(SubmissionState.APPROVED));
		assertEquals(AccessRequestStatusEnum.rejected, RequestManagerImpl.toAccessRequestStatus(SubmissionState.REJECTED));
		assertEquals(AccessRequestStatusEnum.cancelled, RequestManagerImpl.toAccessRequestStatus(SubmissionState.CANCELLED));
	}

	@Test
	public void testToAccessRequestStatusFromEnvelope() {
		assertEquals(AccessRequestStatusEnum.draft, RequestManagerImpl.toAccessRequestStatusFromEnvelope("created"));
		assertEquals(AccessRequestStatusEnum.sent, RequestManagerImpl.toAccessRequestStatusFromEnvelope("sent"));
		assertEquals(AccessRequestStatusEnum.delivered, RequestManagerImpl.toAccessRequestStatusFromEnvelope("delivered"));
		assertEquals(AccessRequestStatusEnum.completed, RequestManagerImpl.toAccessRequestStatusFromEnvelope("completed"));
		assertEquals(AccessRequestStatusEnum.completed, RequestManagerImpl.toAccessRequestStatusFromEnvelope("signed"));
		assertEquals(AccessRequestStatusEnum.declined, RequestManagerImpl.toAccessRequestStatusFromEnvelope("declined"));
		assertEquals(AccessRequestStatusEnum.voided, RequestManagerImpl.toAccessRequestStatusFromEnvelope("voided"));
		assertEquals(AccessRequestStatusEnum.correct, RequestManagerImpl.toAccessRequestStatusFromEnvelope("correct"));
	}

	@Test
	public void testToAccessRequestStatusFromEnvelopeWithUnexpectedValue() {
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> RequestManagerImpl.toAccessRequestStatusFromEnvelope("bogus"));

		assertEquals("Unexpected status bogus", ex.getMessage());
	}

	@Test
	public void testToAccessRequestStatusFromEnvelopeWithNull() {
		// call under test — toEDucStatusEnum(null) returns null, valueOf(null) throws NPE
		assertThrows(NullPointerException.class,
				() -> RequestManagerImpl.toAccessRequestStatusFromEnvelope(null));
	}

	@Test
	public void testListUserRequestsWithNoRequests() {
		when(mockUser.getId()).thenReturn(1L);
		when(mockRequestDao.getUserRequests(1L, 51L, 0L, null, null)).thenReturn(List.of());

		AccessRequestListRequest listRequest = new AccessRequestListRequest();

		// call under test
		AccessRequestList result = manager.listUserRequests(mockUser, listRequest);

		assertNotNull(result);
		assertEquals(0, result.getResults().size());
		assertNull(result.getNextPageToken());
	}

	@Test
	public void testListUserRequestsWithSubmittedRequest() {
		when(mockUser.getId()).thenReturn(1L);

		RequestUserInfo info = new RequestUserInfo();
		info.setRequestId("100");
		info.setAccessRequirementName("AR Name");
		info.setSubmissionStatus(SubmissionState.APPROVED);
		info.setEnvelopeId(null);
		info.setSubmittedOn(new Date(1000L));
		info.setModifiedOn(new Date(2000L));

		when(mockRequestDao.getUserRequests(1L, 51L, 0L, null, null)).thenReturn(List.of(info));

		AccessRequestListRequest listRequest = new AccessRequestListRequest();

		// call under test
		AccessRequestList result = manager.listUserRequests(mockUser, listRequest);

		assertEquals(1, result.getResults().size());
		AccessRequestSummary summary = result.getResults().get(0);
		assertEquals("100", summary.getRequestId());
		assertEquals("AR Name", summary.getAccessRequirementName());
		assertEquals(AccessRequestStatusEnum.approved, summary.getStatus());
		assertEquals(false, summary.getIsEDuc());
		assertNull(summary.getSignaturesRequested());
		assertNull(summary.getSignaturesAcquired());
		assertEquals(new Date(1000L), summary.getSubmittedOn());
		assertEquals(new Date(2000L), summary.getModifiedOn());
	}

	@Test
	public void testListUserRequestsWithEDucEnvelope() {
		when(mockUser.getId()).thenReturn(1L);

		RequestUserInfo info = new RequestUserInfo();
		info.setRequestId("200");
		info.setAccessRequirementName("AR2");
		info.setSubmissionStatus(null);
		info.setEnvelopeId("env-abc");

		when(mockRequestDao.getUserRequests(1L, 51L, 0L, null, null)).thenReturn(List.of(info));

		Signer signer1 = new Signer();
		signer1.setStatus("completed");
		Signer signer2 = new Signer();
		signer2.setStatus("sent");
		Signer signer3 = new Signer();
		signer3.setStatus("signed");

		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(signer1, signer2, signer3));

		Envelope envelope = new Envelope();
		envelope.setEnvelopeId("env-abc");
		envelope.setStatus("sent");
		envelope.setRecipients(recipients);

		when(mockDocuSignClient.listEnvelopeStatuses(List.of("env-abc"))).thenReturn(List.of(envelope));

		AccessRequestListRequest listRequest = new AccessRequestListRequest();

		// call under test
		AccessRequestList result = manager.listUserRequests(mockUser, listRequest);

		assertEquals(1, result.getResults().size());
		AccessRequestSummary summary = result.getResults().get(0);
		assertEquals("200", summary.getRequestId());
		assertEquals(AccessRequestStatusEnum.sent, summary.getStatus());
		assertEquals(true, summary.getIsEDuc());
		assertEquals(Long.valueOf(3), summary.getSignaturesRequested());
		assertEquals(Long.valueOf(2), summary.getSignaturesAcquired());
	}

	@Test
	public void testListUserRequestsWithNoSubmissionAndNoEnvelope() {
		when(mockUser.getId()).thenReturn(1L);

		RequestUserInfo info = new RequestUserInfo();
		info.setRequestId("300");
		info.setAccessRequirementName("AR3");
		info.setSubmissionStatus(null);
		info.setEnvelopeId(null);

		when(mockRequestDao.getUserRequests(1L, 51L, 0L, null, null)).thenReturn(List.of(info));

		AccessRequestListRequest listRequest = new AccessRequestListRequest();

		// call under test
		AccessRequestList result = manager.listUserRequests(mockUser, listRequest);

		assertEquals(1, result.getResults().size());
		AccessRequestSummary summary = result.getResults().get(0);
		assertEquals("300", summary.getRequestId());
		assertEquals(AccessRequestStatusEnum.created, summary.getStatus());
		assertEquals(false, summary.getIsEDuc());
		assertNull(summary.getSignaturesRequested());
		assertNull(summary.getSignaturesAcquired());
	}

	@Test
	public void testListUserRequestsWithNullUserInfo() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.listUserRequests(null, new AccessRequestListRequest()));
	}

	@Test
	public void testListUserRequestsWithNullRequest() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.listUserRequests(mockUser, null));
	}
}
