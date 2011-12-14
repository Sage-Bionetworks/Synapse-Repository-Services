package org.sagebionetworks.tool.migration.job;

import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Configuration;

/**
 * A worker that will execute a single backup/restore job.
 * 
 * @author John
 * 
 */
public class CreateUpdateWorker implements Callable<WorkerResult> {

	static private Log log = LogFactory.getLog(CreateUpdateWorker.class);
	
	private static long WOKER_TIMEOUT = Configuration.getWorkerTimeoutMs();

	ClientFactory clientFactory = null;
	Set<String> entites = null;

	public CreateUpdateWorker(ClientFactory clientFactory, Set<String> entites) {
		super();
		this.clientFactory = clientFactory;
		this.entites = entites;
	}


	/**
	 * Wait for a daemon to finish.
	 * @param daemonId
	 * @param client
	 * @return
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 * @throws InterruptedException
	 */
	public BackupRestoreStatus waitForDaemon(String daemonId, Synapse client)
			throws SynapseException, JSONObjectAdapterException,
			InterruptedException {
		// Wait for the daemon to finish.
		long start = System.currentTimeMillis();
		while (true) {
			long now = System.currentTimeMillis();
			if(now-start > WOKER_TIMEOUT){
				throw new InterruptedException("Timed out waiting for the daemon to complete");
			}
			BackupRestoreStatus status = client.getDaemonStatus(daemonId);
			// Check to see if we failed.
			if(DaemonStatus.FAILED == status.getStatus()){
				throw new InterruptedException("Failed: "+status.getType()+" message:"+status.getErrorMessage());
			}
			// Are we done?
			if (DaemonStatus.COMPLETED == status.getStatus()) {
				logStatus(status);
				return status;
			} else {
				logStatus(status);
			}
			// Wait.
			Thread.sleep(1000);
		}
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
	 * Log the status if trace is enabled.
	 * @param status
	 */
	public void logStatus(BackupRestoreStatus status) {
		if (log.isTraceEnabled()) {
			String format = "Worker: %1$-10d : %2$s";
			String statString = DaemonStatusUtil.printStatus(status);
			log.trace(String.format(format, Thread.currentThread().getId(),	statString));
		}
	}

	@Override
	public WorkerResult call() throws Exception {
		try {
			// First get a connection to the source
			Synapse client = clientFactory.createNewSourceClient();
			BackupSubmission sumbission = new BackupSubmission();
			sumbission.setEntityIdsToBackup(this.entites);
			// Start a backup.
			BackupRestoreStatus status = client.startBackupDaemon(sumbission);
			// Wait for the backup to complete
			status = waitForDaemon(status.getId(), client);
			// Now restore this to the destination
			client = clientFactory.createNewDestinationClient();
			String backupFileName = getFileNameFromUrl(status.getBackupUrl());
			RestoreSubmission restoreSub = new RestoreSubmission();
			restoreSub.setFileName(backupFileName);
			status = client.startRestoreDaemon(restoreSub);
			// Wait for the backup to complete
			status = waitForDaemon(status.getId(), client);
			// Success
			return new WorkerResult(this.entites.size(), WorkerResult.JobStatus.SUCCEDED);
		} catch (Exception e) {
			// Log any errors
			log.error("CreateUpdateWorker Failed to run job: "+ entites.toString(), e);
			return new WorkerResult(0, WorkerResult.JobStatus.FAILED);
		}
	}

}
