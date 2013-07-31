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

public class CreateUpdateWorker implements Callable<Long>, BatchWorker {
	
	static private Log log = LogFactory.getLog(CreateUpdateWorker.class);

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
	long timeoutMS;
	int retryDenominator;
	
	/**
	 * 
	 * @param type - The type to be migrated.
	 * @param count - The number of items in the iterator to be migrated.
	 * @param iterator - Abstraction for iterating over the objects to be migrated.
	 * @param progress - The worker will update the progress objects so its progress can be monitored externally.
	 * @param destClient - A handle to the destination SynapseAdministration client. Data will be pushed to the destination.
	 * @param sourceClient - A handle to the source SynapseAdministration client. Data will be pulled from the source.
	 * @param batchSize - Data is migrated in batches.  This controls the size of the batches.
	 * @param timeout - How long should the worker wait for Daemon job to finish its task before timing out in milliseconds.
	 * @param retryDenominator - If a daemon fails to backup or restore a single batch, the worker will divide the batch into sub-batches
	 * using this number as the denominator. An attempt will then be made to retry the migration of each sub-batch in an attempt to isolate the problem.
	 * If this is set to less than 2, then no re-try will be attempted.
	 */
	public CreateUpdateWorker(MigrationType type, long count, Iterator<RowMetadata> iterator, BasicProgress progress,
			SynapseAdministrationInt destClient,
			SynapseAdministrationInt sourceClient, long batchSize, long timeoutMS, int retryDenominator) {
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
		this.timeoutMS = timeoutMS;
		this.retryDenominator = retryDenominator;
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
					migrateBatch(batch);
					updateCount += batch.size();
					batch.clear();
				}
			}
		}
		// If there is any data left in the batch send it
		if(batch.size() > 0){
			migrateBatch(batch);
			updateCount += batch.size();
			batch.clear();
		}
		return updateCount;
	}
	
	/**
	 * Migrate the batches
	 * @param ids
	 * @throws Exception
	 */
	protected void migrateBatch(List<Long> ids) throws Exception {
		// This utility will first attempt to execute the batch.
		// If there are failures it will break the batch into sub-batches and attempt to execute eatch sub-batch.
		BatchUtility.attemptBatchWithRetry(this, ids);
	}

	/**
	 * Attempt to migrate a single batch.
	 * @param ids
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 * @throws InterruptedException
	 */
	public Long attemptBatch(List<Long> ids) throws JSONObjectAdapterException, SynapseException, InterruptedException {
		int listSize = ids.size();
		progress.setMessage("Starting backup daemon for "+listSize+" objects");
		// Start a backup.
		IdList request = new IdList();
		request.setList(ids);
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
		return (long) (listSize);
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
			if(now-start > timeoutMS){
				log.debug("Timeout waiting for daemon to complete");
				throw new InterruptedException("Timed out waiting for the daemon to complete");
			}
			BackupRestoreStatus status = client.getStatus(daemonId);
			progress.setMessage(String.format("\t Waiting for daemon: %1$s id: %2$s", status.getType().name(), status.getId()));
			// Check to see if we failed.
			if(DaemonStatus.FAILED == status.getStatus()){
				log.debug("Daemon failure");
				throw new DaemonFailedException("Failed: "+status.getType()+" message:"+status.getErrorMessage());
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
	 * Extract the filename from the full url.
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl){;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index+1, fullUrl.length());
	}


}
