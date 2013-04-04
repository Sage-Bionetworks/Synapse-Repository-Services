package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Exercise the Evaluation Services methods in the Synapse Java Client
 * 
 * @author bkng
 */
public class IT520SynapseJavaClientEvaluationTest {

	private static Synapse synapseOne = null;
	private static Synapse synapseTwo = null;
	private static Project project = null;
	private static Study dataset = null;
	private static Project projectTwo = null;
	
	private static String userName;
	
	private static Evaluation eval1;
	private static Evaluation eval2;
	private static Participant part1;
	private static Participant part2;
	private static Submission sub1;
	private static Submission sub2;
	
	private static List<String> evaluationsToDelete;
	private static List<Participant> participantsToDelete;
	private static List<String> submissionsToDelete;
	private static List<String> entitiesToDelete;

	@BeforeClass
	public static void beforeClass() throws Exception {

		synapseOne = createSynapseClient(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		synapseTwo = createSynapseClient(StackConfiguration.getIntegrationTestUserTwoName(),
				StackConfiguration.getIntegrationTestUserTwoPassword());
	}
	
	private static Synapse createSynapseClient(String user, String pw) throws SynapseException {
		Synapse synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(user, pw);
		
		return synapse;
	}
	
	@Before
	public void before() throws DatastoreException, NotFoundException, SynapseException {
		evaluationsToDelete = new ArrayList<String>();
		participantsToDelete = new ArrayList<Participant>();
		submissionsToDelete = new ArrayList<String>();
		entitiesToDelete = new ArrayList<String>();
		
		// create Entities
		project = synapseOne.createEntity(new Project());
		dataset = new Study();
		dataset.setParentId(project.getId());
		dataset = synapseOne.createEntity(dataset);
		projectTwo = synapseTwo.createEntity(new Project());
		
		entitiesToDelete.add(project.getId());
		entitiesToDelete.add(dataset.getId());
		entitiesToDelete.add(projectTwo.getId());
		
		// initialize Evaluations
		eval1 = new Evaluation();
		eval1.setName("some name");
		eval1.setDescription("description");
        eval1.setContentSource("contentSource");
        eval1.setStatus(EvaluationStatus.PLANNED);
        eval2 = new Evaluation();
		eval2.setName("name2");
		eval2.setDescription("description");
        eval2.setContentSource("contentSource");
        eval2.setStatus(EvaluationStatus.PLANNED);
        
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
				synapseOne.deleteSubmission(id);
			} catch (Exception e) {}
		};
		
		// clean up participants
		for (Participant part : participantsToDelete) {
			try {
				synapseOne.deleteParticipant(part.getEvaluationId(), part.getUserId());
			} catch (Exception e) {}
		}
		
		// clean up evaluations
		for (String id : evaluationsToDelete) {
			try {
				synapseOne.deleteEvaluation(id);
			} catch (Exception e) {}
		}
		
