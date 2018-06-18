package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.MemberSubmissionEligibility;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionEligibility;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.query.QueryTableResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * Exercise the Evaluation Services methods in the Synapse Java Client
 * 
 * @author bkng
 */
public class IT520SynapseJavaClientEvaluationTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseClient synapseTwo;
	private static Long user1ToDelete;
	private static Long user2ToDelete;
	
	private Project project = null;
	private Folder dataset = null;
	private Project projectTwo = null;
	private S3FileHandle fileHandle = null;
	
	private static String userName;
	
	private Evaluation eval1;
	private Evaluation eval2;
	private Submission sub1;
	private Submission sub2;
	private File dataSourceFile;
	private FileEntity fileEntity;
	private Team participantTeam;
	
	private List<String> evaluationsToDelete;
	private List<String> submissionsToDelete;
	private List<String> entitiesToDelete;
	private List<String> teamsToDelete;
	private Challenge challenge;

	private static final int RDS_WORKER_TIMEOUT = 2*1000*60; // Two min
	private static final String FILE_NAME = "LittleImage.png";
	
	private static final String MOCK_TEAM_ENDPOINT = "https://www.synapse.org/#Team:";
	private static final String MOCK_NOTIFICATION_UNSUB_ENDPOINT = "https://www.synapse.org#unsub:";
	private static final String MOCK_CHALLENGE_ENDPOINT = "https://synapse.org/#ENTITY:";
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);

		synapseTwo = new SynapseClientImpl();
		user2ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo);
	}
	
	@Before
	public void before() throws DatastoreException, NotFoundException, SynapseException, IOException {
		adminSynapse.clearAllLocks();
		evaluationsToDelete = new ArrayList<String>();
		submissionsToDelete = new ArrayList<String>();
		entitiesToDelete = new ArrayList<String>();
		
		// create Entities
		project = synapseOne.createEntity(new Project());
		dataset = new Folder();
		dataset.setParentId(project.getId());
		dataset = synapseOne.createEntity(dataset);
		projectTwo = synapseTwo.createEntity(new Project());
		
		{
			dataSourceFile = File.createTempFile("integrationTest", ".txt");
			dataSourceFile.deleteOnExit();
			FileWriter writer = new FileWriter(dataSourceFile);
			writer.write("Hello world!");
			writer.close();
			FileHandle fileHandle = synapseOne.multipartUpload(dataSourceFile, null, false, false);
			fileEntity = new FileEntity();
			fileEntity.setParentId(project.getId());
			fileEntity.setDataFileHandleId(fileHandle.getId());
			fileEntity = synapseOne.createEntity(fileEntity);
		}
		
		entitiesToDelete.add(project.getId());
		entitiesToDelete.add(dataset.getId());
		entitiesToDelete.add(projectTwo.getId());
		
		// initialize Evaluations
		eval1 = new Evaluation();
		eval1.setName("some name");
		eval1.setDescription("description");
        eval1.setContentSource(project.getId());
        eval1.setStatus(EvaluationStatus.PLANNED);
        eval1.setSubmissionInstructionsMessage("foo");
        eval1.setSubmissionReceiptMessage("bar");
        eval2 = new Evaluation();
		eval2.setName("name2");
		eval2.setDescription("description");
        eval2.setContentSource(project.getId());
        eval2.setStatus(EvaluationStatus.PLANNED);
        eval2.setSubmissionInstructionsMessage("baz");
        eval2.setSubmissionReceiptMessage("mumble");
        
        // initialize Submissions
        sub1 = new Submission();
        sub1.setName("submission1");
        sub1.setVersionNumber(1L);
        sub1.setSubmitterAlias("Team Awesome!");
        sub2 = new Submission();
        sub2.setName("submission2");
        sub2.setVersionNumber(1L);
        sub2.setSubmitterAlias("Team Even Better!");
        
		teamsToDelete = new ArrayList<String>();
		// create a Team
		participantTeam = new Team();
		participantTeam.setCanPublicJoin(true);
		participantTeam.setName("challenge participant team");
		participantTeam = synapseOne.createTeam(participantTeam);
		teamsToDelete.add(participantTeam.getId());
		
		challenge = new Challenge();
		challenge.setProjectId(project.getId());
		challenge.setParticipantTeamId(participantTeam.getId());
		challenge = adminSynapse.createChallenge(challenge);
	}
	
	@After
	public void after() throws Exception {
		if (challenge!=null) {
			adminSynapse.deleteChallenge(challenge.getId());
		}
		// clean up submissions
		for (String id : submissionsToDelete) {
			try {
				adminSynapse.deleteSubmission(id);
			} catch (SynapseNotFoundException e) {}
		}
		// clean up evaluations
		for (String id : evaluationsToDelete) {
			try {
				adminSynapse.deleteEvaluation(id);
			} catch (SynapseNotFoundException e) {}
		}
		
		// clean up nodes
		for (String id : entitiesToDelete) {
			try {
				adminSynapse.deleteAndPurgeEntityById(id);
			} catch (SynapseNotFoundException e) {}
		}
		
		// clean up FileHandle
		if(fileHandle != null){
			try {
				adminSynapse.deleteFileHandle(fileHandle.getId());
			} catch (SynapseException e) { }
		}
		
		for (String id : teamsToDelete) {
			try {
				adminSynapse.deleteTeam(id);
			} catch (SynapseNotFoundException e) {}
		}
		dataSourceFile=null;
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
		try {
			adminSynapse.deleteUser(user2ToDelete);
		} catch (SynapseException e) { }
	}

	@Test
	public void testEvaluationRoundTrip() throws SynapseException, UnsupportedEncodingException {
		int initialCount = synapseOne.getAvailableEvaluationsPaginated(0, 100).getResults().size();
		
		// Create
		eval1 = synapseOne.createEvaluation(eval1);
		assertNotNull(eval1.getEtag());
		assertNotNull(eval1.getId());
		evaluationsToDelete.add(eval1.getId());
		int newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getAvailableEvaluationsPaginated(0, 100).getResults().size());
		
		// Read
		Evaluation fetched = synapseOne.getEvaluation(eval1.getId());
		assertEquals(eval1, fetched);
		fetched = synapseOne.findEvaluation(eval1.getName());
		assertEquals(eval1, fetched);
		PaginatedResults<Evaluation> evals = synapseOne.getEvaluationByContentSource(project.getId(), 0, 10);
		assertEquals(1, evals.getTotalNumberOfResults());
		fetched = evals.getResults().get(0);
		assertEquals(eval1, fetched);
		
		// Update
		fetched.setDescription(eval1.getDescription() + " (modified)");
		fetched.setSubmissionInstructionsMessage("foobar2");
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
		assertEquals(initialCount, synapseOne.getAvailableEvaluationsPaginated(100, 0).getResults().size());
	}
	
	@Test
	public void testSubmissionRoundTrip() throws SynapseException, NotFoundException, InterruptedException, IOException {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		String entityId = fileEntity.getId();
		String entityEtag = fileEntity.getEtag();
		String entityFileHandleId = fileEntity.getDataFileHandleId();
		assertNotNull(entityId);
		
		int initialCount = synapseOne.getAllSubmissions(eval1.getId(), 0, 100).getResults().size();
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createIndividualSubmission(sub1, entityEtag,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		int newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getAllSubmissions(eval1.getId(), 0, 100).getResults().size());
		
		// read
		Submission clone = synapseOne.getSubmission(sub1.getId());
		assertNotNull(clone.getEntityBundleJSON());
		sub1.setEntityBundleJSON(clone.getEntityBundleJSON());
		assertEquals(sub1, clone);
		SubmissionStatus status = synapseOne.getSubmissionStatus(sub1.getId());
		assertNotNull(status);
		assertEquals(sub1.getId(), status.getId());
		assertEquals(sub1.getEntityId(), status.getEntityId());
		assertEquals(sub1.getVersionNumber(), status.getVersionNumber());
		assertEquals(SubmissionStatusEnum.RECEIVED, status.getStatus());
		
		File target = File.createTempFile("test", null);
		target.deleteOnExit();
		adminSynapse.downloadFromSubmission(sub1.getId(), entityFileHandleId, target);
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(this.dataSourceFile);
		String actualMD5 = MD5ChecksumHelper.getMD5Checksum(target);
		assertEquals(expectedMD5, actualMD5);

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
		status.setCanCancel(true);
		
		SubmissionStatus statusClone = synapseOne.updateSubmissionStatus(status);
		assertFalse("Modified date was not updated", status.getModifiedOn().equals(statusClone.getModifiedOn()));
		status.setModifiedOn(statusClone.getModifiedOn());
		assertFalse("Etag was not updated", status.getEtag().equals(statusClone.getEtag()));
		status.setEtag(statusClone.getEtag());
		status.setStatusVersion(statusClone.getStatusVersion());
		status.getAnnotations().setObjectId(sub1.getId());
		status.getAnnotations().setScopeId(sub1.getEvaluationId());
		assertEquals(status, statusClone);
		assertEquals(newCount, synapseOne.getAllSubmissions(eval1.getId(), 0, 100).getResults().size());
		
		status = statusClone; // 'status' is, once again, the current version
		SubmissionStatusBatch batch = new SubmissionStatusBatch();
		List<SubmissionStatus> statuses = new ArrayList<SubmissionStatus>();
		statuses.add(status);
		batch.setStatuses(statuses);
		batch.setIsFirstBatch(true);
		batch.setIsLastBatch(true);
		BatchUploadResponse batchUpdateResponse = synapseOne.updateSubmissionStatusBatch(eval1.getId(), batch);
		// after last batch there's no 'next batch' token
		assertNull(batchUpdateResponse.getNextUploadToken());

		synapseOne.requestToCancelSubmission(sub1.getId());
		status = synapseOne.getSubmissionStatus(sub1.getId());
		assertTrue(status.getCancelRequested());
		
		// delete
		synapseOne.deleteSubmission(sub1.getId());
		try {
			synapseOne.deleteSubmission(sub1.getId());
			fail("Failed to delete Submission " + sub1.toString());
		} catch (SynapseException e) {
			// expected
		}
		assertEquals(initialCount, synapseOne.getAllSubmissions(eval1.getId(), 100, 0).getResults().size());
	}
	
	private static SubmissionQuota createSubmissionQuota() {
		SubmissionQuota quota = new SubmissionQuota();
		quota.setFirstRoundStart(new Date(System.currentTimeMillis()));
		quota.setNumberOfRounds(1L);
		quota.setRoundDurationMillis(60*1000L); // 60 seconds
		quota.setSubmissionLimit(1L);
		return quota;
	}
	
	private Team createParticipantTeam() throws SynapseException {
		Team myTeam = new Team();
		myTeam.setCanPublicJoin(true);
		myTeam.setName("registered Team");
		myTeam = synapseOne.createTeam(myTeam);
		this.teamsToDelete.add(myTeam.getId());
		ChallengeTeam challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challenge.getId());
		challengeTeam.setTeamId(myTeam.getId());
		// this is the actual Team registration step
		challengeTeam = synapseOne.createChallengeTeam(challengeTeam);
		return myTeam;
	}
	
	@Test
	public void testTeamSubmissionRoundTrip() throws SynapseException, NotFoundException, InterruptedException, IOException {
		eval1.setStatus(EvaluationStatus.OPEN);
		SubmissionQuota quota = createSubmissionQuota();
		eval1.setQuota(quota);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		
		Evaluation evaluationClone = synapseOne.getEvaluation(eval1.getId());
		assertEquals(quota, evaluationClone.getQuota());
		
		String entityId = fileEntity.getId();
		String entityEtag = fileEntity.getEtag();
		assertNotNull(entityId);
		
		int initialCount = synapseOne.getAllSubmissions(eval1.getId(), 0, 100).getResults().size();
		
		// let's register for the challenge!
		Team myTeam = createParticipantTeam();
				
		// am I eligible to submit?
		TeamSubmissionEligibility tse = synapseOne.getTeamSubmissionEligibility(eval1.getId(), myTeam.getId());
		assertEquals(eval1.getId(), tse.getEvaluationId());
		assertEquals(myTeam.getId(), tse.getTeamId());
		SubmissionEligibility teamEligibility = tse.getTeamEligibility();
		assertTrue(teamEligibility.getIsEligible());
		assertFalse(teamEligibility.getIsQuotaFilled());
		assertTrue(teamEligibility.getIsRegistered());
		List<MemberSubmissionEligibility> mseList = tse.getMembersEligibility();
		assertEquals(1, mseList.size());
		MemberSubmissionEligibility mse = mseList.get(0);
		assertFalse(mse.getHasConflictingSubmission());
		assertTrue(mse.getIsEligible());
		assertFalse(mse.getIsQuotaFilled());
		assertTrue(mse.getIsRegistered());
		assertEquals(user1ToDelete, mse.getPrincipalId());
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1.setTeamId(myTeam.getId());
		long submissionEligibilityHash = tse.getEligibilityStateHash();
		sub1 = synapseOne.createTeamSubmission(sub1, entityEtag, ""+submissionEligibilityHash,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		int newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getAllSubmissions(eval1.getId(), 0, 100).getResults().size());
		
		Submission clone = synapseOne.getSubmission(sub1.getId());
		assertEquals(myTeam.getId(), clone.getTeamId());
		assertEquals(1, clone.getContributors().size());
		SubmissionContributor sb = clone.getContributors().iterator().next();
		assertEquals(""+user1ToDelete, sb.getPrincipalId());
		assertNotNull(sb.getCreatedOn());
		
		// add my colleague as a contributor
		SubmissionContributor added = new SubmissionContributor();
		added.setPrincipalId(""+user2ToDelete);
		try {
			synapseOne.addSubmissionContributor(clone.getId(), added);
			fail("UnauthorizedException expected");
		} catch (SynapseForbiddenException e) {
			// as expected
		}
		// can't do it myself, but an admin can do it
		SubmissionContributor created = adminSynapse.addSubmissionContributor(clone.getId(), added);
		assertEquals(""+user2ToDelete, created.getPrincipalId());
		assertNotNull(created.getCreatedOn());
	}
	
	@Test
	public void testTeamSubmissionRoundTripWithNotification() throws Exception {
		eval1.setStatus(EvaluationStatus.OPEN);
		SubmissionQuota quota = createSubmissionQuota();
		eval1.setQuota(quota);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		
		Evaluation evaluationClone = synapseOne.getEvaluation(eval1.getId());
		assertEquals(quota, evaluationClone.getQuota());
		
		String entityId = fileEntity.getId();
		String entityEtag = fileEntity.getEtag();
		
		// let's register for the challenge!
		Team myTeam = createParticipantTeam();
		
		// I want my friend to join my team
		// first, he must register for the challenge
		UserProfile contributorProfile = synapseTwo.getMyProfile();
		synapseTwo.addTeamMember(participantTeam.getId(), contributorProfile.getOwnerId(), null, null);
		// then he has to join my team
		synapseTwo.addTeamMember(myTeam.getId(), contributorProfile.getOwnerId(), null, null);
				
		List<String> contributorEmails = contributorProfile.getEmails();
		assertEquals(1, contributorEmails.size());
		String contributorEmail = contributorEmails.get(0);
		String contributorNotification = EmailValidationUtil.getBucketKeyForEmail(contributorEmail);
		// make sure there is no notification before the submission is created
		if (EmailValidationUtil.doesFileExist(contributorNotification, 2000L))
			EmailValidationUtil.deleteFile(contributorNotification);

		TeamSubmissionEligibility tse = synapseOne.getTeamSubmissionEligibility(eval1.getId(), myTeam.getId());
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1.setTeamId(myTeam.getId());
		SubmissionContributor contributor = new SubmissionContributor();
		contributor.setPrincipalId(contributorProfile.getOwnerId());
		sub1.setContributors(Collections.singleton(contributor));
		long submissionEligibilityHash = tse.getEligibilityStateHash();
		sub1 = synapseOne.createTeamSubmission(sub1, entityEtag, ""+submissionEligibilityHash,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		submissionsToDelete.add(sub1.getId());

		// contributor should get notification
		assertTrue(EmailValidationUtil.doesFileExist(contributorNotification, 60000L));
	}

	@Test
	public void testSubmissionEntityBundle() throws SynapseException, NotFoundException, InterruptedException, JSONObjectAdapterException {
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		String entityId = project.getId();
		String entityEtag = project.getEtag();
		assertNotNull(entityId);
		entitiesToDelete.add(entityId);
		
		int initialCount = synapseOne.getAllSubmissions(eval1.getId(), 0, 100).getResults().size();
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createIndividualSubmission(sub1, entityEtag,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());
		int newCount = initialCount + 1;
		assertEquals(newCount, synapseOne.getAllSubmissions(eval1.getId(), 0, 100).getResults().size());
		
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
		assertEquals(initialCount, synapseOne.getAllSubmissions(eval1.getId(), 100, 0).getResults().size());
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
		sub1 = synapseOne.createIndividualSubmission(sub1, entityEtag1,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());		
		sub2.setEvaluationId(eval1.getId());
		sub2.setEntityId(entityId2);
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = synapseOne.createIndividualSubmission(sub2, entityEtag2,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
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
				
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.RECEIVED, 0, 10);
		subStatuses = synapseOne.getAllSubmissionStatusesByStatus(eval1.getId(), SubmissionStatusEnum.RECEIVED, 0, 10);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(eval1.getId(), SubmissionStatusEnum.RECEIVED, 0, 10);
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
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.RECEIVED, 0, 1);
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(1, subs.getResults().size());
		
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.RECEIVED, 0, 10);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(eval1.getId(), SubmissionStatusEnum.RECEIVED, 0, 10);
		assertEquals(2, subs.getTotalNumberOfResults());
		assertEquals(2, subBundles.getTotalNumberOfResults());
		assertEquals(2, subs.getResults().size());
		for (Submission s : subs.getResults()) {
			assertTrue("Unknown Submission returned: " + s.toString(), s.equals(sub1) || s.equals(sub2));
		}
		
		subs = synapseOne.getAllSubmissionsByStatus(eval1.getId(), SubmissionStatusEnum.SCORED, 0, 10);
		subBundles = synapseOne.getAllSubmissionBundlesByStatus(eval1.getId(), SubmissionStatusEnum.SCORED, 0, 10);
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

		// open the evaluation for user 2 to join
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(12);
		accessSet.add(ACCESS_TYPE.SUBMIT);
		accessSet.add(ACCESS_TYPE.READ);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		String user2Id = synapseTwo.getMyProfile().getOwnerId();
		ra.setPrincipalId(Long.parseLong(user2Id));
		AccessControlList acl = synapseOne.getEvaluationAcl(eval1.getId());
		acl.getResourceAccess().add(ra);
		acl = synapseOne.updateEvaluationAcl(acl);
		assertNotNull(acl);

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
		sub1 = synapseOne.createIndividualSubmission(sub1, entityEtag1,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertNotNull(sub1.getId());
		submissionsToDelete.add(sub1.getId());

		// synapseTwo must join the challenge
		synapseTwo.addTeamMember(participantTeam.getId(), ""+user2ToDelete, MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		sub2.setEvaluationId(eval1.getId());
		sub2.setEntityId(projectTwo.getId());
		sub2.setVersionNumber(1L);
		sub2.setUserId(userName);
		sub2 = synapseTwo.createIndividualSubmission(sub2, entityEtag2,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
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
		
		FileEntity file = createTestFileEntity(project);
		
		// create Submission
		String entityId = file.getId();
		String entityEtag = file.getEtag();
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createIndividualSubmission(sub1, entityEtag,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		submissionsToDelete.add(sub1.getId());

		// get file URL
		String expected = synapseOne.getFileEntityTemporaryUrlForCurrentVersion(file.getId()).toString();
		String actual = synapseOne.getFileTemporaryUrlForSubmissionFileHandle(sub1.getId(), fileHandle.getId()).toString();
		
		// trim time-sensitive params from URL (PLFM-2019)
		String timeSensitiveParameterStart = "&X-Amz-Date";
		expected = expected.substring(0, expected.indexOf(timeSensitiveParameterStart));
		actual = actual.substring(0, actual.indexOf(timeSensitiveParameterStart));
		
		assertEquals("invalid URL returned", expected, actual);
	}

	private FileEntity createTestFileEntity(Entity parent) throws SynapseException, FileNotFoundException, IOException {
		// create a FileHandle
		URL url = IT054FileEntityTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		File imageFile = new File(url.getFile().replaceAll("%20", " "));
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		fileHandle = synapseOne.multipartUpload(imageFile, null, false, false);
		
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
	public void testAclRoundtrip() throws Exception {

		// Create ACL
		eval1 = synapseOne.createEvaluation(eval1);
		assertNotNull(eval1);
		final String evalId = eval1.getId();
		assertNotNull(evalId);
		evaluationsToDelete.add(evalId);

		// Get ACL
		AccessControlList acl = synapseOne.getEvaluationAcl(evalId);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());

		// Get Permissions
		UserEvaluationPermissions uep1 = synapseOne.getUserEvaluationPermissions(evalId);
		assertNotNull(uep1);
		assertTrue(uep1.getCanChangePermissions());
		assertTrue(uep1.getCanDelete());
		assertTrue(uep1.getCanEdit());
		assertFalse(uep1.getCanPublicRead());
		assertTrue(uep1.getCanView());
		UserEvaluationPermissions uep2 = synapseTwo.getUserEvaluationPermissions(evalId);
		assertNotNull(uep2);
		assertFalse(uep2.getCanChangePermissions());
		assertFalse(uep2.getCanDelete());
		assertFalse(uep2.getCanEdit());
		assertFalse(uep2.getCanPublicRead());
		assertFalse(uep2.getCanView());

		// Update ACL
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(12);
		accessSet.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessSet.add(ACCESS_TYPE.DELETE);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		UserSessionData session = synapseTwo.getUserSessionData();
		Long user2Id = Long.parseLong(session.getProfile().getOwnerId());
		ra.setPrincipalId(user2Id);
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		acl = synapseOne.updateEvaluationAcl(acl);
		assertNotNull(acl);
		assertEquals(evalId, acl.getId());

		// Check again for updated permissions
		uep1 = synapseOne.getUserEvaluationPermissions(evalId);
		assertNotNull(uep1);
		uep2 = synapseTwo.getUserEvaluationPermissions(evalId);
		assertNotNull(uep2);
		assertTrue(uep2.getCanChangePermissions());
		assertTrue(uep2.getCanDelete());
		assertFalse(uep2.getCanEdit());
		assertFalse(uep2.getCanPublicRead());
		assertFalse(uep2.getCanView());
	}
	
	@Test
	public void testAnnotationsQuery() throws SynapseException, InterruptedException, JSONObjectAdapterException {
		// set up objects
		eval1.setStatus(EvaluationStatus.OPEN);
		eval1 = synapseOne.createEvaluation(eval1);
		evaluationsToDelete.add(eval1.getId());
		String entityId = project.getId();
		String entityEtag = project.getEtag();
		entitiesToDelete.add(entityId);
		
		// create
		sub1.setEvaluationId(eval1.getId());
		sub1.setEntityId(entityId);
		sub1 = synapseOne.createIndividualSubmission(sub1, entityEtag,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		submissionsToDelete.add(sub1.getId());
		sub2.setEvaluationId(eval1.getId());
		sub2.setEntityId(entityId);
		sub2 = synapseOne.createIndividualSubmission(sub2, entityEtag,
				MOCK_CHALLENGE_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		submissionsToDelete.add(sub2.getId());
		
		String doubleHeader = "DOUBLE";
		double doubleValue = Double.NaN;
		// add annotations
		BatchUploadResponse response = null;
		{
			SubmissionStatus status = synapseOne.getSubmissionStatus(sub1.getId());
			Thread.sleep(1L);		
			StringAnnotation sa = new StringAnnotation();
			sa.setIsPrivate(true);
			sa.setKey("foo");
			sa.setValue("bar");
			List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
			stringAnnos.add(sa);
			Annotations annos = new Annotations();
			annos.setStringAnnos(stringAnnos);		
			
			DoubleAnnotation da = new DoubleAnnotation();
			da.setIsPrivate(true);
			da.setKey(doubleHeader);
			da.setValue(doubleValue);
			List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
			doubleAnnos.add(da);
			annos.setDoubleAnnos(doubleAnnos);					
			
			status.setScore(0.5);
			status.setStatus(SubmissionStatusEnum.SCORED);
			status.setReport("Lorem ipsum");
			status.setAnnotations(annos);
			SubmissionStatusBatch batch = new SubmissionStatusBatch();
			batch.setStatuses(Collections.singletonList(status));
			batch.setIsFirstBatch(true);
			batch.setIsLastBatch(false);
			response = synapseOne.updateSubmissionStatusBatch(eval1.getId(), batch);
		}
		{
			SubmissionStatus status = synapseOne.getSubmissionStatus(sub2.getId());
			Thread.sleep(1L);		
			StringAnnotation sa = new StringAnnotation();
			sa.setIsPrivate(true);
			sa.setKey("foo");
			sa.setValue("bar");
			List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
			stringAnnos.add(sa);
			Annotations annos = new Annotations();
			annos.setStringAnnos(stringAnnos);		
			
			DoubleAnnotation da = new DoubleAnnotation();
			da.setIsPrivate(true);
			da.setKey(doubleHeader);
			da.setValue(doubleValue);
			List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
			doubleAnnos.add(da);
			annos.setDoubleAnnos(doubleAnnos);					
			
			status.setScore(0.5);
			status.setStatus(SubmissionStatusEnum.SCORED);
			status.setReport("Lorem ipsum");
			status.setAnnotations(annos);
			SubmissionStatusBatch batch = new SubmissionStatusBatch();
			batch.setStatuses(Collections.singletonList(status));
			batch.setIsFirstBatch(false);
			batch.setIsLastBatch(true);
			batch.setBatchToken(response.getNextUploadToken());
			response = synapseOne.updateSubmissionStatusBatch(eval1.getId(), batch);
			assertNull(response.getNextUploadToken());
		}

	
		// query for the object
		// we must wait for the annotations to be populated by a worker
		String queryString = "SELECT * FROM evaluation_" + eval1.getId() + " WHERE foo == \"bar\"";
		QueryTableResults results = synapseOne.queryEvaluation(queryString);
		assertNotNull(results);
		long start = System.currentTimeMillis();
		while (results.getTotalNumberOfResults() < 1) {
			long elapsed = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for annotations to be published for query: " + queryString,
					elapsed < RDS_WORKER_TIMEOUT);
			System.out.println("Waiting for annotations to be published... " + elapsed + "ms");		
			Thread.sleep(1000);
			results = synapseOne.queryEvaluation(queryString);
		}
		
		// verify the results
		List<String> headers = results.getHeaders();
		List<org.sagebionetworks.repo.model.query.Row> rows = results.getRows();
		assertEquals(2, rows.size());
		assertTrue(headers.contains("foo"));
		int index = headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID);
		assertTrue(rows.get(0).getValues().get(index).contains(sub1.getId()));
		assertTrue(rows.get(1).getValues().get(index).contains(sub2.getId()));
		
		int nanColumnIndex = headers.indexOf(doubleHeader);
		assertTrue("Expected NaN but found: "+rows.get(0).getValues().get(nanColumnIndex).toString(), 
				rows.get(0).getValues().get(nanColumnIndex).contains(""+doubleValue));
		assertTrue("Expected NaN but found: "+rows.get(1).getValues().get(nanColumnIndex).toString(), 
				rows.get(1).getValues().get(nanColumnIndex).contains(""+doubleValue));
		
		
		// now check that if you delete the submission it stops appearing in the query
		adminSynapse.deleteSubmission(sub1.getId());
		submissionsToDelete.remove(sub1.getId());
		// rerun the query.  We should get just one result (for sub2)
		// we must wait for the annotations to be populated by a worker
		results = synapseOne.queryEvaluation(queryString);
		assertNotNull(results);
		start = System.currentTimeMillis();
		while (results.getTotalNumberOfResults() > 1) {
			long elapsed = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for annotations to be deleted for query: " + queryString,
					elapsed < RDS_WORKER_TIMEOUT);
			System.out.println("Waiting for annotations to be deleted... " + elapsed + "ms");
			Thread.sleep(1000);
			results = synapseOne.queryEvaluation(queryString);
		}
		assertEquals(1, results.getRows().size());
		
	}
}