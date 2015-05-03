package org.sagebionetworks.audit.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.util.ProgressCallback;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This worker merges access record files as they can be small.
 * 
 * @author John
 *
 */
public class MergeWorker {
		
	static private Log log = LogFactory.getLog(MergeWorker.class);
	private AccessRecordDAO accessRecordDAO;
	private ProgressCallback<Void> callback;

	/**
	 * All dependencies are provide at construction time.
	 * @param accessRecordDAO
	 * @param minFileSize
	 */
	public MergeWorker(AccessRecordDAO accessRecordDAO, ProgressCallback<Void> callback) {
		super();
		this.accessRecordDAO = accessRecordDAO;
		this.callback = callback;
	}

	/**
	 * Merge one batch of files.
	 * 
	 * @return True if there are more files to be merged, else False.
	 */
	public boolean mergeOneBatch() {
		try {
			// Walk all files for this stack looking for files that are under the minimum
			String marker = null;
			BatchData batchData = null;
			do{
				ObjectListing listing = accessRecordDAO.listBatchKeys(marker);
				marker = listing.getNextMarker();
				if(listing.getObjectSummaries() != null){
					List<String> rollingKeys = new ArrayList<String>();
					for (S3ObjectSummary summ: listing.getObjectSummaries()) {
						final String key = summ.getKey();
						if (key.contains(KeyGeneratorUtil.ROLLING)) {
							rollingKeys.add(key);
						}
					}
					// To avoid thrashing on the current data, we skip any file that
					// matches the current date and hour
					String currentDateHour = KeyGeneratorUtil.getDateAndHourFromTimeMS(System.currentTimeMillis());
					for(String key: rollingKeys){
						// Bucket by date/hour
						String fileDateHour = KeyGeneratorUtil.getDateAndHourFromKey(key);
						// skip files that are in the current date an hour
						if(currentDateHour.equals(fileDateHour)){
							// Skip this file for now. It will be processed in an hour.
							continue;
						}
						// Do we have a complete batch?
						if (batchData != null) {
							// The current batch is completed if the next file
							// does not have the same date/hout as the current
							// batch.
							if (!batchData.batchDateString.equals(fileDateHour)) {
								// This method will save the current batch and
								// delete all of the original files.
								if (mergeBatch(batchData)) {
									// The batch was merged to completion and we
									// are done for this round
									return true;
								} else {
									// The batch was not merged. This occurs if
									// where there was nothing in the batch to
									// merged. For this case we want to start
									// with a new batch.
									batchData = null;
								}
							}
						}
							
						// If the batchData is null then this is the start
						// of a new batch.
						if (batchData == null) {
							batchData = new BatchData(new LinkedList<String>(),	fileDateHour);
						}
						// This is a file we would like to merge
						batchData.mergedKeys.add(key);
					}
				}
			}while(marker != null);
			// If there is any batch data left then complete it.
			if(batchData != null){
				mergeBatch(batchData);
			}
			// If we made it this far then there is no more data.
			return false;
		} catch (Exception e) {
			log.error("Worker failed", e);
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * This will save the new batch file and delete all of the sub files that were merged.
	 * @param data
	 * @return True if the batch was merged.  False if there was nothing to merge or the batch was empty.
	 */
	private boolean mergeBatch(BatchData data) {
		if(data != null){
			if(data.mergedKeys.size() > 0){
				try {
					// Load the data from each file to merge
					List<AccessRecord> mergedBatches = new LinkedList<AccessRecord>();
					for(String key: data.mergedKeys){
						List<AccessRecord> subBatch;
						try {
							subBatch = accessRecordDAO.getBatch(key);
							mergedBatches.addAll(subBatch);
							if (this.callback != null) {
								this.callback.progressMade(null);
							}
						} catch (Exception e) {
							// We need to continue even if one file is bad.
							log.error("Failed to read: "+key, e);
						}

					}
					
					// Use the time stamp from the first record as the time stamp for the new batch.
					long timestamp = mergedBatches.get(0).getTimestamp();
					// Save the merged batches
					String newfileKey = accessRecordDAO.saveBatch(mergedBatches, timestamp, false);
					
					// Now delete all of the files that were merged.
					for(String key: data.mergedKeys){
						accessRecordDAO.deleteBactch(key);
					}
					long elapse = System.currentTimeMillis()-data.startMs;
					long msPerfile = elapse/data.mergedKeys.size();
					log.info("Merged: "+data.mergedKeys.size()+" files into new file: "+newfileKey+" in "+elapse+" ms rate of: "+msPerfile+" ms/file");
					
					// Extend semaphore timeout after writing collated log file
					if (this.callback != null) {
						this.callback.progressMade(null);
					}
					
					// We merged this batch successfully
					return true;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return false;
	}
	
	/**
	 * All of the data about a batch.
	 * @author John
	 *
	 */
	private static class BatchData {
		List<String> mergedKeys = null;
		String batchDateString;
		long startMs;
		public BatchData(List<String> mergedKeys, String batchDateString) {
			super();
			this.mergedKeys = mergedKeys;
			this.batchDateString = batchDateString;
			this.startMs = System.currentTimeMillis();
		}
	}

}
