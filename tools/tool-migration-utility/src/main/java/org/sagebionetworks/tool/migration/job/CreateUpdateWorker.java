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
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.ClientFactory;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * A worker that will execute a single backup/restore job.
 * 
 * @author John
 * 
 */
public class CreateUpdateWorker implements Callable<WorkerResult> {
	
	// Restore takes takes up 75% of the time
	private static final float RESTORE_FRACTION = 75.0f/100.0f;
	// Backup takes 25% of the time.
	private static final float BAKUP_FRACTION = 1.0f-RESTORE_FRACTION;
	
	static private Log log = LogFactory.getLog(CreateUpdateWorker.class);
	
	private static long WOKER_TIMEOUT = Configuration.getWorkerTimeoutMs();

	ClientFactory clientFactory = null;
	Set<String> entites = null;
	BasicProgress progress = null;

	public CreateUpdateWorker(ClientFactory clientFactory, Set<String> entites, BasicProgress progress) {
		super();
		this.clientFactory = clientFactory;
		this.entites = entites;
		this.progress = progress;
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
			// Update the status
			updateProgress(status, this.progress);
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
			Thread.sleep(2000);
		}
	}


	/**
	 * Update the progress
	 * @param status
	 */
	public static void updateProgress(BackupRestoreStatus status, BasicProgress progress) {
		// Calculate the progress
		float current = status.getProgresssCurrent();
		float total = status.getProgresssTotal();
		// we can only update the status if there are status numbers
		if(current < 1.0f ||  total < 1.0){
			return;
		}
		float fraction = current/total;
		float progressCurrent = progress.getTotal()*fraction;
		long adjustedProgress = 0;
		if(DaemonType.RESTORE == status.getType()){
			// Restores happen after backup is at 100%
			long backupProgress = (long) (progress.getTotal()*BAKUP_FRACTION);
			// the restores are slower, so progress made is more significant.
			// Add the completed backup progress
			adjustedProgress = (long)(progressCurrent*RESTORE_FRACTION)+backupProgress;
		}else{
			// the backups are faster, so progress made is not as significant.
			adjustedProgress = (long)(progressCurrent*BAKUP_FRACTION);
		}
		progress.setCurrent(adjustedProgress);
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
			// set the progress to done.
			progress.setDone();
			return new WorkerResult(this.entites.size(), WorkerResult.JobStatus.SUCCEDED);
		} catch (Exception e) {
			// set the progress to done.
			progress.setDone();
			// Log any errors
			log.error("CreateUpdateWorker Failed to run job: "+ entites.toString(), e);
			return new WorkerResult(0, WorkerResult.JobStatus.FAILED);
		}
	}
	
	/**
	 * A test for printing progress
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException{
		
		BasicProgress progress = new BasicProgress();
		progress.setCurrent(0);
		progress.setTotal(100);
		
		// Simulate backup
		BackupRestoreStatus status = new BackupRestoreStatus();
		status.setType(DaemonType.BACKUP);
		status.setProgresssTotal(new Long(25));
		for(int i=0; i<25; i++){
			status.setProgresssCurrent(new Long(i));
			// Update the progress
			updateProgress(status, progress);
			System.out.println(progress.getCurrentStatus());
			Thread.sleep(100);
		}
		// Simulate restore
		status = new BackupRestoreStatus();
		status.setType(DaemonType.RESTORE);
		status.setProgresssTotal(new Long(75));
		for(int i=0; i<75; i++){
			status.setProgresssCurrent(new Long(i));
			// Update the progress
			updateProgress(status, progress);
			System.out.println(progress.getCurrentStatus());
			Thread.sleep(100);
		}
	}

}
