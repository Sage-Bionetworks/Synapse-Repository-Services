package org.sagebionetworks.tool.migration.v3;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.ListBucketProvider;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationUtils;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

public class CreateUpdateWorker implements Callable<Long> {
	
	static private Log log = LogFactory.getLog(MetadataIterator.class);

	// Restore takes takes up 75% of the time
	private static final float RESTORE_FRACTION = 75.0f/100.0f;
	// Backup takes 25% of the time.
	private static final float BAKUP_FRACTION = 1.0f-RESTORE_FRACTION;
	
	MigrationType type;
	long count;
	Iterator<RowMetadata> iterator;
	BasicProgress progress;
	SynapseAdministrationInt destClient;
	SynapseAdministrationInt sourceClient;
	long batchSize;
	long timeout;
	
	/**
	 * Create/update worker.
	 * @param type
	 * @param count
	 * @param iterator
	 * @param progress
	 * @param destClient
	 * @param sourceClient
	 * @param batchSize
	 */
	public CreateUpdateWorker(MigrationType type, long count, Iterator<RowMetadata> iterator, BasicProgress progress,
			SynapseAdministrationInt destClient,
			SynapseAdministrationInt sourceClient, long batchSize, long timeout) {
		super();
		this.type = type;
		this.count = count;
		this.iterator = iterator;
		this.progress = progress;
		this.progress.setCurrent(0);
		this.progress.setTotal(count);
		this.destClient = destClient;
		this.sourceClient = sourceClient;
		this.batchSize = batchSize;
		this.timeout = timeout;
	}

	@Override
	public Long call() throws Exception {
		// First we need to calculate the required buckets.
		ListBucketProvider provider = new ListBucketProvider();
		// This utility will guarantee that all parents are in buckets that proceed their children
		// so as long as we create the buckets in order, all parents and their children can be created
		// without foreign key constraint violations.
		progress.setMessage("Bucketing by tree level...");
		MigrationUtils.bucketByTreeLevel(this.iterator, provider);
		List<List<Long>> listOfBuckets = provider.getListOfBuckets();
		// Send each bucket batched.
		long updateCount = 0;
		for(List<Long> bucket: listOfBuckets){
			updateCount += backupBucketAsBatch(bucket.iterator());
		}
		progress.setDone();
		return updateCount;
	}
	
	/**
	 * Backup a single bucket
	 * @param bucketIt
	 * @return
	 * @throws Exception
	 */
	private long backupBucketAsBatch(Iterator<Long> bucketIt) throws Exception{
		// Iterate and create batches.
		Long id = null;
		List<Long> batch = new LinkedList<Long>();
		long updateCount = 0;
		while(bucketIt.hasNext()){
			id = bucketIt.next();
			if(id != null){
				batch.add(id);
				if(batch.size() >= batchSize){
					IdList request = new IdList();
					request.setList(batch);
					backupBatch(request);
					updateCount += batch.size();
					batch.clear();
				}
			}
		}
		// If there is any data left in the batch send it
		if(batch.size() > 0){
			IdList request = new IdList();
			request.setList(batch);
			backupBatch(request);
			updateCount += batch.size();
			batch.clear();
		}
		return updateCount;
	}
	
	private void backupBatch(IdList request) throws Exception {
		int listSize = request.getList().size();
		progress.setMessage("Starting backup daemon for "+listSize+" objects");
		// Start a backup.
		BackupRestoreStatus status = this.sourceClient.startBackup(type, request);
		// Wait for the backup to complete
		status = waitForDaemon(status.getId(), this.sourceClient);
		// Now restore this to the destination
		String backupFileName = getFileNameFromUrl(status.getBackupUrl());
		RestoreSubmission restoreSub = new RestoreSubmission();
		restoreSub.setFileName(backupFileName);
		status = this.destClient.startRestore(type, restoreSub);
		// Wait for the backup to complete
		status = waitForDaemon(status.getId(), this.destClient);
		// Update the progress
		progress.setMessage("Finished restore for "+listSize+" objects");
		progress.setCurrent(progress.getCurrent()+listSize);
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
	public BackupRestoreStatus waitForDaemon(String daemonId, SynapseAdministrationInt client)
			throws SynapseException, JSONObjectAdapterException,
			InterruptedException {
		// Wait for the daemon to finish.
		long start = System.currentTimeMillis();
		while (true) {
			long now = System.currentTimeMillis();
			if(now-start > timeout){
				throw new InterruptedException("Timed out waiting for the daemon to complete");
			}
			BackupRestoreStatus status = client.getStatus(daemonId);
			float complete = getPercentComplet(status);
			progress.setMessage(String.format("Waiting for daemon: %1$s id: %2$s Progress: %3$6.2f %3$2%", status.getType().name(), status.getId(), (complete*100f)));
//			System.out.println(progress.getMessage());
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
	
	/**
	 * Update the progress
	 * @param status
	 */
	public static float getPercentComplet(BackupRestoreStatus status) {
		// Calculate the progress
		long current = status.getProgresssCurrent();
		long total = status.getProgresssTotal();
		if(total <= 0) return 0;
		if(current > total){
			return 1f;
		}
		float fraction = ((float)current)/((float)total);
		return fraction;
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


}
