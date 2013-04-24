package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * This test will push data from a backup into synapse.
 * 
 * @author jmhill
 * 
 */
public class IT100BackupRestoration {

	public static final long TEST_TIME_OUT = 1000 * 60 * 4; // Currently 4 mins

	public static final String BACKUP_FILE_NAME = "Backup-staging-A-66004-4066545524488105200.zip";
	public static final String PRINCIPALS_BACKUP_FILE_NAME = "Backup-principals.zip";
	private static final String S3_DOMAIN = "https://s3.amazonaws.com/";
	private static String S3_WORKFLOW_BUCKET = StackConfiguration.getS3WorkflowBucket();
	private static final String S3_WORKFLOW_URL_PREFIX = S3_DOMAIN + S3_WORKFLOW_BUCKET + "/";
			
	
	private static SynapseAdministration synapse;
	private static AmazonS3Client s3Client;
	private static String bucket;
	
	private List<Entity> toDelete = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Use the synapse client to do some of the work for us.
		synapse = new SynapseAdministration();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());

		System.out.println(StackConfiguration.getPortalEndpoint());
		String iamId = StackConfiguration.getIAMUserId();
		String iamKey = StackConfiguration.getIAMUserKey();
		if (iamId == null)
			throw new IllegalArgumentException("IAM id cannot be null");
		if (iamKey == null)
			throw new IllegalArgumentException("IAM key cannot be null");
		bucket = StackConfiguration.getSharedS3BackupBucket();
		if (bucket == null)
			throw new IllegalArgumentException("Bucket cannot be null null");
		AWSCredentials creds = new BasicAWSCredentials(iamId, iamKey);
		s3Client = new AmazonS3Client(creds);
	}
	
	@After
	public void after() throws Exception {
		if(synapse != null && toDelete != null){
			for(Entity e: toDelete){
				synapse.deleteEntity(e);
			}
		}
	}
	
	@Before
	public void before()throws Exception {
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
		toDelete = new ArrayList<Entity>();
	}

	@Test
	public void restoreFromBackup() throws Exception {

		// restore principals
		{
			// move the file to S3
			URL principalsFileUrl = IT100BackupRestoration.class.getClassLoader()
					.getResource(PRINCIPALS_BACKUP_FILE_NAME);
			File principalBackupFile = new File(principalsFileUrl.getFile().replaceAll("%20", " "));
			assertTrue(principalBackupFile.getAbsolutePath()+" does not exist.", principalBackupFile.exists());
			// Now upload the file to s3
			PutObjectResult putResults = s3Client.putObject(bucket,	PRINCIPALS_BACKUP_FILE_NAME, principalBackupFile);
			System.out.println(putResults);
			
			
			// Start the daemon
			RestoreSubmission submission = new RestoreSubmission();
			submission.setFileName(PRINCIPALS_BACKUP_FILE_NAME);
			BackupRestoreStatus status = synapse.startRestoreDaemon(submission, MigratableObjectType.PRINCIPAL);

			assertNotNull(status);
			assertNotNull(status.getStatus());
			assertFalse(status.getErrorMessage(),DaemonStatus.FAILED == status.getStatus());
			assertTrue(DaemonType.RESTORE == status.getType());
			String restoreId = status.getId();
			assertNotNull(restoreId);
			
			// Wait for it to finish
			waitForDaemon(status.getId());

		}
	}
	
	/**
	 * Test the complete round trip of user migration.
	 * Modified to test PLFM-1425:
	 * - after back up, modify my own user profile
	 * - after restoration, check that old user profile has been restored
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testUserProfileRoundTrip() throws Exception {
		// first create a backup copy of all of the users
		// Start the daemon
		BackupSubmission submission = new BackupSubmission();
		Set<String> allPrincipalIds = synapse.getAllUserAndGroupIds();
		
		UserProfile myUserProfile = synapse.getMyProfile();
		assertTrue(allPrincipalIds.contains(myUserProfile.getOwnerId()));
		// generate a unique string
		String myCompany = "MyCompany";
		myUserProfile.setCompany(myCompany);
		synapse.updateMyProfile(myUserProfile);
		
		submission.setEntityIdsToBackup(allPrincipalIds);
		BackupRestoreStatus status = synapse.startBackupDaemon(submission, MigratableObjectType.PRINCIPAL);
		assertNotNull(status);
		assertNotNull(status.getStatus());
		assertFalse(status.getErrorMessage(),DaemonStatus.FAILED == status.getStatus());
		assertTrue(DaemonType.BACKUP == status.getType());
		String restoreId = status.getId();
		assertNotNull(restoreId);
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		myUserProfile = synapse.getMyProfile();
		// generate a unique string
		String newCompany = "MyCompany-"+System.currentTimeMillis();
		myUserProfile.setCompany(newCompany);
		synapse.updateMyProfile(myUserProfile);
		
		// Now restore the users from this backup file
		RestoreSubmission restoreSub = new RestoreSubmission();
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		restoreSub.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restoreSub, MigratableObjectType.PRINCIPAL);
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// after restoration, user profile should be restored to original value
		myUserProfile = synapse.getMyProfile();
		assertEquals(myCompany, myUserProfile.getCompany());
	}

	// TODO: PLFM-1753 The trash can folder does not exist as the out-of-date
	// backup file is loaded in this test case and overwrites the bootstrap folders
	@Ignore
	public void testTrashedEntityRoundTrip() throws Exception {

		// Create two entities
		Entity entity1 = new Project();
		entity1.setName("IT100BackupRestoration.testUserProfileRoundTrip.1");
		entity1 = synapse.createEntity(entity1);
		assertNotNull(entity1);
		Entity entity2 = new Project();
		entity2.setName("IT100BackupRestoration.testUserProfileRoundTrip.2");
		entity2 = synapse.createEntity(entity2);
		assertNotNull(entity2);

		// Move entity1 to the trash can
		synapse.moveToTrash(entity1.getId());
		PaginatedResults<TrashedEntity> page = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertEquals(1, page.getResults().size());
		assertEquals(entity1.getId(), page.getResults().get(0).getEntityId());

		// Back up the trash can
		Set<String> idSet = new HashSet<String>();
		idSet.add(entity1.getId());
		BackupSubmission submission = new BackupSubmission();
		submission.setEntityIdsToBackup(idSet);
		BackupRestoreStatus status = synapse.startBackupDaemon(submission, MigratableObjectType.TRASHED_ENTITY);
		assertNotNull(status);
		assertNotNull(status.getStatus());
		assertFalse(status.getErrorMessage(),DaemonStatus.FAILED == status.getStatus());
		assertTrue(DaemonType.BACKUP == status.getType());
		String restoreId = status.getId();
		assertNotNull(restoreId);
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());

		// Move entity2 to the trash can
		synapse.moveToTrash(entity2.getId());
		page = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertEquals(2, page.getResults().size());

		// Now restore the trash can from this backup file
		RestoreSubmission restoreSub = new RestoreSubmission();
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		restoreSub.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restoreSub, MigratableObjectType.PRINCIPAL);
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());

		// After restoration, the trash can should be restored to having only entity1
		page = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertEquals(1, page.getResults().size());
		assertEquals(entity1.getId(), page.getResults().get(0).getEntityId());

		// Clean up
		synapse.restoreFromTrash(entity1.getId(), null);
		synapse.deleteEntityById(entity1.getId());
		synapse.deleteEntityById(entity2.getId());
	}

	/**
	 * Test the complete round trip of Access Requirement/Approval migration.
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testAccessRequirementRoundTrip() throws Exception {
		// first create a backup copy of all of the ARs
		// Start the daemon
		BackupSubmission submission = new BackupSubmission();
		// Create an AR, AA and get its ID, then add it's ID here:
		// Create an entity to which an AccessRequirement is added. We don't normally add ARs to projects, but it's convenient to do it here
		Project project = new Project();
		project.setDescription("foo");
		project = synapse.createEntity(project);
		assertNotNull(project);
		toDelete.add(project);
		// now create the access requirement
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setEntityIds(Arrays.asList(new String[]{project.getId()}));
		ar.setEntityType(TermsOfUseAccessRequirement.class.getName());
		ar.setTermsOfUse("foo");
		ar = synapse.createAccessRequirement(ar);
		assertNotNull(ar.getId());
		
		// now create an approval
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		UserProfile up = synapse.getMyProfile();
		String userId = up.getOwnerId();
		aa.setAccessorId(userId);
		aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(ar.getId());
		aa = synapse.createAccessApproval(aa);
		
		submission.setEntityIdsToBackup(new HashSet<String>(Arrays.asList(new String[]{ar.getId().toString()})));
		BackupRestoreStatus status = synapse.startBackupDaemon(submission, MigratableObjectType.ACCESSREQUIREMENT);
		assertNotNull(status);
		assertNotNull(status.getStatus());
		assertFalse(status.getErrorMessage(),DaemonStatus.FAILED == status.getStatus());
		assertTrue(DaemonType.BACKUP == status.getType());
		String restoreId = status.getId();
		assertNotNull(restoreId);
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// delete the access requirement from the system
		MigratableObjectDescriptor mod = new MigratableObjectDescriptor();
		mod.setId(ar.getId().toString());
		mod.setType(MigratableObjectType.ACCESSREQUIREMENT);
		synapse.deleteObject(mod);
		
		// verify that it's deleted
		VariableContentPaginatedResults<AccessRequirement>  vcprs = synapse.getAccessRequirements(project.getId());
		assertEquals(0L, vcprs.getTotalNumberOfResults());
		
		// Now restore the access requirements from this backup file
		RestoreSubmission restoreSub = new RestoreSubmission();
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		restoreSub.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restoreSub, MigratableObjectType.ACCESSREQUIREMENT);
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// verify that it's restored
		vcprs = synapse.getAccessRequirements(project.getId());
		assertEquals(1L, vcprs.getTotalNumberOfResults());
		
		// now clean up the access requirement (cascading to the access approval)
		synapse.deleteObject(mod);
		
	}
	

	@Test
	public void testSingleRoundTrip() throws Exception{
		// Create a project
		Project project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		toDelete.add(project);
		// Now make a backup copy of this entity
		BackupSubmission submission = new BackupSubmission();
		Set<String> set = new HashSet<String>();
		set.add(project.getId());
		submission.setEntityIdsToBackup(set);
		
		BackupRestoreStatus status = synapse.startBackupDaemon(submission, MigratableObjectType.ENTITY);
		assertNotNull(status);
		// Wait for the daemon to complete
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		// extract the file name
		
		
		// Now delete the project
		String projectId = project.getId();
		synapse.deleteEntity(project);
		
		// Now restore the single project
		RestoreSubmission restore = new RestoreSubmission();
		restore.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restore, MigratableObjectType.ENTITY);
		// Wait for the daemon to complete
		status = waitForDaemon(status.getId());
		// Now make sure we can get the project
		project = synapse.getEntity(projectId, Project.class);
		assertNotNull(project);
	}
	
	@Test
	public void testPLFM1464AddACL() throws Exception{
		// Create a project
		Project project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		toDelete.add(project);
		
		// create a child object for this project
		Data data = new Data();
		data.setParentId(project.getId());
		data = synapse.createEntity(data);
		
		// give the child its own ACL
		AccessControlList acl = new AccessControlList();
		acl.setId(data.getId());
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		String myPrincipalId = synapse.getMyProfile().getOwnerId();
		ra.setPrincipalId(Long.parseLong(myPrincipalId));
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})));
		ras.add(ra);
		acl.setResourceAccess(ras);
		acl = synapse.createACL(acl);
		
		// Now make a backup copy of this entity
		BackupSubmission submission = new BackupSubmission();
		Set<String> set = new HashSet<String>();
		set.add(project.getId());
		set.add(data.getId());
		submission.setEntityIdsToBackup(set);
		
		BackupRestoreStatus status = synapse.startBackupDaemon(submission, MigratableObjectType.ENTITY);
		assertNotNull(status);
		// Wait for the daemon to complete
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		// extract the file name
		
		synapse.getACL(data.getId()); // should not generate a SynapseNotFoundException

		// delete the ACL
		synapse.deleteACL(data.getId());
		
		try {
			synapse.getACL(data.getId());
			fail("exception expected");
		} catch (SynapseNotFoundException e) {
			// as expected
		}
		
		// Now restore the single project
		RestoreSubmission restore = new RestoreSubmission();
		restore.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restore, MigratableObjectType.ENTITY);
		// Wait for the daemon to complete
		status = waitForDaemon(status.getId());
		// Now make sure we can get the project
		project = synapse.getEntity(project.getId(), Project.class);
		assertNotNull(project);
		
		// make sure we can see the child object's ACL
		synapse.getACL(data.getId()); // should not generate a SynapseNotFoundException
	}
	
	@Test
	public void testPLFM1464DeleteACL() throws Exception{
		// Create a project
		Project project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		toDelete.add(project);
		
		// create a child object for this project
		Data data = new Data();
		data.setParentId(project.getId());
		data = synapse.createEntity(data);
		
		// Now make a backup copy of this entity
		BackupSubmission submission = new BackupSubmission();
		Set<String> set = new HashSet<String>();
		set.add(project.getId());
		set.add(data.getId());
		submission.setEntityIdsToBackup(set);
		
		BackupRestoreStatus status = synapse.startBackupDaemon(submission, MigratableObjectType.ENTITY);
		assertNotNull(status);
		// Wait for the daemon to complete
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		// extract the file name
		
		try {
			synapse.getACL(data.getId());
			fail("exception expected");
		} catch (SynapseNotFoundException e) {
			// as expected
		}
		
		// give the child its own ACL
		AccessControlList acl = new AccessControlList();
		acl.setId(data.getId());
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		String myPrincipalId = synapse.getMyProfile().getOwnerId();
		ra.setPrincipalId(Long.parseLong(myPrincipalId));
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})));
		ras.add(ra);
		acl.setResourceAccess(ras);
		acl = synapse.createACL(acl);
		
		synapse.getACL(data.getId()); // should not generate a SynapseNotFoundException

		// Now restore the single project
		RestoreSubmission restore = new RestoreSubmission();
		restore.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restore, MigratableObjectType.ENTITY);
		// Wait for the daemon to complete
		status = waitForDaemon(status.getId());
		// Now make sure we can get the project
		project = synapse.getEntity(project.getId(), Project.class);
		assertNotNull(project);
		
		// child should not have an ACL
		try {
			synapse.getACL(data.getId());
			fail("exception expected");
		} catch (SynapseNotFoundException e) {
			// as expected
		}
		
	}
	
	@Test
	public void testGetAndUpdateStatus() throws Exception {
		StackStatus status = synapse.getCurrentStackStatus();
		assertNotNull(status);
		// Set the status
		status.setPendingMaintenanceMessage("Testing that we can set the pending message");
		StackStatus updated = synapse.updateCurrentStackStatus(status);
		assertEquals(status, updated);
		// Clear out the message
		status.setPendingMaintenanceMessage(null);
		updated = synapse.updateCurrentStackStatus(status);
		assertEquals(status, updated);
	}
	
	/**
	 * Extract the filename from the full url.
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl){;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index+1, fullUrl.length());
	}

	/**
	 * Wait for a daemon to complete.
	 * @param daemonId
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * @throws JSONObjectAdapterException
	 * @throws InterruptedException
	 * @throws SynapseException 
	 */
	public BackupRestoreStatus waitForDaemon(String daemonId) throws SynapseException, JSONObjectAdapterException, InterruptedException{
		// Wait for the daemon to finish.
		long start = System.currentTimeMillis();
		while (true) {
			long now = System.currentTimeMillis();
			assertTrue("Timed out waiting for the daemon to complete", now	- start < TEST_TIME_OUT);
			BackupRestoreStatus status = synapse.getDaemonStatus(daemonId);
			assertNotNull(status);
			assertNotNull(status.getStatus());
			// We are done if it failed.
			assertFalse(status.getErrorMessage(), DaemonStatus.FAILED == status.getStatus());
			// Are we done?
			if (DaemonStatus.COMPLETED == status.getStatus()) {
				System.out.println("Restore Complete. Message: "+ status.getProgresssMessage());
				return status;
			} else {
				long current = status.getProgresssCurrent();
				long total = status.getProgresssTotal();
				if (total <= 0) {
					total = 1000000;
				}
				String message = status.getProgresssMessage();
				double percent = ((double) current / (double) total) * 100.0;
				System.out.println("Restore progresss: " + percent+ " % Message: " + message);
			}
			// Wait.
			Thread.sleep(1000);
		}
	}

	/**
	 * Test the complete round trip of Evaluation migration.
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testEvaluationMigrationRoundTrip() throws Exception {
		Long initialEvaluationCount = synapse.getEvaluationCount();
		
		// Create an Evaluation
		Evaluation eval = new Evaluation();
		eval = new Evaluation();
		eval.setId("123");
		eval.setName("my evaluation");
        eval.setContentSource("foobar");
        eval.setStatus(EvaluationStatus.OPEN);
        eval = synapse.createEvaluation(eval);
        assertNotNull(eval);
        assertTrue(synapse.getEvaluationCount().equals(initialEvaluationCount + 1));
        
		// Create a Participant
        Participant part = synapse.createParticipant(eval.getId());
        assertNotNull(part);
        eval = synapse.getEvaluation(eval.getId());
        
		// Verify creation
		assertTrue(synapse.getEvaluationCount().equals(initialEvaluationCount + 1L));
		assertTrue(synapse.getParticipantCount(eval.getId()).equals(1L));
        
        // Start the backup
        BackupSubmission backupSub = new BackupSubmission();
		backupSub.setEntityIdsToBackup(new HashSet<String>(Arrays.asList(new String[]{eval.getId()})));
		BackupRestoreStatus status = synapse.startBackupDaemon(backupSub, MigratableObjectType.EVALUATION);
		assertNotNull(status);
		assertNotNull(status.getStatus());
		assertFalse(status.getErrorMessage(),DaemonStatus.FAILED == status.getStatus());
		assertTrue(DaemonType.BACKUP == status.getType());
		String restoreId = status.getId();
		assertNotNull(restoreId);
		
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// Delete the Evaluation from the system
		MigratableObjectDescriptor mod = new MigratableObjectDescriptor();
		mod.setId(eval.getId());
		mod.setType(MigratableObjectType.EVALUATION);
		synapse.deleteObject(mod);
		
		// Verify deletion
		assertTrue(synapse.getEvaluationCount().equals(initialEvaluationCount));
		assertTrue(synapse.getParticipantCount(eval.getId()).equals(0L));
		
		// Restore from backup
		RestoreSubmission restoreSub = new RestoreSubmission();
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		restoreSub.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restoreSub, MigratableObjectType.EVALUATION);
		
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// Verify restoration
		PaginatedResults<Evaluation> evalsPaginated = synapse.getEvaluationsPaginated(0, 10);
		assertEquals(initialEvaluationCount + 1, evalsPaginated.getTotalNumberOfResults());
		Evaluation evalRestored = evalsPaginated.getResults().get(0);
		assertEquals(eval, evalRestored);
		
		PaginatedResults<Participant> parts = synapse.getAllParticipants(eval.getId(), 0, 10);
		assertEquals(1L, parts.getTotalNumberOfResults());
		Participant partRestored = parts.getResults().get(0);
		assertEquals(part, partRestored);
		
		// Clean up
		synapse.deleteObject(mod);
		try {
			synapse.deleteEvaluation(eval.getId());
		} catch (Exception e) {}
	}
	
	/**
	 * Test the complete round trip of Submission migration.
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testSubmissionMigrationRoundTrip() throws Exception {
		Long initialEvaluationCount = synapse.getEvaluationCount();
		
		// Create Evaluation
		Evaluation eval = new Evaluation();
		eval = new Evaluation();
		eval.setId("123");
		eval.setName("my eval");
        eval.setContentSource("foobar");
        eval.setStatus(EvaluationStatus.OPEN);
        eval = synapse.createEvaluation(eval);
        assertNotNull(eval);
        assertTrue(synapse.getEvaluationCount().equals(initialEvaluationCount + 1));
        
		// Create Participant
        Participant part = synapse.createParticipant(eval.getId());
        assertNotNull(part);
        assertTrue(synapse.getParticipantCount(eval.getId()).equals(1L));
        
        // Create Node
		Project project = new Project();
		project.setDescription("foo");
		project = synapse.createEntity(project);
		assertNotNull(project);
		toDelete.add(project);
		
		// Create Submission
		Submission submission = new Submission();
		submission.setEntityId(project.getId());
		submission.setEvaluationId(eval.getId());
		submission.setName("my submission");
		submission.setVersionNumber(1L);
		submission = synapse.createSubmission(submission, project.getEtag());
				
		// Verify creation
		assertTrue(synapse.getSubmissionCount(eval.getId()).equals(1L));
		SubmissionStatus submissionStatus = synapse.getSubmissionStatus(submission.getId());
        
        // Start the backup
        BackupSubmission backupSub = new BackupSubmission();
		backupSub.setEntityIdsToBackup(new HashSet<String>(Arrays.asList(new String[]{submission.getId()})));
		BackupRestoreStatus status = synapse.startBackupDaemon(backupSub, MigratableObjectType.SUBMISSION);
		assertNotNull(status);
		assertNotNull(status.getStatus());
		assertFalse(status.getErrorMessage(),DaemonStatus.FAILED == status.getStatus());
		assertTrue(DaemonType.BACKUP == status.getType());
		String restoreId = status.getId();
		assertNotNull(restoreId);
		
		// Wait for it to finish
		status = waitForDaemon(restoreId);
		assertNotNull(status.getBackupUrl());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// Delete the Submission from the system
		MigratableObjectDescriptor mod = new MigratableObjectDescriptor();
		mod.setId(submission.getId());
		mod.setType(MigratableObjectType.SUBMISSION);
		synapse.deleteObject(mod);
		
		// Verify deletion
		assertTrue(synapse.getSubmissionCount(eval.getId()).equals(0L));
		
		// Restore from backup
		RestoreSubmission restoreSub = new RestoreSubmission();
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		restoreSub.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restoreSub, MigratableObjectType.SUBMISSION);
		
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
		
		// Verify restoration
		PaginatedResults<Submission> subsPaginated = synapse.getAllSubmissions(eval.getId(), 0, 10);
		assertEquals(1L, subsPaginated.getTotalNumberOfResults());
		assertEquals(1, subsPaginated.getResults().size());
		Submission submissionRestored = subsPaginated.getResults().get(0);
		assertEquals(submission, submissionRestored);
		SubmissionStatus submissionStatusRestored = synapse.getSubmissionStatus(submission.getId());
		assertEquals(submissionStatus, submissionStatusRestored);
		
		// Clean up
		synapse.deleteObject(mod);
		try {
			synapse.deleteEvaluation(eval.getId());
		} catch (Exception e) {}
	}
}
