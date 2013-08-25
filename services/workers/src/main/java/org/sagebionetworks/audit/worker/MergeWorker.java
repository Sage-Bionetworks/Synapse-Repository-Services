package org.sagebionetworks.audit.worker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.audit.AccessRecord;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This worker merges access record files as they can be small.
 * 
 * @author John
 *
 */
public class MergeWorker implements Runnable {
	
	static private Log log = LogFactory.getLog(MergeWorker.class);
	private AccessRecordDAO accessRecordDAO;
	long minFileSizeBytes;

	/**
	 * All dependencies are provide at construction time.
	 * @param accessRecordDAO
	 * @param minFileSize
	 */
	public MergeWorker(AccessRecordDAO accessRecordDAO, long minFileSizeBytes) {
		super();
		this.accessRecordDAO = accessRecordDAO;
		this.minFileSizeBytes = minFileSizeBytes;
	}

	@Override
	public void run() {
		
		try {
			// Walk all files for this stack looking for files that are under the minimum
			String marker = null;
			BatchData batchData = null;
			do{
				ObjectListing listing = accessRecordDAO.listBatchKeys(marker);
				marker = listing.getNextMarker();
				if(listing.getObjectSummaries() != null){
					for(S3ObjectSummary summ: listing.getObjectSummaries()){
						// We only merge files that are under the minimum size.
						if(summ.getSize() < minFileSizeBytes){
							// We found a file that needs to be merged. Read in the data from the file.
							// We do not want to merge files from different days, so when we
							// encounter a new date we complete the current batch and start a new one.
							String fileDate = KeyGeneratorUtil.getDateStringFromKey(summ.getKey());
							if(batchData != null && !batchData.batchDateString.equals(fileDate)){
								// We are done with the current batch since the next does not have the same date.
								completeBatch(batchData);
								// Clear the batch data.
								batchData = null;
							}
							// Complete the batch if the size is exceeded
							if(batchData != null && batchData.batchSizeBytes > minFileSizeBytes){
								// we now have a file that is large enough.
								completeBatch(batchData);
								// Clear the batch data.
								batchData = null;
							}
							
							// If the batchData is null then this is the start
							// of a new batch.
							if (batchData == null) {
								batchData = new BatchData(
										new LinkedList<AccessRecord>(),
										new LinkedList<String>(), fileDate);
							}
							// Download the batch data for this key.
							List<AccessRecord> subBatch = accessRecordDAO.getBatch(summ.getKey());
							// Add this sub-batch to the current batch.
							batchData.batch.addAll(subBatch);
							// We also will need to delete this sub-file after
							// we are done saving the larger batch.
							batchData.mergedKeys.add(summ.getKey());
							batchData.batchSizeBytes += summ.getSize();
						}
					}
				}
			}while(marker != null);
			// If there is any batch data left then complete it.
			if(batchData != null){
				completeBatch(batchData);
			}
		} catch (Exception e) {
			log.error("Worker failed", e);
		}
	}
	
	/**
	 * This will save the new batch file and delete all of the sub files that were merged.
	 * @param data
	 */
	private void completeBatch(BatchData data) {
		if(data != null){
			if(data.batch.size() > 0){
				// Create a new key using the timestamp of the first row in this batch.
				long timestamp = data.batch.get(0).getTimestamp();
				try {
					// Save the batch use the timestamp from the first row for the key.
					String newfileKey = accessRecordDAO.saveBatch(data.batch, timestamp);
					
					// Now delete all of the files that were merged.
					for(String key: data.mergedKeys){
						accessRecordDAO.deleteBactch(key);
					}
					long elapse = System.currentTimeMillis()-data.startMs;
					long msPerfile = elapse/data.batch.size();
					log.info("Merged: "+data.mergedKeys.size()+" files into new file: "+newfileKey+" in "+elapse+" ms rate of: "+msPerfile+" ms/file");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}		
	}
	
	/**
	 * All of the data about a batch.
	 * @author John
	 *
	 */
	private static class BatchData {
		List<AccessRecord> batch = null;
		List<String> mergedKeys = null;
		String batchDateString;
		long batchSizeBytes;
		long startMs;
		public BatchData(List<AccessRecord> batch, List<String> mergedKeys, String batchDateString) {
			super();
			this.batch = batch;
			this.mergedKeys = mergedKeys;
			this.batchDateString = batchDateString;
			this.startMs = System.currentTimeMillis();
		}
	}

}
