package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.BackupRestoreStatus;

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

	public static final String BACKUP_FILE_NAME = "BackupDaemonJob512-958031189387028378.zip";

	private static Synapse synapse;
	private static AmazonS3Client s3Client;
	private static String bucket;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Use the synapse client to do some of the work for us.
		synapse = new Synapse();
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

	// This was used to create the backup used for the restoration step.
	@Ignore
	@Test
	public void createSnapshot() throws Exception {
		// Start the daemon
		JSONObject status = synapse.createEntity("admin/daemon/backup",
				new JSONObject());
		assertNotNull(status);
		assertNotNull(status.getString("status"));
		assertFalse(
				status.getString("errorMessage"),
				BackupRestoreStatus.STATUS.FAILED.name().equals(
						status.getString("status")));
		assertTrue(BackupRestoreStatus.TYPE.BACKUP.name().equals(
				status.getString("type")));
		String backupId = status.getString("id");
		assertNotNull(backupId);
		String backupUri = "/daemon/" + backupId;
		long start = System.currentTimeMillis();
		// Wait for the backup to finish.
		while (true) {
			long now = System.currentTimeMillis();
			assertTrue("Timed out waiting for a backup to complete", now
					- start < TEST_TIME_OUT);
			status = synapse.getEntity(backupUri);
			assertNotNull(status);
			String statusString = status.getString("status");
			assertNotNull(statusString);
			// We are done if it failed.
			assertFalse(
					status.getString("errorMessage"),
					BackupRestoreStatus.STATUS.FAILED.name().equals(
							status.getString("status")));
			// Are we done?
			if (BackupRestoreStatus.STATUS.COMPLETED.name()
					.equals(statusString)) {
				System.out.println("Backup Complete. Message: "
						+ status.getString("progresssMessage"));
				System.out.println("Backup File: "
						+ status.getString("backupUrl"));
				break;
			} else {
				long current = status.getLong("progresssCurrent");
				long total = status.getLong("progresssTotal");
				if (total <= 0) {
					total = 1000000;
				}
				String message = status.getString("progresssMessage");
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
		// Step one is to upload the file to s3.
		URL fileUrl = IT100BackupRestoration.class.getClassLoader()
				.getResource(BACKUP_FILE_NAME);
		File backupFile = new File(fileUrl.getFile().replaceAll("%20", " "));
		// Make sure the file exists
		assertTrue(backupFile.getAbsolutePath()+" does not exist.", backupFile.exists());
		// Now upload the file to s3
		PutObjectResult putResults = s3Client.putObject(bucket,
				BACKUP_FILE_NAME, backupFile);
		System.out.println(putResults);

		// Start the daemon
		JSONObject startUrl = new JSONObject();
		startUrl.putOpt("url", BACKUP_FILE_NAME);
		JSONObject status = synapse.createEntity("/admin/daemon/restore",
				startUrl);
		assertNotNull(status);
		assertNotNull(status.getString("status"));
		assertFalse(
				status.getString("errorMessage"),
				BackupRestoreStatus.STATUS.FAILED.name().equals(
						status.getString("status")));
		assertTrue(BackupRestoreStatus.TYPE.RESTORE.name().equals(
				status.getString("type")));
		String restoreId = status.getString("id");
		assertNotNull(restoreId);
		String backupUri = "/admin/daemon/" + restoreId;
		long start = System.currentTimeMillis();
		// Wait for the backup to finish.
		while (true) {
			long now = System.currentTimeMillis();
			assertTrue("Timed out waiting for the restore to complete", now
					- start < TEST_TIME_OUT);
			status = synapse.getEntity(backupUri);
			assertNotNull(status);
			String statusString = status.getString("status");
			assertNotNull(statusString);
			// We are done if it failed.
			assertFalse(
					status.getString("errorMessage"),
					BackupRestoreStatus.STATUS.FAILED.name().equals(
							status.getString("status")));
			// Are we done?
			if (BackupRestoreStatus.STATUS.COMPLETED.name()
					.equals(statusString)) {
				System.out.println("Restore Complete. Message: "
						+ status.getString("progresssMessage"));
				break;
			} else {
				long current = status.getLong("progresssCurrent");
				long total = status.getLong("progresssTotal");
				if (total <= 0) {
					total = 1000000;
				}
				String message = status.getString("progresssMessage");
				double percent = ((double) current / (double) total) * 100.0;
				System.out.println("Restore progresss: " + percent
						+ " % Message: " + message);
			}
			// Wait.
			Thread.sleep(1000);
		}
		
		// Login as the test user one.
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		// Now make sure we can find one of the datasetst
		JSONObject datasetQueryResults = synapse
				.query("select * from dataset where name == \"MSKCC Prostate Cancer\"");
		assertEquals(1, datasetQueryResults.getJSONArray("results").length());
		System.out.println("Found the 'MSKCC Prostate Cancer' using devUser1@sagebase.org");
	}

}
