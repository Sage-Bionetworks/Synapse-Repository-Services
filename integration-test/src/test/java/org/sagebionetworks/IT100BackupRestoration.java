package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;

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

	// This was used to create the backup used for the restoration step.
	@Ignore
	@Test
	public void createSnapshot() throws Exception {
		// Start the daemon
		BackupSubmission submission = new BackupSubmission();
		BackupRestoreStatus status = synapse.startBackupDaemon(submission, MigratableObjectType.ENTITY);
		assertNotNull(status);
		assertNotNull(status.getStatus());
		assertFalse(DaemonStatus.FAILED == status.getStatus());
		assertEquals(DaemonType.BACKUP, status.getType());
		String backupId = status.getId();
		assertNotNull(backupId);
		String backupUri = "/daemon/" + backupId;
		long start = System.currentTimeMillis();
		// Wait for the backup to finish.
		while (true) {
			long now = System.currentTimeMillis();
			assertTrue("Timed out waiting for a backup to complete", now
					- start < TEST_TIME_OUT);
			status = synapse.getDaemonStatus(backupId);
			assertNotNull(status);
			assertNotNull(status.getStatus());
			// We are done if it failed.
			assertFalse(DaemonStatus.FAILED == status.getStatus());
			// Are we done?
			if (DaemonStatus.COMPLETED == status.getStatus()) {
				System.out.println("Backup Complete. Message: "
						+ status.getProgresssMessage());
				System.out.println("Backup File: "
						+ status.getBackupUrl());
				break;
			} else {
				long current = status.getProgresssCurrent();
				long total = status.getProgresssTotal();
				if (total <= 0) {
					total = 1000000;
				}
				String message = status.getProgresssMessage();
				double percent = ((double) current / (double) total) * 100.0;
				System.out.println("Backup progresss: " + percent
						+ " % Message: " + message);
			}
			// Wait.
			Thread.sleep(1000);
		}
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
		
		// now restore the Entities
		
		// Step one is to upload the file to s3.
		URL fileUrl = IT100BackupRestoration.class.getClassLoader()
				.getResource(BACKUP_FILE_NAME);
		File backupFile = new File(fileUrl.getFile().replaceAll("%20", " "));
		// Make sure the file exists
		assertTrue(backupFile.getAbsolutePath()+" does not exist.", backupFile.exists());
		// Now upload the file to s3
		PutObjectResult putResults = s3Client.putObject(bucket,	BACKUP_FILE_NAME, backupFile);
		System.out.println(putResults);

		// Start the daemon
		RestoreSubmission submission = new RestoreSubmission();
		submission.setFileName(BACKUP_FILE_NAME);
		BackupRestoreStatus status = synapse.startRestoreDaemon(submission, MigratableObjectType.ENTITY);

		assertNotNull(status);
		assertNotNull(status.getStatus());
		assertFalse(status.getErrorMessage(),DaemonStatus.FAILED == status.getStatus());
		assertTrue(DaemonType.RESTORE == status.getType());
		String restoreId = status.getId();
		assertNotNull(restoreId);
		
		// Wait for it to finish
		waitForDaemon(status.getId());
		
		// Login as the test user one.
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		// Now make sure we can find one of the datasetst
		JSONObject datasetQueryResults = synapse
				.query("select * from dataset where name == \"MSKCC Prostate Cancer\"");
		assertEquals(1, datasetQueryResults.getJSONArray("results").length());
		System.out.println("Found the 'MSKCC Prostate Cancer' using devUser1@sagebase.org");
	}
	
	/**
	 * Test the complete round trip of user migration.
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testUserProfileRoundTrip() throws Exception {
		// first create a backup copy of all of the users
		// Start the daemon
		BackupSubmission submission = new BackupSubmission();
		submission.setEntityIdsToBackup(synapse.getAllUserAndGroupIds());
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
		// Now restore the users from this backup file
		RestoreSubmission restoreSub = new RestoreSubmission();
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		restoreSub.setFileName(backupFileName);
		status = synapse.startRestoreDaemon(restoreSub, MigratableObjectType.PRINCIPAL);
		// Wait for it to finish
		status = waitForDaemon(status.getId());
		assertEquals(DaemonStatus.COMPLETED, status.getStatus());
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
	public void testSearchDocumentRoundTrip() throws Exception{
		String projectDescription = "Integration Test - Search Document Round Trip";
		// Create a project
		Project project = new Project();
		project.setDescription(projectDescription);
		project = synapse.createEntity(project);
		assertNotNull(project);
		toDelete.add(project);

		// Now make a search document of this entity
		BackupSubmission submission = new BackupSubmission();
		Set<String> set = new HashSet<String>();
		set.add(project.getId());
		submission.setEntityIdsToBackup(set);
		
		BackupRestoreStatus status = synapse.startSearchDocumentDaemon(submission);
		assertNotNull(status);
		// Wait for the daemon to complete
		status = waitForDaemon(status.getId());
		assertNotNull(status.getBackupUrl());
		assertTrue(status.getBackupUrl().startsWith(S3_WORKFLOW_URL_PREFIX));
		// extract the s3Key
		String searchDocumentS3Key = status.getBackupUrl().substring(S3_WORKFLOW_URL_PREFIX.length());
		
		S3Object s3Object = s3Client.getObject(S3_WORKFLOW_BUCKET, searchDocumentS3Key);
		String serializedSearchDocuments = IOUtils.toString(s3Object.getObjectContent(), "UTF-8");
		JSONArray searchDocuments = new JSONArray(serializedSearchDocuments);
		assertEquals(1, searchDocuments.length());
		JSONObject searchDocument = searchDocuments.getJSONObject(0);
		Document document = EntityFactory.createEntityFromJSONObject(searchDocument, Document.class);
		assertEquals(projectDescription, document.getFields().getDescription());
		assertEquals("dev admin", document.getFields().getCreated_by());
		assertEquals("dev admin", document.getFields().getModified_by());
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
}
