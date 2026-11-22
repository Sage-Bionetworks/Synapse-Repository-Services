package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtilsTest;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EDucEnvelopeNotificationDaoImplAutowiredTest {

	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private ResearchProjectDAO researchProjectDao;
	@Autowired
	private RequestDAO requestDao;
	@Autowired
	private EDucEnvelopeNotificationDao notificationDao;

	private UserGroup individualGroup;
	private Node node;
	private ManagedACTAccessRequirement accessRequirement;
	private ResearchProject researchProject;
	private String requestToDelete;

	private ManagedACTAccessRequirement secondAccessRequirement;
	private ResearchProject secondResearchProject;
	private String secondRequestToDelete;

	@BeforeEach
	public void before() {
		notificationDao.truncateAll();

		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());

		node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
		node.setId(nodeDao.createNew(node));

		accessRequirement = createAccessRequirement();
		researchProject = createResearchProject(accessRequirement);
	}

	@AfterEach
	public void after() {
		notificationDao.truncateAll();
		if (secondRequestToDelete != null) {
			requestDao.delete(secondRequestToDelete);
		}
		if (secondResearchProject != null) {
			researchProjectDao.delete(secondResearchProject.getId());
		}
		if (secondAccessRequirement != null) {
			accessRequirementDAO.delete(secondAccessRequirement.getId().toString());
		}
		if (requestToDelete != null) {
			requestDao.delete(requestToDelete);
		}
		if (researchProject != null) {
			researchProjectDao.delete(researchProject.getId());
		}
		if (accessRequirement != null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (node != null) {
			nodeDao.delete(node.getId());
		}
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
	}

	private ManagedACTAccessRequirement createAccessRequirement() {
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		ar.setCreatedBy(individualGroup.getId());
		ar.setCreatedOn(new Date());
		ar.setModifiedBy(individualGroup.getId());
		ar.setModifiedOn(new Date());
		ar.setEtag("10");
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest
				.createRestrictableObjectDescriptor(node.getId());
		ar.setSubjectIds(Arrays.asList(rod, rod));
		return accessRequirementDAO.create(ar);
	}

	private ResearchProject createResearchProject(ManagedACTAccessRequirement ar) {
		ResearchProject rp = ResearchProjectTestUtils.createNewDto();
		rp.setAccessRequirementId(ar.getId().toString());
		return researchProjectDao.create(rp);
	}

	// A request carrying an envelope, which is what a terminal status is matched against. The request table is unique on
	// (ACCESS_REQUIREMENT_ID, CREATED_BY), so a second request needs its own access requirement.
	private RequestInterface createRequestWithEnvelope(ManagedACTAccessRequirement ar, ResearchProject rp,
			String envelopeId) {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(ar.getId().toString());
		dto.setResearchProjectId(rp.getId());
		dto.setCreatedBy(individualGroup.getId());
		dto.setModifiedBy(individualGroup.getId());
		dto.setEDucSignatureEnvelopeId(envelopeId);
		return requestDao.create(dto);
	}

	private RequestInterface createSecondRequestWithEnvelope(String envelopeId) {
		secondAccessRequirement = createAccessRequirement();
		secondResearchProject = createResearchProject(secondAccessRequirement);
		RequestInterface request = createRequestWithEnvelope(secondAccessRequirement, secondResearchProject,
				envelopeId);
		secondRequestToDelete = request.getId();
		return request;
	}

	@Test
	public void testCreatePendingAndFind() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = request.getId();

		// call under test
		DBOEDucEnvelopeNotification created = notificationDao.createPending(Long.parseLong(request.getId()),
				"env-1", EDucStatusEnum.completed);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertNotNull(created.getObservedOn());

		Optional<DBOEDucEnvelopeNotification> found = notificationDao.find("env-1");
		assertEquals(Optional.of(created), found);
		assertEquals("completed", found.get().getTerminalStatus());
		assertEquals(EDucNotificationState.PENDING.name(), found.get().getNotificationState());
		assertEquals(Long.valueOf(request.getId()), found.get().getRequestId());
		assertNull(found.get().getMessageId());
		assertNull(found.get().getNotifiedOn());
	}

	@Test
	public void testFindWithNoRecord() {
		// call under test
		assertTrue(notificationDao.find("env-does-not-exist").isEmpty());
	}

	// DBOBasicDao reports a constraint breach as an IllegalArgumentException wrapping the cause
	@Test
	public void testCreatePendingWithDuplicateEnvelope() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = request.getId();
		notificationDao.createPending(Long.parseLong(request.getId()), "env-1", EDucStatusEnum.completed);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			notificationDao.createPending(Long.parseLong(request.getId()), "env-1", EDucStatusEnum.declined);
		});
		assertTrue(ex.getCause() instanceof DataIntegrityViolationException);
	}

	@Test
	public void testUpdateNotificationStateWithSent() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = request.getId();
		DBOEDucEnvelopeNotification created = notificationDao.createPending(Long.parseLong(request.getId()),
				"env-1", EDucStatusEnum.completed);

		// call under test
		notificationDao.updateNotificationState("env-1", EDucNotificationState.SENT, 9001L);

		DBOEDucEnvelopeNotification updated = notificationDao.find("env-1").get();
		assertEquals(EDucNotificationState.SENT.name(), updated.getNotificationState());
		assertEquals(Long.valueOf(9001L), updated.getMessageId());
		assertNotNull(updated.getNotifiedOn());
		assertNotEquals(created.getEtag(), updated.getEtag());
		assertEquals(created.getTerminalStatus(), updated.getTerminalStatus());
	}

	@Test
	public void testUpdateNotificationStateWithNotSent() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = request.getId();
		notificationDao.createPending(Long.parseLong(request.getId()), "env-1", EDucStatusEnum.voided);

		// call under test
		notificationDao.updateNotificationState("env-1", EDucNotificationState.NOT_SENT, null);

		DBOEDucEnvelopeNotification updated = notificationDao.find("env-1").get();
		assertEquals(EDucNotificationState.NOT_SENT.name(), updated.getNotificationState());
		assertNull(updated.getMessageId());
		assertNotNull(updated.getNotifiedOn());
	}

	@Test
	public void testUpdateNotificationStateWithPending() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			notificationDao.updateNotificationState("env-1", EDucNotificationState.PENDING, null);
		});
	}

	@Test
	public void testUpdateNotificationStateWithSentAndNoMessageId() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			notificationDao.updateNotificationState("env-1", EDucNotificationState.SENT, null);
		});
	}

	@Test
	public void testUpdateNotificationStateWithNotSentAndMessageId() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			notificationDao.updateNotificationState("env-1", EDucNotificationState.NOT_SENT, 9001L);
		});
	}

	@Test
	public void testFindRequestIdsByEnvelopeId() {
		RequestInterface first = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = first.getId();
		RequestInterface second = createSecondRequestWithEnvelope("env-2");

		// call under test — an envelope no request holds, such as another stack's, is absent
		Map<String, Long> result = notificationDao
				.findRequestIdsByEnvelopeId(List.of("env-1", "env-2", "env-other-stack"));

		assertEquals(Map.of("env-1", Long.parseLong(first.getId()), "env-2", Long.parseLong(second.getId())),
				result);
	}

	@Test
	public void testFindRequestIdsByEnvelopeIdWithRequestWithoutEnvelope() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, null);
		requestToDelete = request.getId();

		// call under test
		assertEquals(Map.of(), notificationDao.findRequestIdsByEnvelopeId(List.of("env-1")));
	}

	@Test
	public void testFindRequestIdsByEnvelopeIdWithEmptyList() {
		// call under test
		assertEquals(Map.of(), notificationDao.findRequestIdsByEnvelopeId(List.of()));
	}

	@Test
	public void testListPendingNotifications() {
		RequestInterface first = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = first.getId();
		RequestInterface second = createSecondRequestWithEnvelope("env-2");
		notificationDao.createPending(Long.parseLong(first.getId()), "env-1", EDucStatusEnum.completed);
		notificationDao.createPending(Long.parseLong(second.getId()), "env-2", EDucStatusEnum.declined);

		Long createdBy = Long.parseLong(individualGroup.getId());

		// call under test
		assertEquals(List.of(
				new EDucPendingNotification(Long.parseLong(first.getId()), "env-1", EDucStatusEnum.completed, createdBy),
				new EDucPendingNotification(Long.parseLong(second.getId()), "env-2", EDucStatusEnum.declined, createdBy)),
				notificationDao.listPendingNotifications(100L));

		// The decoy: once dealt with, a notification is no longer pending
		notificationDao.updateNotificationState("env-1", EDucNotificationState.SENT, 9001L);

		// call under test
		assertEquals(List.of(
				new EDucPendingNotification(Long.parseLong(second.getId()), "env-2", EDucStatusEnum.declined, createdBy)),
				notificationDao.listPendingNotifications(100L));
	}

	@Test
	public void testListPendingNotificationsHonorsLimit() {
		RequestInterface first = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = first.getId();
		RequestInterface second = createSecondRequestWithEnvelope("env-2");
		notificationDao.createPending(Long.parseLong(first.getId()), "env-1", EDucStatusEnum.completed);
		notificationDao.createPending(Long.parseLong(second.getId()), "env-2", EDucStatusEnum.declined);

		// call under test
		List<EDucPendingNotification> limited = notificationDao.listPendingNotifications(1L);

		// The oldest comes first
		assertEquals(List.of("env-1"), limited.stream().map(EDucPendingNotification::envelopeId).toList());
		assertEquals(2, notificationDao.listPendingNotifications(10L).size());
	}

	@Test
	public void testGetStatusCheckpointWithNoneSet() {
		// call under test
		assertEquals(Optional.empty(), notificationDao.getStatusCheckpoint());
	}

	@Test
	public void testSetStatusCheckpoint() {
		// call under test
		notificationDao.setStatusCheckpoint(Instant.parse("2026-11-20T10:05:00.123Z"));

		assertEquals(Optional.of(Instant.parse("2026-11-20T10:05:00.123Z")), notificationDao.getStatusCheckpoint());

		// call under test — moving the checkpoint replaces it rather than adding a row
		notificationDao.setStatusCheckpoint(Instant.parse("2026-11-20T10:10:00.456Z"));

		assertEquals(Optional.of(Instant.parse("2026-11-20T10:10:00.456Z")), notificationDao.getStatusCheckpoint());
	}

	// The signing service reports sub-millisecond time, which is truncated rather than rounded up past a
	// change made in the same millisecond
	@Test
	public void testSetStatusCheckpointWithSubMillisecondTime() {
		// call under test
		notificationDao.setStatusCheckpoint(Instant.parse("2026-11-20T10:05:00.1239999Z"));

		assertEquals(Optional.of(Instant.parse("2026-11-20T10:05:00.123Z")), notificationDao.getStatusCheckpoint());
	}

	// Deleting the request takes its notification record with it, so a recreated envelope id is not blocked by
	// a row nothing refers to any more.
	@Test
	public void testDeleteRequestCascades() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		notificationDao.createPending(Long.parseLong(request.getId()), "env-1", EDucStatusEnum.completed);
		assertTrue(notificationDao.find("env-1").isPresent());

		// call under test
		requestDao.delete(request.getId());

		assertTrue(notificationDao.find("env-1").isEmpty());
	}
}
