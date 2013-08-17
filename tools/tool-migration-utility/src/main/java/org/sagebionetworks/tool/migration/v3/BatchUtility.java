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
	public static Long attemptBatchWithRetry(BatchWorker worker, List<Long> ids) throws Exception{
		Long c = 0L;
		// if there is a failure attempt to migrate a sub-set of the list
		try{
			c = worker.attemptBatch(ids);
		}catch(DaemonFailedException e){

			if(ids.size() < 2){
				// We cannot further divide the batch
				String msg = "Failed ids in batch retry:\t" + ids;
				log.warn(msg);
				throw new DaemonFailedException(msg, e);
			}
			// Break up the batch and attempt one at a time
			int subBatchSize = Math.max(1, (ids.size()+1)/2);
			log.warn("Daemon job failed.  Will divide the batch into two sub-batches of size: "+subBatchSize+" and re-try", e);
			
			List<Long> subBatch1 = new LinkedList<Long>();
			List<Long> subBatch2 = new LinkedList<Long>();
			
			for(Long id: ids){
				if (subBatch1.size() < subBatchSize) {
					subBatch1.add(id);
				} else {
					subBatch2.add(id);
				}
			}

			// Catch exception on 1st sub-batch
			Exception firstBatchException = null;
			try {
				attemptBatchWithRetry(worker, subBatch1);
			} catch (DaemonFailedException e2) {
				firstBatchException = e2;
			}
			
			// Try second batch regardless
			attemptBatchWithRetry(worker, subBatch2);
			
			if (firstBatchException != null) {
				throw firstBatchException;
			}

		}
		return c;
	}
}
