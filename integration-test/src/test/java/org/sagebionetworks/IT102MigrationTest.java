package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;


public class IT102MigrationTest {
	
	private static SynapseAdministration conn;
	private static AmazonS3Client s3Client;
	private static String bucket;
	
	private List<Entity> toDelete;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Use the synapse client to do some of the work for us.
		conn = new SynapseAdministration();
		conn.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		conn.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		conn.login(StackConfiguration.getIntegrationTestUserAdminName(),
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
	
	@Before
	public void before() throws Exception {
		conn.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
		toDelete = new ArrayList<Entity>();
	}

	@After
	public void after() throws Exception {
		if(conn != null && toDelete != null){
			for(Entity e: toDelete){
				conn.deleteEntity(e);
			}
		}
	}

	private BackupRestoreStatus waitForDaemonCompletion(BackupRestoreStatus brStatus) throws InterruptedException, JSONObjectAdapterException, SynapseException {
		int loopCount = 1;
		while (brStatus.getStatus() != DaemonStatus.COMPLETED) {
			System.out.println("\t" + brStatus.getProgresssMessage());
			System.out.println("\tProgress:\t" + brStatus.getProgresssCurrent());
			Thread.sleep(1000L);
			brStatus = conn.getStatus(brStatus.getId());
			loopCount++;
			if (loopCount > 10) {
				throw new RuntimeException("Backup/Restore should have completed by now...");
			}
			if (brStatus.getStatus() == DaemonStatus.FAILED) {
				throw new RuntimeException("Backup failed...");
			}
		}
		return brStatus;
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
	
	@Test
	public void testRoundTrip() throws Exception {
		// Primary types
		System.out.println("Migration types");
		MigrationTypeList mtList = conn.getPrimaryTypes();
		List<MigrationType> migrationTypes = mtList.getList();
		Map<MigrationType, Long> countByMigrationType = new HashMap<MigrationType, Long>();
		for (MigrationType mt: migrationTypes) {
			System.out.println(mt.name());
			countByMigrationType.put(mt, 0L);
		}
		// Counts per type
		System.out.println("Counts by type");
		MigrationTypeCounts mtCounts = conn.getTypeCounts();
		List<MigrationTypeCount> mtcs = mtCounts.getList();
		for (MigrationTypeCount mtc: mtcs) {
			System.out.println(mtc.getType().name() + ":" + mtc.getCount());
			countByMigrationType.put(mtc.getType(), mtc.getCount());
		}
		// Round trip
		System.out.println("Backup/restore");
		Project p = new Project();
		p.setName("projectIT102");
		p = conn.createEntity(p);
		IdList idList = new IdList();
		List<String> ids = new ArrayList<String>();
		ids.add(p.getId());
		idList.setList(ids);
		System.out.println("Backing up...");
		BackupRestoreStatus brStatus = conn.startBackup(MigrationType.NODE, idList);
		brStatus = waitForDaemonCompletion(brStatus);
		conn.deleteEntity(p);
		System.out.println("Restoring " + brStatus.getBackupUrl() + "...");
		String fName = getFileNameFromUrl(brStatus.getBackupUrl());
		RestoreSubmission rReq = new RestoreSubmission();
		rReq.setFileName(fName);
		brStatus = conn.startRestore(MigrationType.NODE, rReq);
		brStatus = waitForDaemonCompletion(brStatus);
		Project rp = conn.getEntity(p.getId(), Project.class);
		assertNotNull(rp);
		assertEquals(p.getId(), rp.getId());
		assertEquals(p.getName(), rp.getName());
	}

}
