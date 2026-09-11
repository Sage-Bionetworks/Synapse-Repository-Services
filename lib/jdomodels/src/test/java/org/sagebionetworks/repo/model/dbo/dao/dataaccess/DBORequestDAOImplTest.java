package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestSortField;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.SortDirection;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtilsTest;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBORequestDAOImplTest {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private AccessApprovalDAO accessApprovalDAO;

	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@Autowired
	private RequestDAO requestDao;

	@Autowired
	private SubmissionDAO submissionDao;

	@Autowired
	private TransactionTemplate readCommitedTransactionTemplate;

	private UserGroup individualGroup = null;
	private UserGroup otherUser = null;
	private Node node = null;
	private ManagedACTAccessRequirement accessRequirement = null;
	private ResearchProject researchProject = null;
	private String toDelete;
	private String submissionToDelete;

	// A second access requirement is needed to give one user two requests: the request table has a
	// unique key on (ACCESS_REQUIREMENT_ID, CREATED_BY).
	private ManagedACTAccessRequirement secondAccessRequirement = null;
	private ResearchProject secondResearchProject = null;
	private String secondRequestToDelete = null;

	@BeforeEach
	public void before() {
		toDelete = null;
		submissionToDelete = null;
		secondRequestToDelete = null;
		secondResearchProject = null;
		secondAccessRequirement = null;

		// create a user
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());

		otherUser = new UserGroup();
		otherUser.setIsIndividual(true);
		otherUser.setCreationDate(new Date());
		otherUser.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		otherUser.setId(userGroupDAO.create(otherUser).toString());

		// create a node
		node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
		node.setId(nodeDao.createNew(node));

		// create an ACTAccessRequirement
		accessRequirement = new ManagedACTAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod}));
		accessRequirement = accessRequirementDAO.create(accessRequirement);

		// create a ResearchProject
		researchProject = ResearchProjectTestUtils.createNewDto();
		researchProject.setAccessRequirementId(accessRequirement.getId().toString());
		researchProject = researchProjectDao.create(researchProject);
	}

	@AfterEach
	public void after() {
		if (submissionToDelete != null) {
			submissionDao.delete(submissionToDelete);
		}
		if (secondRequestToDelete != null) {
			requestDao.delete(secondRequestToDelete);
		}
		if (secondResearchProject != null) {
			researchProjectDao.delete(secondResearchProject.getId());
		}
		if (secondAccessRequirement != null) {
			accessRequirementDAO.delete(secondAccessRequirement.getId().toString());
		}
		if (toDelete != null) {
			requestDao.delete(toDelete);
		}
		if (researchProject != null) {
			researchProjectDao.delete(researchProject.getId());
		}
		if (accessRequirement != null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (node != null) {
			nodeDao.delete(node.getId());
			node = null;
		}
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
		if (otherUser != null) {
			userGroupDAO.delete(otherUser.getId());
		}
	}

	@Test
	public void testNotFound() {
		Request dto = RequestTestUtils.createNewRequest();
		String message = assertThrows(NotFoundException.class, () -> {			
			requestDao.getUserOwnCurrentRequest(dto.getAccessRequirementId(), dto.getCreatedBy());
		}).getMessage();
		
		assertEquals("Data access request does not exist for access requirement: '" + dto.getAccessRequirementId() + "' and user id: '"+ dto.getCreatedBy() +"'", message);
	}

	@Test
	public void testCRUD() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		dto.setId(created.getId());
		dto.setEtag(created.getEtag());
		assertEquals(dto, created);

		// should get back the same object
		assertEquals(dto, (Request) requestDao.getUserOwnCurrentRequest(
				dto.getAccessRequirementId(), dto.getCreatedBy()));
		assertEquals(dto, (Request) requestDao.get(dto.getId()));
		toDelete = dto.getId();

		AccessorChange add = new AccessorChange();
		add.setUserId(individualGroup.getId());
		add.setType(AccessType.GAIN_ACCESS);

		// update
		dto.setAccessorChanges(Arrays.asList(add));
		final RequestInterface updated = requestDao.update(dto);
		dto.setEtag(updated.getEtag());
		assertEquals(dto, updated);

		// insert another one with the same accessRequirementId & createdBy
		assertThrows(IllegalArgumentException.class, () -> {			
			requestDao.create(dto);
		});

		// test get for update
		Request locked = readCommitedTransactionTemplate.execute(new TransactionCallback<Request>() {
			@Override
			public Request doInTransaction(TransactionStatus status) {
				return (Request) requestDao.getForUpdate(updated.getId());
			}
		});
		assertEquals(updated, locked);
	}

	@Test
	public void testSetAndGetEDucContentHash() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();

		// initially null
		assertNull(requestDao.getEDucContentHash(created.getId()));

		// call under test
		requestDao.setEDucContentHash(created.getId(), "abc123");
		assertEquals("abc123", requestDao.getEDucContentHash(created.getId()));

		// can be overwritten
		requestDao.setEDucContentHash(created.getId(), "def456");
		assertEquals("def456", requestDao.getEDucContentHash(created.getId()));

		// can be cleared
		requestDao.setEDucContentHash(created.getId(), null);
		assertNull(requestDao.getEDucContentHash(created.getId()));
	}

	@Test
	public void testSetEDucContentHashChangesEtag() {
		// Migration between stacks detects a changed row by comparing etags, so writing the hash has
		// to rotate the etag or the change would never migrate from production to staging.
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();
		String etagAtCreate = created.getEtag();

		// call under test
		requestDao.setEDucContentHash(created.getId(), "hash-at-route");

		String etagAfterFirstHash = requestDao.get(created.getId()).getEtag();
		assertNotEquals(etagAtCreate, etagAfterFirstHash);

		// call under test — correcting the envelope records a new hash, which must be visible too
		requestDao.setEDucContentHash(created.getId(), "hash-at-correction");

		String etagAfterSecondHash = requestDao.get(created.getId()).getEtag();
		assertNotEquals(etagAfterFirstHash, etagAfterSecondHash);
		assertEquals("hash-at-correction", requestDao.getEDucContentHash(created.getId()));
	}

	@Test
	public void testUpdatePreservesEDucContentHash() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		dto.setId(created.getId());
		dto.setEtag(created.getEtag());
		toDelete = created.getId();

		// record a content hash (as routing/correcting an envelope would)
		requestDao.setEDucContentHash(created.getId(), "hash-at-route");

		// a routine request edit must NOT clear the server-managed hash
		AccessorChange add = new AccessorChange();
		add.setUserId(individualGroup.getId());
		add.setType(AccessType.GAIN_ACCESS);
		dto.setAccessorChanges(Arrays.asList(add));
		requestDao.update(dto);

		assertEquals("hash-at-route", requestDao.getEDucContentHash(created.getId()));
	}

	@Test
	public void testGetEDucContentHashWithNonExisting() {
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			requestDao.getEDucContentHash("-123");
		}).getMessage();

		assertEquals("Data access request: '-123' does not exist", message);
	}

	@Test
	public void testGetForUpdateWithoutTransaction() {
		Request dto = RequestTestUtils.createNewRequest();
		
		assertThrows(IllegalTransactionStateException.class, () -> {			
			requestDao.getForUpdate(dto.getId());
		});
	}
	
	@Test
	public void testGetAccessRequirementId() {
		Request request = RequestTestUtils.createNewRequest();
		request.setAccessRequirementId(accessRequirement.getId().toString());
		request.setResearchProjectId(researchProject.getId());
		request.setCreatedBy(individualGroup.getId());
		request.setModifiedBy(individualGroup.getId());
		request.setAccessorChanges(null);
		request = requestDao.create(request);
		
		toDelete = request.getId();
		
		// Call under test
		String result = requestDao.getAccessRequirementId(request.getId());
		
		assertEquals(accessRequirement.getId().toString(), result);
	}
	
	@Test
	public void testGetAccessRequirementIdWithNonExisting() {

		String message = assertThrows(NotFoundException.class, () -> {
			// Call under test
			requestDao.getAccessRequirementId("-123");
		}).getMessage();

		assertEquals("Data access request: '-123' does not exist", message);
	}

	@Test
	public void testGetUserRequestsWithNoSubmission() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();

		// call under test
		List<RequestUserInfo> results = requestDao.getUserRequests(
				Long.parseLong(individualGroup.getId()), null, null, 10, 0, null, null);

		assertEquals(1, results.size());
		RequestUserInfo info = results.get(0);
		assertEquals(created.getId(), info.getRequestId());
		assertEquals(accessRequirement.getId().toString(), info.getAccessRequirementId());
		assertNotNull(info.getAccessRequirementName());
		assertNull(info.getSubmissionStatus());
		assertNull(info.getEnvelopeId());
		assertNull(info.getSubmittedOn());
		assertNull(info.getModifiedOn());
	}

	@Test
	public void testGetUserRequestsWithSubmission() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();

		Submission submission = new Submission();
		submission.setAccessRequirementId(accessRequirement.getId().toString());
		submission.setAccessRequirementVersion(accessRequirement.getVersionNumber());
		submission.setRequestId(created.getId());
		AccessorChange change = new AccessorChange();
		change.setType(AccessType.GAIN_ACCESS);
		change.setUserId(individualGroup.getId());
		submission.setAccessorChanges(new ArrayList<>(Arrays.asList(change)));
		submission.setIsRenewalSubmission(false);
		submission.setSubmittedBy(individualGroup.getId());
		submission.setSubmittedOn(new Date());
		submission.setModifiedBy(individualGroup.getId());
		submission.setModifiedOn(new Date());
		submission.setResearchProjectSnapshot(researchProject);
		submission.setState(SubmissionState.SUBMITTED);
		submissionDao.createSubmission(submission);
		submissionToDelete = submission.getId();

		// call under test
		List<RequestUserInfo> results = requestDao.getUserRequests(
				Long.parseLong(individualGroup.getId()), null, null, 10, 0, null, null);

		assertEquals(1, results.size());
		RequestUserInfo info = results.get(0);
		assertEquals(created.getId(), info.getRequestId());
		assertEquals(SubmissionState.SUBMITTED, info.getSubmissionStatus());
		assertNotNull(info.getSubmittedOn());
		assertNotNull(info.getModifiedOn());
	}

	/**
	 * Creates a second access requirement, research project and request owned by the same user, so
	 * that a filter can be shown to select one of the user's two requests and not the other.
	 *
	 * @param envelopeId the DUC envelope to record on the request, or null for a request that has none
	 * @return the ID of the created request
	 */
	private String createSecondRequest(String envelopeId) {
		secondAccessRequirement = new ManagedACTAccessRequirement();
		secondAccessRequirement.setCreatedBy(individualGroup.getId());
		secondAccessRequirement.setCreatedOn(new Date());
		secondAccessRequirement.setModifiedBy(individualGroup.getId());
		secondAccessRequirement.setModifiedOn(new Date());
		secondAccessRequirement.setEtag("11");
		secondAccessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest
				.createRestrictableObjectDescriptor(node.getId());
		secondAccessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[] { rod, rod }));
		secondAccessRequirement = accessRequirementDAO.create(secondAccessRequirement);

		secondResearchProject = ResearchProjectTestUtils.createNewDto();
		secondResearchProject.setAccessRequirementId(secondAccessRequirement.getId().toString());
		secondResearchProject = researchProjectDao.create(secondResearchProject);

		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(secondAccessRequirement.getId().toString());
		dto.setResearchProjectId(secondResearchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		dto.setEDucSignatureEnvelopeId(envelopeId);
		Request created = requestDao.create(dto);
		secondRequestToDelete = created.getId();
		return created.getId();
	}

	/**
	 * Creates the request owned by the shared access requirement.
	 *
	 * @param envelopeId the DUC envelope to record on the request, or null for a request that has none
	 * @return the ID of the created request
	 */
	private String createFirstRequest(String envelopeId) {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		dto.setEDucSignatureEnvelopeId(envelopeId);
		Request created = requestDao.create(dto);
		toDelete = created.getId();
		return created.getId();
	}

	private static List<String> requestIds(List<RequestUserInfo> results) {
		return results.stream().map(RequestUserInfo::getRequestId).collect(Collectors.toList());
	}

	@Test
	public void testGetUserRequestsWithIsEDucTrue() {
		String withEnvelope = createFirstRequest("env-123");
		createSecondRequest(null);
		Long userId = Long.parseLong(individualGroup.getId());

		// call under test
		List<RequestUserInfo> results = requestDao.getUserRequests(userId, true, null, 10, 0, null, null);

		assertEquals(List.of(withEnvelope), requestIds(results));
		assertEquals("env-123", results.get(0).getEnvelopeId());
	}

	@Test
	public void testGetUserRequestsWithIsEDucFalse() {
		createFirstRequest("env-123");
		String withoutEnvelope = createSecondRequest(null);
		Long userId = Long.parseLong(individualGroup.getId());

		// call under test
		List<RequestUserInfo> results = requestDao.getUserRequests(userId, false, null, 10, 0, null, null);

		assertEquals(List.of(withoutEnvelope), requestIds(results));
		assertNull(results.get(0).getEnvelopeId());
	}

	@Test
	public void testGetUserRequestsWithNoIsEDucFilter() {
		String withEnvelope = createFirstRequest("env-123");
		String withoutEnvelope = createSecondRequest(null);
		Long userId = Long.parseLong(individualGroup.getId());

		// call under test — a null filter returns both, regardless of envelope
		List<RequestUserInfo> results = requestDao.getUserRequests(userId, null, null, 10, 0, null, null);

		assertEquals(Set.of(withEnvelope, withoutEnvelope), new HashSet<>(requestIds(results)));
	}

	@Test
	public void testGetUserRequestsWithAccessRequirementIdFilter() {
		String firstRequest = createFirstRequest(null);
		String secondRequest = createSecondRequest(null);
		Long userId = Long.parseLong(individualGroup.getId());

		// call under test
		List<RequestUserInfo> forFirstAr = requestDao.getUserRequests(
				userId, null, accessRequirement.getId(), 10, 0, null, null);

		assertEquals(List.of(firstRequest), requestIds(forFirstAr));
		assertEquals(accessRequirement.getId().toString(), forFirstAr.get(0).getAccessRequirementId());

		// call under test — the other requirement selects the other request
		List<RequestUserInfo> forSecondAr = requestDao.getUserRequests(
				userId, null, secondAccessRequirement.getId(), 10, 0, null, null);

		assertEquals(List.of(secondRequest), requestIds(forSecondAr));
	}

	@Test
	public void testGetUserRequestsWithBothFilters() {
		String withEnvelope = createFirstRequest("env-123");
		createSecondRequest(null);
		Long userId = Long.parseLong(individualGroup.getId());

		// call under test — the filters combine, so the eDUC request is found under its own requirement
		List<RequestUserInfo> matching = requestDao.getUserRequests(
				userId, true, accessRequirement.getId(), 10, 0, null, null);

		assertEquals(List.of(withEnvelope), requestIds(matching));

		// call under test — but not under the other requirement, proving the filters are combined
		// rather than either one alone being applied
		List<RequestUserInfo> conflicting = requestDao.getUserRequests(
				userId, true, secondAccessRequirement.getId(), 10, 0, null, null);

		assertEquals(0, conflicting.size());
	}

	@Test
	public void testGetUserRequestsWithFilterAndPagination() {
		// the filter has to be applied in the query, or a page would be built from unfiltered rows
		String withEnvelope = createFirstRequest("env-123");
		createSecondRequest(null);
		Long userId = Long.parseLong(individualGroup.getId());

		// call under test — one match, so the first page holds it and the second is empty
		List<RequestUserInfo> firstPage = requestDao.getUserRequests(userId, true, null, 1, 0, null, null);
		List<RequestUserInfo> secondPage = requestDao.getUserRequests(userId, true, null, 1, 1, null, null);

		assertEquals(List.of(withEnvelope), requestIds(firstPage));
		assertEquals(0, secondPage.size());
	}

	@Test
	public void testGetUserRequestsWithNoResults() {
		// call under test
		List<RequestUserInfo> results = requestDao.getUserRequests(999999L, null, null, 10, 0, null, null);

		assertEquals(0, results.size());
	}

	@Test
	public void testGetUserRequestsWithPagination() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();

		// call under test — offset past the single result
		List<RequestUserInfo> results = requestDao.getUserRequests(
				Long.parseLong(individualGroup.getId()), null, null, 10, 1, null, null);

		assertEquals(0, results.size());
	}

	@Test
	public void testGetUserRequestsWithApprovalForRequester() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();

		Submission submission = new Submission();
		submission.setAccessRequirementId(accessRequirement.getId().toString());
		submission.setAccessRequirementVersion(accessRequirement.getVersionNumber());
		submission.setRequestId(created.getId());
		AccessorChange change = new AccessorChange();
		change.setType(AccessType.GAIN_ACCESS);
		change.setUserId(individualGroup.getId());
		submission.setAccessorChanges(new ArrayList<>(Arrays.asList(change)));
		submission.setIsRenewalSubmission(false);
		submission.setSubmittedBy(individualGroup.getId());
		submission.setSubmittedOn(new Date());
		submission.setModifiedBy(individualGroup.getId());
		submission.setModifiedOn(new Date());
		submission.setResearchProjectSnapshot(researchProject);
		submission.setState(SubmissionState.APPROVED);
		submissionDao.createSubmission(submission);
		submissionToDelete = submission.getId();

		long expirationMs = System.currentTimeMillis() + 86400000L;
		AccessApproval approval = new AccessApproval();
		approval.setCreatedBy(individualGroup.getId());
		approval.setCreatedOn(new Date());
		approval.setModifiedBy(individualGroup.getId());
		approval.setModifiedOn(new Date());
		approval.setAccessorId(individualGroup.getId());
		approval.setRequirementId(accessRequirement.getId());
		approval.setRequirementVersion(accessRequirement.getVersionNumber());
		approval.setSubmitterId(individualGroup.getId());
		approval.setState(ApprovalState.APPROVED);
		approval.setExpiredOn(new Date(expirationMs));
		accessApprovalDAO.create(approval);

		// call under test
		List<RequestUserInfo> results = requestDao.getUserRequests(
				Long.parseLong(individualGroup.getId()), null, null, 10, 0, null, null);

		assertEquals(1, results.size());
		assertNotNull(results.get(0).getExpiresOn());
		assertEquals(expirationMs, results.get(0).getExpiresOn().getTime());
	}

	@Test
	public void testGetUserRequestsWithNoApproval() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();

		Submission submission = new Submission();
		submission.setAccessRequirementId(accessRequirement.getId().toString());
		submission.setAccessRequirementVersion(accessRequirement.getVersionNumber());
		submission.setRequestId(created.getId());
		AccessorChange change = new AccessorChange();
		change.setType(AccessType.GAIN_ACCESS);
		change.setUserId(individualGroup.getId());
		submission.setAccessorChanges(new ArrayList<>(Arrays.asList(change)));
		submission.setIsRenewalSubmission(false);
		submission.setSubmittedBy(individualGroup.getId());
		submission.setSubmittedOn(new Date());
		submission.setModifiedBy(individualGroup.getId());
		submission.setModifiedOn(new Date());
		submission.setResearchProjectSnapshot(researchProject);
		submission.setState(SubmissionState.SUBMITTED);
		submissionDao.createSubmission(submission);
		submissionToDelete = submission.getId();

		// call under test — no approval exists
		List<RequestUserInfo> results = requestDao.getUserRequests(
				Long.parseLong(individualGroup.getId()), null, null, 10, 0, null, null);

		assertEquals(1, results.size());
		assertNull(results.get(0).getExpiresOn());
	}

	@Test
	public void testGetUserRequestsWithApprovalForOtherAccessor() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setAccessorChanges(null);
		Request created = requestDao.create(dto);
		toDelete = created.getId();

		Submission submission = new Submission();
		submission.setAccessRequirementId(accessRequirement.getId().toString());
		submission.setAccessRequirementVersion(accessRequirement.getVersionNumber());
		submission.setRequestId(created.getId());
		AccessorChange change = new AccessorChange();
		change.setType(AccessType.GAIN_ACCESS);
		change.setUserId(individualGroup.getId());
		submission.setAccessorChanges(new ArrayList<>(Arrays.asList(change)));
		submission.setIsRenewalSubmission(false);
		submission.setSubmittedBy(individualGroup.getId());
		submission.setSubmittedOn(new Date());
		submission.setModifiedBy(individualGroup.getId());
		submission.setModifiedOn(new Date());
		submission.setResearchProjectSnapshot(researchProject);
		submission.setState(SubmissionState.APPROVED);
		submissionDao.createSubmission(submission);
		submissionToDelete = submission.getId();

		// Create approval for a different accessor
		AccessApproval approval = new AccessApproval();
		approval.setCreatedBy(individualGroup.getId());
		approval.setCreatedOn(new Date());
		approval.setModifiedBy(individualGroup.getId());
		approval.setModifiedOn(new Date());
		approval.setAccessorId(otherUser.getId());
		approval.setRequirementId(accessRequirement.getId());
		approval.setRequirementVersion(accessRequirement.getVersionNumber());
		approval.setSubmitterId(individualGroup.getId());
		approval.setState(ApprovalState.APPROVED);
		approval.setExpiredOn(new Date(System.currentTimeMillis() + 86400000L));
		accessApprovalDAO.create(approval);

		// call under test — the requesting user has no approval, only otherUser does
		List<RequestUserInfo> results = requestDao.getUserRequests(
				Long.parseLong(individualGroup.getId()), null, null, 10, 0, null, null);

		assertEquals(1, results.size());
		assertNull(results.get(0).getExpiresOn());
	}

	@Test
	public void testGetUserRequestsSortByAccessRequirementName() {
		ManagedACTAccessRequirement ar2 = new ManagedACTAccessRequirement();
		ar2.setCreatedBy(individualGroup.getId());
		ar2.setCreatedOn(new Date());
		ar2.setModifiedBy(individualGroup.getId());
		ar2.setModifiedOn(new Date());
		ar2.setEtag("11");
		ar2.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		ar2.setSubjectIds(Arrays.asList(rod));
		ar2 = accessRequirementDAO.create(ar2);

		ResearchProject rp2 = ResearchProjectTestUtils.createNewDto();
		rp2.setAccessRequirementId(ar2.getId().toString());
		rp2 = researchProjectDao.create(rp2);

		Request dto1 = RequestTestUtils.createNewRequest();
		dto1.setAccessRequirementId(accessRequirement.getId().toString());
		dto1.setResearchProjectId(researchProject.getId());
		dto1.setCreatedBy(individualGroup.getId());
		dto1.setModifiedBy(individualGroup.getId());
		dto1.setAccessorChanges(null);
		Request created1 = requestDao.create(dto1);

		Request dto2 = RequestTestUtils.createNewRequest();
		dto2.setAccessRequirementId(ar2.getId().toString());
		dto2.setResearchProjectId(rp2.getId());
		dto2.setCreatedBy(individualGroup.getId());
		dto2.setModifiedBy(individualGroup.getId());
		dto2.setAccessorChanges(null);
		Request created2 = requestDao.create(dto2);

		try {
			// call under test
			List<RequestUserInfo> results = requestDao.getUserRequests(
					Long.parseLong(individualGroup.getId()), null, null, 10, 0,
					AccessRequestSortField.ACCESS_REQUIREMENT_NAME, SortDirection.ASC);

			assertEquals(2, results.size());
			String name1 = results.get(0).getAccessRequirementName();
			String name2 = results.get(1).getAccessRequirementName();
			assertTrue(name1.compareTo(name2) <= 0);
		} finally {
			requestDao.delete(created1.getId());
			requestDao.delete(created2.getId());
			researchProjectDao.delete(rp2.getId());
			accessRequirementDAO.delete(ar2.getId().toString());
		}
	}

	@Test
	public void testGetUserRequestsSortBySubmittedOn() {
		ManagedACTAccessRequirement ar2 = createSecondAccessRequirement();
		ResearchProject rp2 = createSecondResearchProject(ar2);

		Request dto1 = RequestTestUtils.createNewRequest();
		dto1.setAccessRequirementId(accessRequirement.getId().toString());
		dto1.setResearchProjectId(researchProject.getId());
		dto1.setCreatedBy(individualGroup.getId());
		dto1.setModifiedBy(individualGroup.getId());
		dto1.setAccessorChanges(null);
		Request created1 = requestDao.create(dto1);

		Request dto2 = RequestTestUtils.createNewRequest();
		dto2.setAccessRequirementId(ar2.getId().toString());
		dto2.setResearchProjectId(rp2.getId());
		dto2.setCreatedBy(individualGroup.getId());
		dto2.setModifiedBy(individualGroup.getId());
		dto2.setAccessorChanges(null);
		Request created2 = requestDao.create(dto2);

		// Create submissions with different dates
		String subId1 = createSubmissionForRequest(created1, new Date(1000L));
		String subId2 = createSubmissionForRequest(created2, new Date(2000L));

		try {
			// call under test — DESC: most recent first
			List<RequestUserInfo> desc = requestDao.getUserRequests(
					Long.parseLong(individualGroup.getId()), null, null, 10, 0,
					AccessRequestSortField.SUBMITTED_ON, SortDirection.DESC);

			assertEquals(2, desc.size());
			assertEquals(created2.getId(), desc.get(0).getRequestId());
			assertEquals(created1.getId(), desc.get(1).getRequestId());

			// call under test — ASC: oldest first
			List<RequestUserInfo> asc = requestDao.getUserRequests(
					Long.parseLong(individualGroup.getId()), null, null, 10, 0,
					AccessRequestSortField.SUBMITTED_ON, SortDirection.ASC);

			assertEquals(2, asc.size());
			assertEquals(created1.getId(), asc.get(0).getRequestId());
			assertEquals(created2.getId(), asc.get(1).getRequestId());
		} finally {
			submissionDao.delete(subId1);
			submissionDao.delete(subId2);
			submissionToDelete = null;
			requestDao.delete(created1.getId());
			requestDao.delete(created2.getId());
			researchProjectDao.delete(rp2.getId());
			accessRequirementDAO.delete(ar2.getId().toString());
		}
	}

	@Test
	public void testGetUserRequestsSortByExpiresOn() {
		ManagedACTAccessRequirement ar2 = createSecondAccessRequirement();
		ResearchProject rp2 = createSecondResearchProject(ar2);

		Request dto1 = RequestTestUtils.createNewRequest();
		dto1.setAccessRequirementId(accessRequirement.getId().toString());
		dto1.setResearchProjectId(researchProject.getId());
		dto1.setCreatedBy(individualGroup.getId());
		dto1.setModifiedBy(individualGroup.getId());
		dto1.setAccessorChanges(null);
		Request created1 = requestDao.create(dto1);

		Request dto2 = RequestTestUtils.createNewRequest();
		dto2.setAccessRequirementId(ar2.getId().toString());
		dto2.setResearchProjectId(rp2.getId());
		dto2.setCreatedBy(individualGroup.getId());
		dto2.setModifiedBy(individualGroup.getId());
		dto2.setAccessorChanges(null);
		Request created2 = requestDao.create(dto2);

		String subId1 = createSubmissionForRequest(created1, new Date());
		String subId2 = createSubmissionForRequest(created2, new Date());

		// Create approvals with different expiration dates
		long earlyExpiry = System.currentTimeMillis() + 86400000L;
		long lateExpiry = System.currentTimeMillis() + 172800000L;
		createApproval(accessRequirement, individualGroup.getId(), earlyExpiry);
		createApproval(ar2, individualGroup.getId(), lateExpiry);

		try {
			// call under test — ASC: earliest expiry first
			List<RequestUserInfo> asc = requestDao.getUserRequests(
					Long.parseLong(individualGroup.getId()), null, null, 10, 0,
					AccessRequestSortField.EXPIRES_ON, SortDirection.ASC);

			assertEquals(2, asc.size());
			assertEquals(created1.getId(), asc.get(0).getRequestId());
			assertEquals(created2.getId(), asc.get(1).getRequestId());

			// call under test — DESC: latest expiry first
			List<RequestUserInfo> desc = requestDao.getUserRequests(
					Long.parseLong(individualGroup.getId()), null, null, 10, 0,
					AccessRequestSortField.EXPIRES_ON, SortDirection.DESC);

			assertEquals(2, desc.size());
			assertEquals(created2.getId(), desc.get(0).getRequestId());
			assertEquals(created1.getId(), desc.get(1).getRequestId());
		} finally {
			submissionDao.delete(subId1);
			submissionDao.delete(subId2);
			submissionToDelete = null;
			requestDao.delete(created1.getId());
			requestDao.delete(created2.getId());
			researchProjectDao.delete(rp2.getId());
			accessRequirementDAO.delete(ar2.getId().toString());
		}
	}

	@Test
	public void testGetUserRequestsSortByModifiedOn() {
		ManagedACTAccessRequirement ar2 = createSecondAccessRequirement();
		ResearchProject rp2 = createSecondResearchProject(ar2);

		Request dto1 = RequestTestUtils.createNewRequest();
		dto1.setAccessRequirementId(accessRequirement.getId().toString());
		dto1.setResearchProjectId(researchProject.getId());
		dto1.setCreatedBy(individualGroup.getId());
		dto1.setModifiedBy(individualGroup.getId());
		dto1.setAccessorChanges(null);
		Request created1 = requestDao.create(dto1);

		Request dto2 = RequestTestUtils.createNewRequest();
		dto2.setAccessRequirementId(ar2.getId().toString());
		dto2.setResearchProjectId(rp2.getId());
		dto2.setCreatedBy(individualGroup.getId());
		dto2.setModifiedBy(individualGroup.getId());
		dto2.setAccessorChanges(null);
		Request created2 = requestDao.create(dto2);

		String subId1 = createSubmissionForRequest(created1, new Date(1000L));
		String subId2 = createSubmissionForRequest(created2, new Date(2000L));

		try {
			// call under test — DESC: most recently modified first
			List<RequestUserInfo> desc = requestDao.getUserRequests(
					Long.parseLong(individualGroup.getId()), null, null, 10, 0,
					AccessRequestSortField.MODIFIED_ON, SortDirection.DESC);

			assertEquals(2, desc.size());
			assertEquals(created2.getId(), desc.get(0).getRequestId());
			assertEquals(created1.getId(), desc.get(1).getRequestId());

			// call under test — ASC: least recently modified first
			List<RequestUserInfo> asc = requestDao.getUserRequests(
					Long.parseLong(individualGroup.getId()), null, null, 10, 0,
					AccessRequestSortField.MODIFIED_ON, SortDirection.ASC);

			assertEquals(2, asc.size());
			assertEquals(created1.getId(), asc.get(0).getRequestId());
			assertEquals(created2.getId(), asc.get(1).getRequestId());
		} finally {
			submissionDao.delete(subId1);
			submissionDao.delete(subId2);
			submissionToDelete = null;
			requestDao.delete(created1.getId());
			requestDao.delete(created2.getId());
			researchProjectDao.delete(rp2.getId());
			accessRequirementDAO.delete(ar2.getId().toString());
		}
	}

	private ManagedACTAccessRequirement createSecondAccessRequirement() {
		ManagedACTAccessRequirement ar2 = new ManagedACTAccessRequirement();
		ar2.setCreatedBy(individualGroup.getId());
		ar2.setCreatedOn(new Date());
		ar2.setModifiedBy(individualGroup.getId());
		ar2.setModifiedOn(new Date());
		ar2.setEtag("11");
		ar2.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		ar2.setSubjectIds(Arrays.asList(rod));
		return accessRequirementDAO.create(ar2);
	}

	private ResearchProject createSecondResearchProject(ManagedACTAccessRequirement ar2) {
		ResearchProject rp2 = ResearchProjectTestUtils.createNewDto();
		rp2.setAccessRequirementId(ar2.getId().toString());
		return researchProjectDao.create(rp2);
	}

	private String createSubmissionForRequest(Request request, Date submittedOn) {
		Submission submission = new Submission();
		submission.setAccessRequirementId(request.getAccessRequirementId());
		submission.setAccessRequirementVersion(accessRequirement.getVersionNumber());
		submission.setRequestId(request.getId());
		AccessorChange change = new AccessorChange();
		change.setType(AccessType.GAIN_ACCESS);
		change.setUserId(individualGroup.getId());
		submission.setAccessorChanges(new ArrayList<>(Arrays.asList(change)));
		submission.setIsRenewalSubmission(false);
		submission.setSubmittedBy(individualGroup.getId());
		submission.setSubmittedOn(submittedOn);
		submission.setModifiedBy(individualGroup.getId());
		submission.setModifiedOn(submittedOn);
		submission.setResearchProjectSnapshot(researchProject);
		submission.setState(SubmissionState.SUBMITTED);
		submissionDao.createSubmission(submission);
		submissionToDelete = submission.getId();
		return submission.getId();
	}

	private void createApproval(ManagedACTAccessRequirement ar, String userId, long expirationMs) {
		AccessApproval approval = new AccessApproval();
		approval.setCreatedBy(userId);
		approval.setCreatedOn(new Date());
		approval.setModifiedBy(userId);
		approval.setModifiedOn(new Date());
		approval.setAccessorId(userId);
		approval.setRequirementId(ar.getId());
		approval.setRequirementVersion(ar.getVersionNumber());
		approval.setSubmitterId(userId);
		approval.setState(ApprovalState.APPROVED);
		approval.setExpiredOn(new Date(expirationMs));
		accessApprovalDAO.create(approval);
	}
}
