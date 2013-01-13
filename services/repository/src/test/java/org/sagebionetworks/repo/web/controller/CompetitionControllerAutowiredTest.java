package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.dao.SubmissionDAO;
import org.sagebionetworks.competition.dao.SubmissionStatusDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CompetitionControllerAutowiredTest {
	
	@Autowired
	EntityServletTestHelper entityServletHelper;
	@Autowired
	UserManager userManager;
	@Autowired
	CompetitionDAO competitionDAO;
	@Autowired
	ParticipantDAO participantDAO;
	@Autowired
	SubmissionDAO submissionDAO;
	@Autowired
	SubmissionStatusDAO submissionStatusDAO;
	@Autowired
	NodeDAO nodeDAO;
	
	private static String ownerName;
	private static String userName;
	private static String ownerId;
	private static String userId;
	
	private static Competition comp1;
	private static Competition comp2;
	private static Participant part1;
	private static Participant part2;
	private static Submission sub1;
	private static Submission sub2;
	
	private static List<String> competitionsToDelete;
	private static List<Participant> participantsToDelete;
	private static List<String> submissionsToDelete;
	private static List<String> nodesToDelete;
	
	@Before
	public void before() throws DatastoreException, NotFoundException {
		competitionsToDelete = new ArrayList<String>();
		participantsToDelete = new ArrayList<Participant>();
		submissionsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
		
		// get user IDs
		ownerName = TestUserDAO.ADMIN_USER_NAME;
		userName = TestUserDAO.TEST_USER_NAME;
		ownerId = userManager.getUserInfo(ownerName).getIndividualGroup().getId();
		userId = userManager.getUserInfo(userName).getIndividualGroup().getId();
		
		// initialize Competitions
		comp1 = new Competition();
		comp1.setName("name");
		comp1.setDescription("description");
        comp1.setContentSource("contentSource");
        comp1.setStatus(CompetitionStatus.PLANNED);
        comp2 = new Competition();
		comp2.setName("name2");
		comp2.setDescription("description");
        comp2.setContentSource("contentSource");
        comp2.setStatus(CompetitionStatus.PLANNED);
        
        // initialize Participants
        part1 = new Participant();
        part1.setUserId(ownerName);
        part2 = new Participant();
        part2.setUserId(userName);		
        
        // initialize Submissions
        sub1 = new Submission();
        sub1.setName("submission1");
        sub1.setVersionNumber(1L);
        sub2 = new Submission();
        sub2.setName("submission2");
        sub2.setVersionNumber(1L);
	}
	
	@After
	public void after() {
		// clean up submissions
		for (String id : submissionsToDelete) {
			try {
				submissionStatusDAO.delete(id);
			} catch (Exception e) {}
			try {
				submissionDAO.delete(id);
			} catch (Exception e) {}
		}
		
		// clean up participants
		for (Participant part : participantsToDelete) {
			try {
				participantDAO.delete(part.getUserId(), part.getCompetitionId());
			} catch (Exception e) {}
		}
		
		// clean up competitions
		for (String id : competitionsToDelete) {
			try {
				competitionDAO.delete(id);
			} catch (Exception e) {}
		}
		
		// clean up nodes
		for (String id : nodesToDelete) {
			try {
				nodeDAO.delete(id);
			} catch (Exception e) {}
		}
	}
	
	@Test
	public void testCompetitionRoundTrip() throws DatastoreException, JSONObjectAdapterException, IOException, NotFoundException, ServletException {
		long initialCount = entityServletHelper.getCompetitionCount();
		
		// Create
		comp1 = entityServletHelper.createCompetition(comp1, ownerName);		
		assertNotNull(comp1.getEtag());
		assertNotNull(comp1.getId());
		competitionsToDelete.add(comp1.getId());
		
		// Read
		Competition fetched = entityServletHelper.getCompetition(comp1.getId());
		assertEquals(comp1, fetched);
		fetched = entityServletHelper.findCompetition(comp1.getName());
		assertEquals(comp1, fetched);
		
		// Update
		fetched.setDescription(comp1.getDescription() + " (modified)");
		Competition updated = entityServletHelper.updateCompetition(fetched, ownerName);
		assertFalse("eTag was not updated", updated.getEtag().equals(fetched.getEtag()));
		fetched.setEtag(updated.getEtag());
		assertEquals(fetched, updated);		
		assertEquals(initialCount + 1, entityServletHelper.getCompetitionCount());
		
		// Delete
		entityServletHelper.deleteCompetition(comp1.getId(), ownerName);
		try {
			entityServletHelper.getCompetition(comp1.getId());
			fail("Delete failed");
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getCompetitionCount());
	}
	
	@Test
	public void testParticipantRoundTrip() throws DatastoreException, JSONObjectAdapterException, IOException, NotFoundException, ServletException {
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = entityServletHelper.createCompetition(comp1, ownerName);
		competitionsToDelete.add(comp1.getId());
		
		long initialCount = entityServletHelper.getParticipantCount(comp1.getId());
		
		// create
		part1 = entityServletHelper.createParticipant(userName, comp1.getId());
		assertNotNull(part1.getCreatedOn());
		participantsToDelete.add(part1);
		assertEquals(initialCount + 1, entityServletHelper.getParticipantCount(comp1.getId()));
		
		// read
		Participant clone = entityServletHelper.getParticipant(userId, comp1.getId());
		assertEquals(part1, clone);
		
		// delete
		entityServletHelper.deleteParticipant(ownerName, userId, comp1.getId());
		try {
			entityServletHelper.getParticipant(userId, comp1.getId());
			fail("Failed to delete Participant " + part1.toString());
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getParticipantCount(comp1.getId()));
	}
	
	@Test
	public void testSubmissionRoundTrip() throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException, IOException, ServletException, InterruptedException {
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = entityServletHelper.createCompetition(comp1, ownerName);
		competitionsToDelete.add(comp1.getId());
		part1 = entityServletHelper.createParticipant(userName, comp1.getId());
		participantsToDelete.add(part1);
		String nodeId = createNode("An entity");
		assertNotNull(nodeId);
		nodesToDelete.add(nodeId);
		
		long initialCount = entityServletHelper.getSubmissionCount(comp1.getId());
		
		// create
		sub1.setCompetitionId(comp1.getId());
		sub1.setEntityId(nodeId);
		sub1 = entityServletHelper.createSubmission(sub1, userName);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		assertEquals(initialCount + 1, entityServletHelper.getSubmissionCount(comp1.getId()));
		
		// read
		Submission clone = entityServletHelper.getSubmission(sub1.getId());
		assertEquals(sub1, clone);
		SubmissionStatus status = entityServletHelper.getSubmissionStatus(sub1.getId());
		assertNotNull(status);
		assertEquals(sub1.getId(), status.getId());
		assertEquals(SubmissionStatusEnum.OPEN, status.getStatus());
		
		// update
		Thread.sleep(1L);
		status.setScore(50L);
		status.setStatus(SubmissionStatusEnum.SCORED);
		SubmissionStatus statusClone = entityServletHelper.updateSubmissionStatus(status, ownerName);
		assertFalse("Modified date was not updated", status.getModifiedOn().equals(statusClone.getModifiedOn()));
		status.setModifiedOn(statusClone.getModifiedOn());
		assertFalse("Etag was not updated", status.getEtag().equals(statusClone.getEtag()));
		status.setEtag(statusClone.getEtag());
		assertEquals(status, statusClone);
		assertEquals(initialCount + 1, entityServletHelper.getSubmissionCount(comp1.getId()));
		
		// delete
		entityServletHelper.deleteSubmission(sub1.getId(), ownerName);
		try {
			entityServletHelper.deleteSubmission(sub1.getId(), ownerName);
			fail("Failed to delete Submission " + sub1.toString());
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getSubmissionCount(comp1.getId()));
	}
	
	@Test
	public void testPaginated() throws DatastoreException, JSONObjectAdapterException, IOException, NotFoundException, ServletException {
		// create objects
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = entityServletHelper.createCompetition(comp1, ownerName);
		assertNotNull(comp1.getId());
		competitionsToDelete.add(comp1.getId());
		comp2 = entityServletHelper.createCompetition(comp2, ownerName);
		assertNotNull(comp2.getId());
		competitionsToDelete.add(comp2.getId());
		
		part1 = entityServletHelper.createParticipant(ownerName, comp1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
		part2 = entityServletHelper.createParticipant(userName, comp1.getId());
		assertNotNull(part2);
		participantsToDelete.add(part2);
		
		// fetch comp1 and verify that eTag has been updated
		String oldEtag = comp1.getEtag();
		comp1 = entityServletHelper.getCompetition(comp1.getId());
		assertFalse("Etag was not updated", oldEtag.equals(comp1.getEtag()));
		
		String node1 = createNode("entity1");
		assertNotNull(node1);
		nodesToDelete.add(node1);
		String node2 = createNode("entity2");
		assertNotNull(node2);
		nodesToDelete.add(node2);
		
		sub1.setCompetitionId(comp1.getId());
		sub1.setEntityId(node1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(userName);
		sub1 = entityServletHelper.createSubmission(sub1, userName);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());		
		sub2.setCompetitionId(comp1.getId());
		sub2.setEntityId(node2);
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = entityServletHelper.createSubmission(sub2, userName);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
		
		// paginated competitions
		PaginatedResults<Competition> comps = entityServletHelper.getCompetitionsPaginated(10, 0);
		assertEquals(2, comps.getTotalNumberOfResults());
		for (Competition c : comps.getResults())
			assertTrue("Unknown Competition returned: " + c.toString(), c.equals(comp1) || c.equals(comp2));
		
		// paginated participants
		PaginatedResults<Participant> parts = entityServletHelper.getAllParticipants(comp1.getId());
		assertEquals(2, parts.getTotalNumberOfResults());
		for (Participant p : parts.getResults())
			assertTrue("Unknown Participant returned: " + p.toString(), p.equals(part1) || p.equals(part2));
		
		parts = entityServletHelper.getAllParticipants(comp2.getId());
		assertEquals(0, parts.getTotalNumberOfResults());
		
		// paginated submissions
		PaginatedResults<Submission> subs = entityServletHelper.getAllSubmissions(ownerName, comp1.getId(), null);
		assertEquals(2, subs.getTotalNumberOfResults());
		for (Submission s : subs.getResults())
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		
		subs = entityServletHelper.getAllSubmissions(ownerName, comp1.getId(), SubmissionStatusEnum.CLOSED);
		assertEquals(0, subs.getTotalNumberOfResults());
		
		subs = entityServletHelper.getAllSubmissions(ownerName, comp2.getId(), null);
		assertEquals(0, subs.getTotalNumberOfResults());
	}
	
	private String createNode(String name) throws DatastoreException, InvalidModelException, NotFoundException {
		Node toCreate = new Node();
		toCreate.setName(name);
		toCreate.setCreatedByPrincipalId(Long.parseLong(ownerId));
		toCreate.setModifiedByPrincipalId(Long.parseLong(ownerId));
		toCreate.setCreatedOn(new Date(System.currentTimeMillis()));
		toCreate.setModifiedOn(toCreate.getCreatedOn());
		toCreate.setNodeType(EntityType.project.name());
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("1");
    	String id = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	nodesToDelete.add(id);
    	return id;
	}

}
