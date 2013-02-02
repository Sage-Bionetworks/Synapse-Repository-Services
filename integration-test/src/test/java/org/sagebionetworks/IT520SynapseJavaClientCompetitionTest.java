package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.competition.model.SubmissionBundle;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Exercise the Competition Services methods in the Synapse Java Client
 * 
 * @author bkng
 */
public class IT520SynapseJavaClientCompetitionTest {

	private static Synapse synapseOne = null;
	private static Synapse synapseTwo = null;
	private static Project project = null;
	private static Study dataset = null;
	private static Project projectTwo = null;
	
	private static String userName;
	
	private static Competition comp1;
	private static Competition comp2;
	private static Participant part1;
	private static Participant part2;
	private static Submission sub1;
	private static Submission sub2;
	
	private static List<String> competitionsToDelete;
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
		competitionsToDelete = new ArrayList<String>();
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
				synapseOne.deleteParticipant(part.getCompetitionId(), part.getUserId());
			} catch (Exception e) {}
		}
		
		// clean up competitions
		for (String id : competitionsToDelete) {
			try {
				synapseOne.deleteCompetition(id);
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
	public void testCompetitionRoundTrip() throws SynapseException {
		Long initialCount = synapseOne.getCompetitionCount();
		
		// Create
		comp1 = synapseOne.createCompetition(comp1);		
		assertNotNull(comp1.getEtag());
		assertNotNull(comp1.getId());
		competitionsToDelete.add(comp1.getId());
		
		// Read
		Competition fetched = synapseOne.getCompetition(comp1.getId());
		assertEquals(comp1, fetched);
		fetched = synapseOne.findCompetition(comp1.getName());
		assertEquals(comp1, fetched);
		
		// Update
		fetched.setDescription(comp1.getDescription() + " (modified)");
		Competition updated = synapseOne.updateCompetition(fetched);
		assertFalse("eTag was not updated", updated.getEtag().equals(fetched.getEtag()));
		fetched.setEtag(updated.getEtag());
		assertEquals(fetched, updated);
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getCompetitionCount());
		
		// Delete
		synapseOne.deleteCompetition(comp1.getId());
		try {
			synapseOne.getCompetition(comp1.getId());
			fail("Delete failed");
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getCompetitionCount());
	}
	
	@Test
	public void testParticipantRoundTrip() throws SynapseException {
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = synapseOne.createCompetition(comp1);
		competitionsToDelete.add(comp1.getId());
		
		Long initialCount = synapseOne.getParticipantCount(comp1.getId());
		
		// create
		part1 = synapseOne.createParticipant(comp1.getId());
		assertNotNull(part1.getCreatedOn());
		participantsToDelete.add(part1);
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getParticipantCount(comp1.getId()));
		
		// read
		Participant clone = synapseOne.getParticipant(comp1.getId(), part1.getUserId());
		assertEquals(part1, clone);
		
		// delete
		synapseOne.deleteParticipant(comp1.getId(), part1.getUserId());
		try {
			synapseOne.getParticipant(comp1.getId(), part1.getUserId());
			fail("Failed to delete Participant " + part1.toString());
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getParticipantCount(comp1.getId()));
	}
	
	@Test
	public void testSubmissionRoundTrip() throws SynapseException, NotFoundException, InterruptedException {
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = synapseOne.createCompetition(comp1);
		competitionsToDelete.add(comp1.getId());
		part1 = synapseOne.createParticipant(comp1.getId());
		participantsToDelete.add(part1);
		String entityId = project.getId();
		assertNotNull(entityId);
		entitiesToDelete.add(entityId);
		
		Long initialCount = synapseOne.getSubmissionCount(comp1.getId());
		
		// create
		sub1.setCompetitionId(comp1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createSubmission(sub1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getSubmissionCount(comp1.getId()));
		
		// read
		Submission clone = synapseOne.getSubmission(sub1.getId());
		assertEquals(sub1, clone);
		SubmissionStatus status = synapseOne.getSubmissionStatus(sub1.getId());
		assertNotNull(status);
		assertEquals(sub1.getId(), status.getId());
		assertEquals(SubmissionStatusEnum.OPEN, status.getStatus());
		
		// update
		Thread.sleep(1L);
		status.setScore(0.5);
		status.setStatus(SubmissionStatusEnum.SCORED);
		SubmissionStatus statusClone = synapseOne.updateSubmissionStatus(status);
		assertFalse("Modified date was not updated", status.getModifiedOn().equals(statusClone.getModifiedOn()));
		status.setModifiedOn(statusClone.getModifiedOn());
		assertFalse("Etag was not updated", status.getEtag().equals(statusClone.getEtag()));
		status.setEtag(statusClone.getEtag());
		assertEquals(status, statusClone);
		assertEquals(newCount, synapseOne.getSubmissionCount(comp1.getId()));
		
		// delete
		synapseOne.deleteSubmission(sub1.getId());
		try {
			synapseOne.deleteSubmission(sub1.getId());
			fail("Failed to delete Submission " + sub1.toString());
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getSubmissionCount(comp1.getId()));
	}
	
	@Test
	public void testCompetitionsParticipantsPaginated() throws SynapseException {
		// create objects
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = synapseOne.createCompetition(comp1);
		assertNotNull(comp1.getId());
		competitionsToDelete.add(comp1.getId());
		comp2 = synapseOne.createCompetition(comp2);
		assertNotNull(comp2.getId());
		competitionsToDelete.add(comp2.getId());
		
		part1 = synapseOne.createParticipant(comp1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
		
		
		String userId = synapseTwo.getMyProfile().getOwnerId();
		part2 = synapseOne.createParticipantAsAdmin(comp1.getId(), userId);
		assertNotNull(part2);
		participantsToDelete.add(part2);
		
		// fetch comp1 and verify that eTag has been updated
		String oldEtag = comp1.getEtag();
		comp1 = synapseOne.getCompetition(comp1.getId());
		assertFalse("Etag was not updated", oldEtag.equals(comp1.getEtag()));
		
		String entityId1 = project.getId();
		assertNotNull(entityId1);
		entitiesToDelete.add(entityId1);
		String entityId2 = dataset.getId();
		assertNotNull(entityId2);
		entitiesToDelete.add(entityId2);
		
		sub1.setCompetitionId(comp1.getId());
		sub1.setEntityId(entityId1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(userName);
		sub1 = synapseOne.createSubmission(sub1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());		
		sub2.setCompetitionId(comp1.getId());
		sub2.setEntityId(entityId2);
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = synapseOne.createSubmission(sub2);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
		
		// paginated competitions
		PaginatedResults<Competition> comps = synapseOne.getCompetitionsPaginated(10, 0);
		assertEquals(2, comps.getTotalNumberOfResults());
		for (Competition c : comps.getResults())
			assertTrue("Unknown Competition returned: " + c.toString(), c.equals(comp1) || c.equals(comp2));
		
		// paginated participants
		PaginatedResults<Participant> parts = synapseOne.getAllParticipants(comp1.getId(), 10, 0);
		assertEquals(2, parts.getTotalNumberOfResults());
		for (Participant p : parts.getResults())
			assertTrue("Unknown Participant returned: " + p.toString(), p.equals(part1) || p.equals(part2));
		
		parts = synapseOne.getAllParticipants(comp2.getId(), 10, 0);
		assertEquals(0, parts.getTotalNumberOfResults());
	}
	
	@Test
	public void testSubmissionsPaginated() throws SynapseException {
		// create objects
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = synapseOne.createCompetition(comp1);
		assertNotNull(comp1.getId());
		competitionsToDelete.add(comp1.getId());
		comp2 = synapseOne.createCompetition(comp2);
		assertNotNull(comp2.getId());
		competitionsToDelete.add(comp2.getId());
		
		part1 = synapseOne.createParticipant(comp1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
				
		String userId = synapseTwo.getMyProfile().getOwnerId();
		part2 = synapseOne.createParticipantAsAdmin(comp1.getId(), userId);
		assertNotNull(part2);
		participantsToDelete.add(part2);
		
		String entityId1 = project.getId();
		assertNotNull(entityId1);
		entitiesToDelete.add(entityId1);
		String entityId2 = dataset.getId();
		assertNotNull(entityId2);
		entitiesToDelete.add(entityId2);
		
		sub1.setCompetitionId(comp1.getId());
		sub1.setEntityId(entityId1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(userName);
		sub1 = synapseOne.createSubmission(sub1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());		
		sub2.setCompetitionId(comp1.getId());
		sub2.setEntityId(entityId2);
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = synapseOne.createSubmission(sub2);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
				
		// paginated submissions and bundles
		PaginatedResults<Submission> subs;
		PaginatedResults<SubmissionBundle> subBundles;
		
		subs = synapseOne.getAllSubmissions(comp1.getId(), 10, 0);
		subBundles = synapseOne.getAllSubmissionBundles(comp1.getId(), 10, 0);		
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		for (SubmissionBundle bundle : subBundles.getResults()) {
			Submission sub = bundle.getSubmission();
			SubmissionStatus status = bundle.getSubmissionStatus();
			assertTrue("Unknown Submission returned: " + bundle.toString(), sub.equals(sub1) || sub.equals(sub2));
			assertTrue("SubmissionBundle contents do not match: " + bundle.toString(), sub.getId().equals(status.getId()));
		}
		
		
		subs = synapseOne.getAllSubmissionsByStatus(comp1.getId(), SubmissionStatusEnum.OPEN, 10, 0);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(comp1.getId(), SubmissionStatusEnum.OPEN, 10, 0);
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		for (SubmissionBundle bundle : subBundles.getResults()) {
			Submission sub = bundle.getSubmission();
			SubmissionStatus status = bundle.getSubmissionStatus();
			assertTrue("Unknown Submission returned: " + bundle.toString(), sub.equals(sub1) || sub.equals(sub2));
			assertTrue("SubmissionBundle contents do not match: " + bundle.toString(), sub.getId().equals(status.getId()));
		}
		
		subs = synapseOne.getAllSubmissionsByStatus(comp1.getId(), SubmissionStatusEnum.CLOSED, 10, 0);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(comp1.getId(), SubmissionStatusEnum.CLOSED, 10, 0);
		assertEquals(0, subs.getTotalNumberOfResults());
		assertEquals(0, subBundles.getTotalNumberOfResults());
		assertEquals(0, subs.getResults().size());
		assertEquals(0, subBundles.getResults().size());
		
		subs = synapseOne.getAllSubmissions(comp2.getId(), 10, 0);
		subBundles = synapseOne.getAllSubmissionBundles(comp2.getId(), 10, 0);
		assertEquals(0, subs.getTotalNumberOfResults());
		assertEquals(0, subBundles.getTotalNumberOfResults());
		assertEquals(0, subs.getResults().size());
		assertEquals(0, subBundles.getResults().size());
	}
	
	@Test
	public void testGetMySubmissions() throws SynapseException {
		// create objects
		comp1.setStatus(CompetitionStatus.OPEN);
		comp1 = synapseOne.createCompetition(comp1);
		assertNotNull(comp1.getId());
		competitionsToDelete.add(comp1.getId());
		
		part1 = synapseOne.createParticipant(comp1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
		
		part2 = synapseTwo.createParticipant(comp1.getId());
		assertNotNull(part2);
		participantsToDelete.add(part2);
		
		String entityId1 = project.getId();
		assertNotNull(entityId1);
		entitiesToDelete.add(entityId1);
		String entityId2 = dataset.getId();
		assertNotNull(entityId2);
		entitiesToDelete.add(entityId2);
		
		sub1.setCompetitionId(comp1.getId());
		sub1.setEntityId(entityId1);
		sub1.setVersionNumber(1L);
		sub1.setUserId(userName);
		sub1 = synapseOne.createSubmission(sub1);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		
		sub2.setCompetitionId(comp1.getId());
		sub2.setEntityId(projectTwo.getId());
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = synapseTwo.createSubmission(sub2);
		assertNotNull(sub2.getId());
		submissionsToDelete.add(sub2.getId());
		
		// paginated submissions
		PaginatedResults<Submission> subs = synapseOne.getMySubmissions(comp1.getId(), 10, 0);
		assertEquals(1, subs.getTotalNumberOfResults());
		for (Submission s : subs.getResults())
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1));

	}
}