		// clean up nodes
		for (String id : entitiesToDelete) {
			try {
				synapseOne.deleteEntityById(id);
			} catch (Exception e) {}
		}
	}
	
	@Test
	public void testEvaluationRoundTrip() throws SynapseException, UnsupportedEncodingException {
		Long initialCount = synapseOne.getEvaluationCount();
		
		// Create
		eval1 = synapseOne.createEvaluation(eval1);		
		assertNotNull(eval1.getEtag());
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getEvaluationCount());
		
		// Read
		Evaluation fetched = synapseOne.getEvaluation(eval1.getId());
		assertEquals(eval1, fetched);
		fetched = synapseOne.findEvaluation(eval1.getName());
		assertEquals(eval1, fetched);
		
		// Update
		fetched.setDescription(eval1.getDescription() + " (modified)");
		Evaluation updated = synapseOne.updateEvaluation(fetched);
		assertFalse("eTag was not updated", updated.getEtag().equals(fetched.getEtag()));
		fetched.setEtag(updated.getEtag());
		assertEquals(fetched, updated);
		
		// Delete
		synapseOne.deleteEvaluation(eval1.getId());
		try {
			synapseOne.getEvaluation(eval1.getId());
			fail("Delete failed");
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getEvaluationCount());
	}
	
	@Test
	public void testParticipantRoundTrip() throws SynapseException {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		
		Long initialCount = synapseOne.getParticipantCount(eval1.getId());
		
		// create
		part1 = synapseOne.createParticipant(eval1.getId());
		assertNotNull(part1.getCreatedOn());
		participantsToDelete.add(part1);
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getParticipantCount(eval1.getId()));
		
		// read
		Participant clone = synapseOne.getParticipant(eval1.getId(), part1.getUserId());
		assertEquals(part1, clone);
		
		// delete
		synapseOne.deleteParticipant(eval1.getId(), part1.getUserId());
		try {
			synapseOne.getParticipant(eval1.getId(), part1.getUserId());
			fail("Failed to delete Participant " + part1.toString());
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getParticipantCount(eval1.getId()));
	}
	
	@Test
	public void testSubmissionRoundTrip() throws SynapseException, NotFoundException, InterruptedException {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		part1 = synapseOne.createParticipant(eval1.getId());
		participantsToDelete.add(part1);
		String entityId = project.getId();
		String entityEtag = project.getEtag();
		assertNotNull(entityId);
		entitiesToDelete.add(entityId);
		
		Long initialCount = synapseOne.getSubmissionCount(eval1.getId());
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createSubmission(sub1, entityEtag);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getSubmissionCount(eval1.getId()));
		
		// read
		Submission clone = synapseOne.getSubmission(sub1.getId());
		assertNotNull(clone.getEntityBundleJSON());
		sub1.setEntityBundleJSON(clone.getEntityBundleJSON());
		assertEquals(sub1, clone);
		SubmissionStatus status = synapseOne.getSubmissionStatus(sub1.getId());
		assertNotNull(status);
		assertEquals(sub1.getId(), status.getId());
		assertEquals(SubmissionStatusEnum.OPEN, status.getStatus());
		
		// update
		Thread.sleep(1L);
		status.setScore(0.5);
		status.setStatus(SubmissionStatusEnum.SCORED);
		status.setReport("Lorem ipsum");
		SubmissionStatus statusClone = synapseOne.updateSubmissionStatus(status);
		assertFalse("Modified date was not updated", status.getModifiedOn().equals(statusClone.getModifiedOn()));
		status.setModifiedOn(statusClone.getModifiedOn());
		assertFalse("Etag was not updated", status.getEtag().equals(statusClone.getEtag()));
		status.setEtag(statusClone.getEtag());
		assertEquals(status, statusClone);
		assertEquals(newCount, synapseOne.getSubmissionCount(eval1.getId()));
		
		// delete
		synapseOne.deleteSubmission(sub1.getId());
		try {
			synapseOne.deleteSubmission(sub1.getId());
			fail("Failed to delete Submission " + sub1.toString());
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getSubmissionCount(eval1.getId()));
	}
	
	@Test
	public void testSubmissionEntityBundle() throws SynapseException, NotFoundException, InterruptedException, JSONObjectAdapterException {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		part1 = synapseOne.createParticipant(eval1.getId());
		participantsToDelete.add(part1);
		String entityId = project.getId();
		String entityEtag = project.getEtag();
		assertNotNull(entityId);
		entitiesToDelete.add(entityId);
		
		Long initialCount = synapseOne.getSubmissionCount(eval1.getId());
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createSubmission(sub1, entityEtag);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getSubmissionCount(eval1.getId()));
		
		// read
		sub1 = synapseOne.getSubmission(sub1.getId());
		
		// verify EntityBundle
		int partsMask = EntityBundle.ENTITY + EntityBundle.ANNOTATIONS;
		EntityBundle bundle = synapseOne.getEntityBundle(entityId, partsMask);
		EntityBundle clone = new EntityBundle();
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		clone.initializeFromJSONObject(joa.createNew(sub1.getEntityBundleJSON()));
		assertEquals(bundle, clone);
		
		// delete
		synapseOne.deleteSubmission(sub1.getId());
		try {
			synapseOne.deleteSubmission(sub1.getId());
			fail("Failed to delete Submission " + sub1.toString());
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getSubmissionCount(eval1.getId()));
	}
	
	@Test
	public void testEvaluationsParticipantsPaginated() throws SynapseException {
		Long initialEvaluationCount = synapseOne.getEvaluationCount();
		
		// create objects
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		eval2 = synapseOne.createEvaluation(eval2);
		assertNotNull(eval2.getId());
		evaluationsToDelete.add(eval2.getId());
		
		part1 = synapseOne.createParticipant(eval1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
				
		String userId = synapseTwo.getMyProfile().getOwnerId();
		part2 = synapseOne.createParticipantAsAdmin(eval1.getId(), userId);
		assertNotNull(part2);
		participantsToDelete.add(part2);

		// paginated evaluations
		eval1 = synapseOne.getEvaluation(eval1.getId());
		eval2 = synapseOne.getEvaluation(eval2.getId());
		PaginatedResults<Evaluation> evals = synapseOne.getEvaluationsPaginated(0, 10);
		assertEquals(initialEvaluationCount + 2, evals.getTotalNumberOfResults());
		Set<Evaluation> evalSet = new HashSet<Evaluation>();
		evalSet.addAll(evals.getResults());
		assertTrue(evalSet.contains(eval1));
		assertTrue(evalSet.contains(eval2));
		
		// paginated participants
		PaginatedResults<Participant> parts = synapseOne.getAllParticipants(eval1.getId(), 0, 10);
		assertEquals(2, parts.getTotalNumberOfResults());
		for (Participant p : parts.getResults())
			assertTrue("Unknown Participant returned: " + p.toString(), p.equals(part1) || p.equals(part2));
		
		parts = synapseOne.getAllParticipants(eval2.getId(), 0, 10);
		assertEquals(0, parts.getTotalNumberOfResults());
	}
	
	@Test
	public void testSubmissionsPaginated() throws SynapseException {
		// create objects
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		eval2 = synapseOne.createEvaluation(eval2);
		assertNotNull(eval2.getId());
		evaluationsToDelete.add(eval2.getId());
		
		part1 = synapseOne.createParticipant(eval1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
				
		String userId = synapseTwo.getMyProfile().getOwnerId();
		part2 = synapseOne.createParticipantAsAdmin(eval1.getId(), userId);
		assertNotNull(part2);
		participantsToDelete.add(part2);
		
		String entityId1 = project.getId();
		String entityEtag1 = project.getEtag();
		assertNotNull(entityId1);
		entitiesToDelete.add(entityId1);
		String entityId2 = dataset.getId();
		String entityEtag2 = dataset.getEtag();
		assertNotNull(entityId2);
		entitiesToDelete.add(entityId2);
		
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(userName);
		sub1 = synapseOne.createSubmission(sub1, entityEtag1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());		
		sub2.setEvaluationId(eval1.getId());
		sub2.setEntityId(entityId2);
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = synapseOne.createSubmission(sub2, entityEtag2);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
				
		// paginated submissions and bundles
		PaginatedResults<Submission> subs;
		PaginatedResults<SubmissionBundle> subBundles;
		
		subs = synapseOne.getAllSubmissions(eval1.getId(), 0, 10);
		subBundles = synapseOne.getAllSubmissionBundles(eval1.getId(), 0, 10);		
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		assertEquals(2, subs.getResults().size());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		assertEquals(2, subBundles.getResults().size());
		for (SubmissionBundle bundle : subBundles.getResults()) {
			Submission sub = bundle.getSubmission();
			SubmissionStatus status = bundle.getSubmissionStatus();
			assertTrue("Unknown Submission returned: " + bundle.toString(), sub.equals(sub1) || sub.equals(sub2));
			assertTrue("SubmissionBundle contents do not match: " + bundle.toString(), sub.getId().equals(status.getId()));
		}
				
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 10);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 10);
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		assertEquals(2, subs.getResults().size());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		assertEquals(2, subBundles.getResults().size());
		for (SubmissionBundle bundle : subBundles.getResults()) {
			Submission sub = bundle.getSubmission();
			SubmissionStatus status = bundle.getSubmissionStatus();
			assertTrue("Unknown Submission returned: " + bundle.toString(), sub.equals(sub1) || sub.equals(sub2));
			assertTrue("SubmissionBundle contents do not match: " + bundle.toString(), sub.getId().equals(status.getId()));
		}
		
		// verify url in PaginatedResults object contains eval ID (PLFM-1774)
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 1);
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(1, subs.getResults().size());
		assertTrue(subs.getPaging().get(PaginatedResults.NEXT_PAGE_FIELD).contains(eval1.getId()));
		
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 10);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 10);
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		assertEquals(2, subs.getResults().size());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.CLOSED, 0, 10);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(eval1.getId(), SubmissionStatusEnum.CLOSED, 0, 10);
		assertEquals(0, subs.getTotalNumberOfResults());
		assertEquals(0, subBundles.getTotalNumberOfResults());
		assertEquals(0, subs.getResults().size());
		assertEquals(0, subBundles.getResults().size());
		
		subs = synapseOne.getAllSubmissions(eval2.getId(), 0, 10);
		subBundles = synapseOne.getAllSubmissionBundles(eval2.getId(), 0, 10);
		assertEquals(0, subs.getTotalNumberOfResults());
		assertEquals(0, subBundles.getTotalNumberOfResults());
		assertEquals(0, subs.getResults().size());
		assertEquals(0, subBundles.getResults().size());
	}
	
	@Test
	public void testGetMySubmissions() throws SynapseException {
		// create objects
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		
		part1 = synapseOne.createParticipant(eval1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
		
		part2 = synapseTwo.createParticipant(eval1.getId());
		assertNotNull(part2);
		participantsToDelete.add(part2);
		
		String entityId1 = project.getId();
		String entityEtag1 = project.getEtag();
		assertNotNull(entityId1);
		entitiesToDelete.add(entityId1);
		String entityId2 = projectTwo.getId();
		String entityEtag2 = projectTwo.getEtag();
		assertNotNull(entityId2);
		entitiesToDelete.add(entityId2);
		
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(userName);
		sub1 = synapseOne.createSubmission(sub1, entityEtag1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		
		sub2.setEvaluationId(eval1.getId());
		sub2.setEntityId(projectTwo.getId());
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = synapseTwo.createSubmission(sub2, entityEtag2);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
		
		// paginated submissions
		PaginatedResults<Submission> subs = synapseOne.getMySubmissions(eval1.getId(), 0, 10);
		assertEquals(1, subs.getTotalNumberOfResults());
		for (Submission s : subs.getResults())
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1));

	}
}