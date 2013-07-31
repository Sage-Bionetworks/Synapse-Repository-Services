package org.sagebionetworks.tool.migration.v3;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility for re-trying failed batches with sub-batches.
 * 
 * @author John
 *
 */
public class BatchUtility {
	
	static private Log log = LogFactory.getLog(BatchUtility.class);
	
	/**
	 * Attempt to execute a batch of IDs.  If DaemonFailedException occur, then re-try with a sub-batch.
	 * 
	 * @param worker - The worker that does the actual work.
	 * @param ids - The original batch of IDs.
	 */
	public static void attemptBatchWithRetry(BatchWorker worker, List<Long> ids) throws Exception{
		// if there is a failure attempt to migrate a sub-set of the list
		try{
			worker.attemptBatch(ids);
		}catch(DaemonFailedException e){

			if(ids.size() < 2){
				// We cannot further divide the batch
				throw e;
			}
			// Break up the batch and attempt one at a time
			int subBatchSize = 1;
			log.warn("Daemon job failed: "+e.getMessage()+".  Will divide the batch into sub-batches of size: "+subBatchSize+" and re-try", e);
			List<Long> subBatch = new LinkedList<Long>();
			List<Long> failedIds = new LinkedList<Long>();
			DaemonFailedException lastException = null;
			for(Long id: ids){
				subBatch.add(id);
				try{
					worker.attemptBatch(subBatch);
				}catch(DaemonFailedException e2){
					failedIds.add(id);
					lastException = e2;
				}
				subBatch.clear();
			}
			// Throw the last exception found
			if(lastException != null){
				log.warn("Failed ids in batch retry:\t" + failedIds);
				throw lastException;
			}
		}
	}

}
