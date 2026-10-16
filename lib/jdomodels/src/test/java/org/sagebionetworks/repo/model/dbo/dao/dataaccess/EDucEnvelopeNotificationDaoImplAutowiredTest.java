package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

	// A request carrying an envelope, which is what the scan is looking for. The request table is unique on
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

	@Test
	public void testCreateAndFind() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = request.getId();

		// call under test
		DBOEDucEnvelopeNotification created = notificationDao.create(Long.parseLong(request.getId()), "env-1",
				"completed", 9001L);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertNotNull(created.getObservedOn());

		Optional<DBOEDucEnvelopeNotification> found = notificationDao.find("env-1");
		assertTrue(found.isPresent());
		assertEquals(created, found.get());
		assertEquals("completed", found.get().getTerminalStatus());
		assertEquals(Long.valueOf(9001L), found.get().getMessageId());
		assertEquals(Long.valueOf(request.getId()), found.get().getRequestId());
	}

	@Test
	public void testFindWithNoRecord() {
		// call under test
		assertTrue(notificationDao.find("env-does-not-exist").isEmpty());
	}

	// The unique key is the last-resort guard against notifying a requester twice. DBOBasicDao reports a
	// constraint breach as an IllegalArgumentException wrapping the cause, so that is what a caller sees.
	@Test
	public void testCreateWithDuplicateEnvelope() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = request.getId();
		notificationDao.create(Long.parseLong(request.getId()), "env-1", "completed", 9001L);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> notificationDao.create(Long.parseLong(request.getId()), "env-1", "declined", 9002L));
		assertTrue(ex.getCause() instanceof DataIntegrityViolationException);
	}

	// A terminal state recorded without notifying anyone, which is how a voided envelope stops being examined.
	@Test
	public void testCreateWithNoMessageSent() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = request.getId();

		// call under test
		notificationDao.create(Long.parseLong(request.getId()), "env-1", "voided",
				DBOEDucEnvelopeNotification.NO_MESSAGE_SENT);

		assertEquals(Long.valueOf(DBOEDucEnvelopeNotification.NO_MESSAGE_SENT),
				notificationDao.find("env-1").get().getMessageId());
	}

	@Test
	public void testListEnvelopesToExamine() {
		RequestInterface unexamined = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = unexamined.getId();

		secondAccessRequirement = createAccessRequirement();
		secondResearchProject = createResearchProject(secondAccessRequirement);
		RequestInterface alreadyRecorded = createRequestWithEnvelope(secondAccessRequirement,
				secondResearchProject, "env-2");
		secondRequestToDelete = alreadyRecorded.getId();

		// The decoy: an envelope whose terminal state is already recorded must drop out of the scan.
		notificationDao.create(Long.parseLong(alreadyRecorded.getId()), "env-2", "completed", 9001L);

		// call under test
		List<EDucEnvelopeToExamine> toExamine = notificationDao.listEnvelopesToExamine(100L);

		assertEquals(List.of(new EDucEnvelopeToExamine(Long.parseLong(unexamined.getId()), "env-1",
				Long.parseLong(individualGroup.getId()))), toExamine);
	}

	// A request that never routed an envelope is not something to examine.
	@Test
	public void testListEnvelopesToExamineWithNoEnvelope() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, null);
		requestToDelete = request.getId();

		// call under test
		assertTrue(notificationDao.listEnvelopesToExamine(100L).isEmpty());
	}

	@Test
	public void testListEnvelopesToExamineHonorsLimit() {
		RequestInterface first = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		requestToDelete = first.getId();

		secondAccessRequirement = createAccessRequirement();
		secondResearchProject = createResearchProject(secondAccessRequirement);
		RequestInterface second = createRequestWithEnvelope(secondAccessRequirement, secondResearchProject,
				"env-2");
		secondRequestToDelete = second.getId();

		// call under test
		assertEquals(1, notificationDao.listEnvelopesToExamine(1L).size());
		assertEquals(2, notificationDao.listEnvelopesToExamine(10L).size());
	}

	// Deleting the request takes its notification record with it, so a recreated envelope id is not blocked by
	// a row nothing refers to any more.
	@Test
	public void testDeleteRequestCascades() {
		RequestInterface request = createRequestWithEnvelope(accessRequirement, researchProject, "env-1");
		notificationDao.create(Long.parseLong(request.getId()), "env-1", "completed", 9001L);
		assertTrue(notificationDao.find("env-1").isPresent());

		// call under test
		requestDao.delete(request.getId());

		assertTrue(notificationDao.find("env-1").isEmpty());
	}
}
