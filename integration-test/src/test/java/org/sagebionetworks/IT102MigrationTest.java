package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.v5.AsyncMigrationWorker;
import org.sagebionetworks.tool.progress.BasicProgress;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.DefaultClock;


public class IT102MigrationTest {

	private static SynapseAdminClient adminSynapse;
	private Project project;
	private static final long ASYNC_MIGRATION_MAX_WAIT_MS = 20000;
	
	private List<Entity> toDelete;

	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
	}
	
	@Before
	public void before() throws Exception {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<Entity>();
		project = new Project();
		project.setName("projectIT102");
		project = adminSynapse.createEntity(project);
		toDelete.add(project);
	}

	@After
	public void after() throws Exception {
		if(adminSynapse != null && toDelete != null){
			for(Entity e: toDelete){
				adminSynapse.deleteAndPurgeEntity(e);
			}
		}
	}

	private BackupRestoreStatus waitForDaemonCompletion(BackupRestoreStatus brStatus) throws InterruptedException, JSONObjectAdapterException, SynapseException {
		int loopCount = 1;
		while (brStatus.getStatus() != DaemonStatus.COMPLETED) {
			System.out.println("\t" + brStatus.getProgresssMessage());
			System.out.println("\tProgress:\t" + brStatus.getProgresssCurrent());
			Thread.sleep(1000L);
			brStatus = adminSynapse.getStatus(brStatus.getId());
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
		MigrationTypeList mtList = adminSynapse.getPrimaryTypes();
		List<MigrationType> migrationTypes = mtList.getList();
		Map<MigrationType, Long> countByMigrationType = new HashMap<MigrationType, Long>();
		for (MigrationType mt: migrationTypes) {
			System.out.println(mt.name());
			countByMigrationType.put(mt, 0L);
		}
		// Counts per type
		System.out.println("Counts by type");
		MigrationTypeCounts mtCounts = adminSynapse.getTypeCounts();
		List<MigrationTypeCount> mtcs = mtCounts.getList();
		for (MigrationTypeCount mtc: mtcs) {
			System.out.println(mtc.getType().name() + ":" + mtc.getCount());
			countByMigrationType.put(mtc.getType(), mtc.getCount());
		}
		// Checksums per type
		System.out.println("Checksums by type");
		String salt = "SALT";
		for (MigrationType mt: migrationTypes) {
			MigrationRangeChecksum mtc = adminSynapse.getChecksumForIdRange(mt, salt, 0L, Long.MAX_VALUE);
			System.out.println(mt.name() + ":" + mtc);			
		}
		
		// Round trip
		System.out.println("Backup/restore");
		IdList idList = new IdList();
		List<Long> ids = new ArrayList<Long>();
		ids.add(Long.parseLong(project.getId().substring(3)));
		idList.setList(ids);
		System.out.println("Backing up...");
		BackupRestoreStatus brStatus = adminSynapse.startBackup(MigrationType.NODE, idList);
		brStatus = waitForDaemonCompletion(brStatus);
		adminSynapse.deleteAndPurgeEntity(project);
		System.out.println("Restoring " + brStatus.getBackupUrl() + "...");
		String fName = getFileNameFromUrl(brStatus.getBackupUrl());
		RestoreSubmission rReq = new RestoreSubmission();
		rReq.setFileName(fName);
		brStatus = adminSynapse.startRestore(MigrationType.NODE, rReq);
		brStatus = waitForDaemonCompletion(brStatus);
		Project rp = adminSynapse.getEntity(project.getId(), Project.class);
		assertNotNull(rp);
		assertEquals(project.getId(), rp.getId());
		assertEquals(project.getName(), rp.getName());
		
		// Fire change messages
		FireMessagesResult fmRes = adminSynapse.fireChangeMessages(0L, 10L);
		assertTrue(fmRes.getNextChangeNumber() > 0);
	}
	
	@Test
	public void testChecksumForIdRange() throws SynapseException, JSONObjectAdapterException {
		Long minId = Long.parseLong(project.getId().substring(3));
		Long maxId = Long.parseLong(project.getId().substring(3));
		String salt = "SALT";
		MigrationRangeChecksum checksum1 = adminSynapse.getChecksumForIdRange(MigrationType.NODE, salt, minId, maxId);
		assertNotNull(checksum1);
	}
	
	@Test
	public void testChecksumForIdRangeAsync() throws Exception {
		Long minId = Long.parseLong(project.getId().substring(3));
		Long maxId = Long.parseLong(project.getId().substring(3));
		String salt = "SALT";
		AsyncMigrationRangeChecksumRequest req = new AsyncMigrationRangeChecksumRequest();
		req.setMinId(minId);
		req.setMaxId(maxId);
		req.setSalt(salt);
		req.setType(MigrationType.NODE.name());
		AsyncMigrationRequest migReq = new AsyncMigrationRequest();
		migReq.setAdminRequest(req);
		
		// Checksum before
		AsynchronousJobStatus jobStatus = adminSynapse.startAdminAsynchronousJob(migReq);
		Clock clock = new DefaultClock();
		
		BasicProgress progress = new BasicProgress();
		AsyncMigrationWorker worker = new AsyncMigrationWorker(adminSynapse, req, ASYNC_MIGRATION_MAX_WAIT_MS, progress);
		MigrationRangeChecksum checksum1 = (MigrationRangeChecksum) worker.call();
		assertNotNull(checksum1);
	}

}
