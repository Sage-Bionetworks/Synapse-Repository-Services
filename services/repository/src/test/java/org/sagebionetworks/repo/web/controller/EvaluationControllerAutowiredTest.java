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
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EvaluationControllerAutowiredTest {
	
	@Autowired
	EntityServletTestHelper entityServletHelper;
	@Autowired
	UserManager userManager;
	@Autowired
	NodeManager nodeManager;
	@Autowired
	EvaluationDAO evaluationDAO;
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
	private static String userId;
	
	private static Evaluation eval1;
	private static Evaluation eval2;
	private static Participant part1;
	private static Participant part2;
	private static Submission sub1;
	private static Submission sub2;
	
	private static List<String> evaluationsToDelete;
	private static List<Participant> participantsToDelete;
	private static List<String> submissionsToDelete;
	private static List<String> nodesToDelete;
	
	@Before
	public void before() throws DatastoreException, NotFoundException {
		evaluationsToDelete = new ArrayList<String>();
		participantsToDelete = new ArrayList<Participant>();
		submissionsToDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
		
		// get user IDs
		ownerName = TestUserDAO.ADMIN_USER_NAME;
		userName = TestUserDAO.TEST_USER_NAME;
		userManager.getUserInfo(ownerName).getIndividualGroup().getId();
		userId = userManager.getUserInfo(userName).getIndividualGroup().getId();
		
		// initialize Evaluations
		eval1 = new Evaluation();
		eval1.setName("name");
		eval1.setDescription("description");
        eval1.setContentSource(KeyFactory.SYN_ROOT_ID);
        eval1.setStatus(EvaluationStatus.PLANNED);
        eval2 = new Evaluation();
		eval2.setName("name2");
		eval2.setDescription("description");
        eval2.setContentSource(KeyFactory.SYN_ROOT_ID);
        eval2.setStatus(EvaluationStatus.PLANNED);
        
        // initialize Participants
        part1 = new Participant();
        part1.setUserId(ownerName);
        part2 = new Participant();
        part2.setUserId(userName);		
        
        // initialize Submissions
        sub1 = new Submission();
        sub1.setName("submission1");
        sub1.setVersionNumber(1L);
        sub1.setSubmitterAlias("Team Awesome!");
        sub2 = new Submission();
        sub2.setName("submission2");
        sub2.setVersionNumber(1L);
        sub2.setSubmitterAlias("Team Even Better!");
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
				participantDAO.delete(part.getUserId(), part.getEvaluationId());
			} catch (Exception e) {}
		}
		
		// clean up evaluations
		for (String id : evaluationsToDelete) {
			try {
				evaluationDAO.delete(id);
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
	public void testEvaluationRoundTrip() throws DatastoreException, JSONObjectAdapterException, IOException, NotFoundException, ServletException {
		long initialCount = entityServletHelper.getEvaluationCount();
		
		// Create
		eval1 = entityServletHelper.createEvaluation(eval1, ownerName);		
		assertNotNull(eval1.getEtag());
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		
		//can read
		Boolean canRead = entityServletHelper.canAccess(ownerName, eval1.getId(), ACCESS_TYPE.READ);
		assertTrue(canRead);
		//test user can also read
		canRead = entityServletHelper.canAccess(userName, eval1.getId(), ACCESS_TYPE.READ);
		assertTrue(canRead);
		
		// Read
		Evaluation fetched = entityServletHelper.getEvaluation(eval1.getId());
		assertEquals(eval1, fetched);
		fetched = entityServletHelper.findEvaluation(eval1.getName());
		assertEquals(eval1, fetched);
		
		//can update
		Boolean canUpdate = entityServletHelper.canAccess(ownerName, eval1.getId(), ACCESS_TYPE.UPDATE);
		assertTrue(canUpdate);
		//test user can't update
		canUpdate = entityServletHelper.canAccess(userName, eval1.getId(), ACCESS_TYPE.UPDATE);
		assertFalse(canUpdate);
		
		// Update
		fetched.setDescription(eval1.getDescription() + " (modified)");
		Evaluation updated = entityServletHelper.updateEvaluation(fetched, ownerName);
		assertFalse("eTag was not updated", updated.getEtag().equals(fetched.getEtag()));
		fetched.setEtag(updated.getEtag());
		assertEquals(fetched, updated);		
		assertEquals(initialCount + 1, entityServletHelper.getEvaluationCount());
		
		// Delete
		entityServletHelper.deleteEvaluation(eval1.getId(), ownerName);
		try {
			entityServletHelper.getEvaluation(eval1.getId());
			fail("Delete failed");
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getEvaluationCount());
	}
	
	@Test
	public void testParticipantRoundTrip() throws DatastoreException, JSONObjectAdapterException, IOException, NotFoundException, ServletException {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = entityServletHelper.createEvaluation(eval1, ownerName);
		evaluationsToDelete.add(eval1.getId());
		
		long initialCount = entityServletHelper.getParticipantCount(eval1.getId());
		
		// create
		part1 = entityServletHelper.createParticipant(userName, eval1.getId());
		assertNotNull(part1.getCreatedOn());
		participantsToDelete.add(part1);
		assertEquals(initialCount + 1, entityServletHelper.getParticipantCount(eval1.getId()));
		
		// query, just checking basic wiring
		PaginatedResults<Evaluation> pr = entityServletHelper.getAvailableEvaluations(userName, null);
		assertEquals(1L, pr.getTotalNumberOfResults());
		// get the new etag (changed when participant was added?)
		eval1 = entityServletHelper.getEvaluation(eval1.getId());
		assertEquals(eval1, pr.getResults().iterator().next());
		// make sure 'status' parameter is wired up
		assertEquals(0, entityServletHelper.getAvailableEvaluations(userName, "PLANNED").getTotalNumberOfResults());
		assertEquals(1, entityServletHelper.getAvailableEvaluations(userName, "OPEN").getTotalNumberOfResults());
		
		// read
		Participant clone = entityServletHelper.getParticipant(userId, eval1.getId());
		assertEquals(part1, clone);
		
		// delete
		entityServletHelper.deleteParticipant(ownerName, userId, eval1.getId());
		try {
			entityServletHelper.getParticipant(userId, eval1.getId());
			fail("Failed to delete Participant " + part1.toString());
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getParticipantCount(eval1.getId()));
	}
	
	@Test
	public void testSubmissionRoundTrip() throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException, IOException, ServletException, InterruptedException {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = entityServletHelper.createEvaluation(eval1, ownerName);
		evaluationsToDelete.add(eval1.getId());
		part1 = entityServletHelper.createParticipant(userName, eval1.getId());
		participantsToDelete.add(part1);
		UserInfo userInfo = userManager.getUserInfo(userName);
		String nodeId = createNode("An entity", userInfo);
		assertNotNull(nodeId);
		nodesToDelete.add(nodeId);
		
		long initialCount = entityServletHelper.getSubmissionCount(eval1.getId());
		
		// create
		Node node = nodeManager.get(userInfo, nodeId);
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(nodeId);
		sub1 = entityServletHelper.createSubmission(sub1, userName, node.getETag());
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		assertEquals(initialCount + 1, entityServletHelper.getSubmissionCount(eval1.getId()));
		
		// read
		Submission clone = entityServletHelper.getSubmission(userName, sub1.getId());
		assertEquals(sub1, clone);
		SubmissionStatus status = entityServletHelper.getSubmissionStatus(sub1.getId());
		assertNotNull(status);
		assertEquals(sub1.getId(), status.getId());
		assertEquals(SubmissionStatusEnum.OPEN, status.getStatus());
		
		// update
		Thread.sleep(1L);
		status.setScore(0.5);
		status.setStatus(SubmissionStatusEnum.SCORED);
		SubmissionStatus statusClone = entityServletHelper.updateSubmissionStatus(status, ownerName);
		assertFalse("Modified date was not updated", status.getModifiedOn().equals(statusClone.getModifiedOn()));
		status.setModifiedOn(statusClone.getModifiedOn());
		assertFalse("Etag was not updated", status.getEtag().equals(statusClone.getEtag()));
		status.setEtag(statusClone.getEtag());
		assertEquals(status, statusClone);
		assertEquals(initialCount + 1, entityServletHelper.getSubmissionCount(eval1.getId()));
		
		// delete
		entityServletHelper.deleteSubmission(sub1.getId(), ownerName);
		try {
			entityServletHelper.deleteSubmission(sub1.getId(), ownerName);
			fail("Failed to delete Submission " + sub1.toString());
		} catch (NotFoundException e) {
			// expected
		}
		assertEquals(initialCount, entityServletHelper.getSubmissionCount(eval1.getId()));
	}
	

	
	@Test(expected=IllegalArgumentException.class)
	public void testSubmissionUnauthorized() throws NotFoundException, DatastoreException, JSONObjectAdapterException, IOException, ServletException {		
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = entityServletHelper.createEvaluation(eval1, ownerName);
		evaluationsToDelete.add(eval1.getId());
		part1 = entityServletHelper.createParticipant(userName, eval1.getId());
		participantsToDelete.add(part1);
		UserInfo ownerInfo = userManager.getUserInfo(ownerName);
		String nodeId = createNode("An entity", ownerInfo);
		assertNotNull(nodeId);
		nodesToDelete.add(nodeId);
		Node node = nodeManager.get(ownerInfo, nodeId);
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(nodeId);
		sub1 = entityServletHelper.createSubmission(sub1, userName, node.getETag());
	}
	
	@Test
	public void testPaginated() throws DatastoreException, JSONObjectAdapterException, IOException, NotFoundException, ServletException {
		// create objects
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = entityServletHelper.createEvaluation(eval1, ownerName);
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		eval2 = entityServletHelper.createEvaluation(eval2, ownerName);
		assertNotNull(eval2.getId());
		evaluationsToDelete.add(eval2.getId());
		
		part1 = entityServletHelper.createParticipant(ownerName, eval1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
		part2 = entityServletHelper.createParticipant(userName, eval1.getId());
		assertNotNull(part2);
		participantsToDelete.add(part2);
		
		// fetch eval1 and verify that eTag has been updated
		String oldEtag = eval1.getEtag();
		eval1 = entityServletHelper.getEvaluation(eval1.getId());
		assertFalse("Etag was not updated", oldEtag.equals(eval1.getEtag()));
		
		UserInfo userInfo = userManager.getUserInfo(userName);
		String node1 = createNode("entity1", userInfo);
		assertNotNull(node1);
		String etag1 = nodeManager.get(userInfo, node1).getETag();
		nodesToDelete.add(node1);
		String node2 = createNode("entity2", userInfo);
		String etag2 = nodeManager.get(userInfo, node2).getETag();
		assertNotNull(node2);
		nodesToDelete.add(node2);
		
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(node1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(userName);
		sub1 = entityServletHelper.createSubmission(sub1, userName, etag1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());		
		sub2.setEvaluationId(eval1.getId());
		sub2.setEntityId(node2);
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = entityServletHelper.createSubmission(sub2, userName, etag2);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
		
		// paginated evaluations
		PaginatedResults<Evaluation> evals = entityServletHelper.getEvaluationsPaginated(10, 0);
		assertEquals(2, evals.getTotalNumberOfResults());
		for (Evaluation c : evals.getResults())
			assertTrue("Unknown Evaluation returned: " + c.toString(), c.equals(eval1) || c.equals(eval2));
		
		// paginated participants
		PaginatedResults<Participant> parts = entityServletHelper.getAllParticipants(eval1.getId());
		assertEquals(2, parts.getTotalNumberOfResults());
		for (Participant p : parts.getResults())
			assertTrue("Unknown Participant returned: " + p.toString(), p.equals(part1) || p.equals(part2));
		
		parts = entityServletHelper.getAllParticipants(eval2.getId());
		assertEquals(0, parts.getTotalNumberOfResults());
		
		// paginated submissions
		PaginatedResults<Submission> subs = entityServletHelper.getAllSubmissions(ownerName, eval1.getId(), null);
		assertEquals(2, subs.getTotalNumberOfResults());
		for (Submission s : subs.getResults())
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		
		subs = entityServletHelper.getAllSubmissions(ownerName, eval1.getId(), SubmissionStatusEnum.CLOSED);
		assertEquals(0, subs.getTotalNumberOfResults());
		
		subs = entityServletHelper.getAllSubmissions(ownerName, eval2.getId(), null);
		assertEquals(0, subs.getTotalNumberOfResults());
	}
	
	private String createNode(String name, UserInfo userInfo) throws DatastoreException, InvalidModelException, NotFoundException {
		Node toCreate = new Node();
		toCreate.setName(name);
		String ownerId = userInfo.getIndividualGroup().getId();
		toCreate.setCreatedByPrincipalId(Long.parseLong(ownerId));
		toCreate.setModifiedByPrincipalId(Long.parseLong(ownerId));
		toCreate.setCreatedOn(new Date(System.currentTimeMillis()));
		toCreate.setModifiedOn(toCreate.getCreatedOn());
		toCreate.setNodeType(EntityType.project.name());
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("1");
    	String id = nodeManager.createNewNode(toCreate, userInfo);
    	nodesToDelete.add(KeyFactory.stringToKey(id).toString());
    	return id;
	}

}
