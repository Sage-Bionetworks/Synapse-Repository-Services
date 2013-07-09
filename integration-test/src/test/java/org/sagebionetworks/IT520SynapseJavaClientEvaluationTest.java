package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationState;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
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
	private static S3FileHandle fileHandle = null;
	
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
	private static List<Long> accessRequirementsToDelete;
	
	public static final long MAX_WAIT_MS = 1000*10; // 10 sec	
	private static String FILE_NAME = "LittleImage.png";

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
		synapse.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
		synapse.login(user, pw);
		
		return synapse;
	}
	
	@Before
	public void before() throws DatastoreException, NotFoundException, SynapseException {
		evaluationsToDelete = new ArrayList<String>();
		participantsToDelete = new ArrayList<Participant>();
		submissionsToDelete = new ArrayList<String>();
		entitiesToDelete = new ArrayList<String>();
		accessRequirementsToDelete = new ArrayList<Long>();
		
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
        eval1.setContentSource(project.getId());
        eval1.setStatus(EvaluationStatus.PLANNED);
        eval2 = new Evaluation();
		eval2.setName("name2");
		eval2.setDescription("description");
        eval2.setContentSource(project.getId());
        eval2.setStatus(EvaluationStatus.PLANNED);
        
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
				synapseOne.deleteSubmission(id);
			} catch (Exception e) {}
		};
		
		// clean up participants
		for (Participant part : participantsToDelete) {
			try {
				synapseOne.deleteParticipant(part.getEvaluationId(), part.getUserId());
			} catch (Exception e) {}
		}
		
		// clean up Access Requirements
		for (Long id : accessRequirementsToDelete) {
			try {
				synapseOne.deleteAccessRequirement(id);
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
		// clean up FileHandle
		if(fileHandle != null){
			try {
				synapseOne.deleteFileHandle(fileHandle.getId());
			} catch (Exception e) {}
		}
	}
	
	@Test
	public void testEvaluationRestrictionRoundTrip() throws SynapseException, UnsupportedEncodingException {
		Long initialCount = synapseOne.getEvaluationCount();
		
		// Create Evaluation
		eval1 = synapseOne.createEvaluation(eval1);		
		assertNotNull(eval1.getEtag());
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getEvaluationCount());

		// Create AccessRestriction
		TermsOfUseAccessRequirement tou = new TermsOfUseAccessRequirement();
		tou.setAccessType(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.EVALUATION);
		subjectId.setId(eval1.getId());
		tou.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		tou = synapseOne.createAccessRequirement(tou);
		assertNotNull(tou.getId());
		accessRequirementsToDelete.add(tou.getId());
		
		// Query AccessRestriction
		VariableContentPaginatedResults<AccessRequirement> paginatedResults;
		paginatedResults = synapseTwo.getAccessRequirements(subjectId);
		checkTOUlist(paginatedResults, tou);
		
		// Query Unmet AccessRestriction
		paginatedResults = synapseTwo.getUnmetAccessRequirements(subjectId);
		checkTOUlist(paginatedResults, tou);
		
		// Create AccessApproval
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setRequirementId(tou.getId());
		synapseTwo.createAccessApproval(aa);
		
		// Query AccessRestriction
		paginatedResults = synapseTwo.getAccessRequirements(subjectId);
		checkTOUlist(paginatedResults, tou);
		
		// Query Unmet AccessRestriction (since the requirement is now met, the list is empty)
		paginatedResults = synapseTwo.getUnmetAccessRequirements(subjectId);
		assertEquals(0L, paginatedResults.getTotalNumberOfResults());
		assertTrue(paginatedResults.getResults().isEmpty());
	}
	
	// check that a paginated results wrapping a ToU matches a given ToU
	private static void checkTOUlist(VariableContentPaginatedResults<AccessRequirement> pagingatedResults, TermsOfUseAccessRequirement tou) {
		assertEquals(1L, pagingatedResults.getTotalNumberOfResults());
		List<AccessRequirement> ars = pagingatedResults.getResults();
		assertEquals(1, ars.size());
		AccessRequirement ar = ars.iterator().next();
		assertTrue(ar instanceof TermsOfUseAccessRequirement);
		TermsOfUseAccessRequirement tou2 = (TermsOfUseAccessRequirement)ar;
		assertEquals(tou.getAccessType(), tou2.getAccessType());
		assertEquals(tou.getSubjectIds(), tou2.getSubjectIds());	
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
		
		// query before joining
		PaginatedResults<Evaluation> evals = synapseOne.getAvailableEvaluationsPaginated(null, 0, 100);
		assertEquals(0, evals.getTotalNumberOfResults());
		
		// create
		part1 = synapseOne.createParticipant(eval1.getId());
		assertNotNull(part1.getCreatedOn());
		participantsToDelete.add(part1);
		Long newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getParticipantCount(eval1.getId()));
		
		// query after joining
		evals = synapseOne.getAvailableEvaluationsPaginated(null, 0, 100);
		assertEquals(1, evals.getTotalNumberOfResults());
		assertEquals(1, evals.getResults().size());
		eval1=synapseOne.getEvaluation(eval1.getId());
		assertEquals(eval1, evals.getResults().iterator().next());
		
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
		
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(true);
		sa.setKey("foo");
		sa.setValue("bar");
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		stringAnnos.add(sa);
		Annotations annos = new Annotations();
		annos.setStringAnnos(stringAnnos);
		
		status.setScore(0.5);
		status.setStatus(SubmissionStatusEnum.SCORED);
		status.setReport("Lorem ipsum");
		status.setAnnotations(annos);
		
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
		int partsMask = ServiceConstants.DEFAULT_ENTITYBUNDLE_MASK_FOR_SUBMISSIONS;
		EntityBundle bundle = synapseOne.getEntityBundle(entityId, partsMask);
		EntityBundle clone = new EntityBundle();
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		clone.initializeFromJSONObject(joa.createNew(sub1.getEntityBundleJSON()));
		// we don't care if etags have changed
		clone.getEntity().setEtag(null);
		clone.getAnnotations().setEtag(null);
		bundle.getEntity().setEtag(null);
		bundle.getAnnotations().setEtag(null);
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
				
		// paginated submissions, statuses, and bundles
		PaginatedResults<Submission> subs;
		PaginatedResults<SubmissionStatus> subStatuses;
		PaginatedResults<SubmissionBundle> subBundles;
		
		subs = synapseOne.getAllSubmissions(eval1.getId(), 0, 10);
		subStatuses = synapseOne.getAllSubmissionStatuses(eval1.getId(), 0, 10);
		subBundles = synapseOne.getAllSubmissionBundles(eval1.getId(), 0, 10);		
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subStatuses.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		assertEquals(2, subs.getResults().size());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		assertEquals(2, subStatuses.getResults().size());
		for (SubmissionStatus status : subStatuses.getResults()) {
			assertTrue("Unknown SubmissionStatus returned: " + status.toString(),
					status.getId().equals(sub1.getId()) || status.getId().equals(sub2.getId()));
		}
		assertEquals(2, subBundles.getResults().size());
		for (SubmissionBundle bundle : subBundles.getResults()) {
			Submission sub = bundle.getSubmission();
			SubmissionStatus status = bundle.getSubmissionStatus();
			assertTrue("Unknown Submission returned: " + bundle.toString(), sub.equals(sub1) || sub.equals(sub2));
			assertTrue("SubmissionBundle contents do not match: " + bundle.toString(), sub.getId().equals(status.getId()));
		}
				
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 10);
		subStatuses = synapseOne.getAllSubmissionStatusesByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 10);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(eval1.getId(), SubmissionStatusEnum.OPEN, 0, 10);
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		assertEquals(2, subs.getResults().size());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		assertEquals(2, subStatuses.getResults().size());
		for (SubmissionStatus status : subStatuses.getResults()) {
			assertTrue("Unknown SubmissionStatus returned: " + status.toString(),
					status.getId().equals(sub1.getId()) || status.getId().equals(sub2.getId()));
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
	
	@Test
	public void testGetFileTemporaryUrlForSubmissionFileHandle() throws Exception {
		// create Objects
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		
		part1 = synapseOne.createParticipant(eval1.getId());
		assertNotNull(part1);
		participantsToDelete.add(part1);
				
		FileEntity file = createTestFileEntity();		
		
		// create Submission
		String entityId = file.getId();
		String entityEtag = file.getEtag();
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createSubmission(sub1, entityEtag);
		submissionsToDelete.add(sub1.getId());

		// get file URL
		URL expected = synapseOne.getFileEntityTemporaryUrlForCurrentVersion(file.getId());
		URL actual = synapseOne.getFileTemporaryUrlForSubmissionFileHandle(sub1.getId(), fileHandle.getId());
		assertEquals("invalid URL returned", expected, actual);
	}

	private FileEntity createTestFileEntity() throws SynapseException {
		// create a FileHandle
		URL url = IT054FileEntityTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		File imageFile = new File(url.getFile().replaceAll("%20", " "));
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		FileHandleResults results = synapseOne.createFileHandles(list);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		fileHandle = (S3FileHandle) results.getList().get(0);
		
		// create a FileEntity
		FileEntity file = new FileEntity();
		file.setName("IT520SynapseJavaClientEvaluationTest.testGetFileTemporaryUrlForSubmissionFileHandle");
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle.getId());
		file = synapseOne.createEntity(file);
		assertNotNull(file);
		entitiesToDelete.add(file.getId());
		return file;
	}

	@Test
	public void testGetUserEvaluationStateRegistered() throws Exception{
		//base case, OPEN competition where user is a participant
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);		
		evaluationsToDelete.add(eval1.getId());
		// create
		part1 = synapseOne.createParticipant(eval1.getId());
		assertNotNull(part1.getCreatedOn());
		participantsToDelete.add(part1);

		UserEvaluationState state = synapseOne.getUserEvaluationState(eval1.getId());
		assertEquals(UserEvaluationState.EVAL_OPEN_USER_REGISTERED, state);
	}
	
	@Test
	public void testGetUserEvaluationStateUnregistered() throws Exception{
		//OPEN competition where user is not yet a participant
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);		
		evaluationsToDelete.add(eval1.getId());

		UserEvaluationState state = synapseOne.getUserEvaluationState(eval1.getId());
		assertEquals(UserEvaluationState.EVAL_OPEN_USER_NOT_REGISTERED, state);
	}
	
	@Test
	public void testGetUserEvaluationStateNotOpen() throws Exception{
		//evaluation is in some state, other than OPEN.  Could be CLOSED, COMPLETED, or PLANNED.
		//in all cases, registration is unavailable
		eval1.setStatus(EvaluationStatus.CLOSED);
		eval1 = synapseOne.createEvaluation(eval1);		
		evaluationsToDelete.add(eval1.getId());
		UserEvaluationState state = synapseOne.getUserEvaluationState(eval1.getId());
		assertEquals(UserEvaluationState.EVAL_REGISTRATION_UNAVAILABLE, state);
		
		eval1.setStatus(EvaluationStatus.COMPLETED);
		eval1 = synapseOne.updateEvaluation(eval1);		
		state = synapseOne.getUserEvaluationState(eval1.getId());
		assertEquals(UserEvaluationState.EVAL_REGISTRATION_UNAVAILABLE, state);

		eval1.setStatus(EvaluationStatus.PLANNED);
		eval1 = synapseOne.updateEvaluation(eval1);		
		state = synapseOne.getUserEvaluationState(eval1.getId());
		assertEquals(UserEvaluationState.EVAL_REGISTRATION_UNAVAILABLE, state);
	}
}