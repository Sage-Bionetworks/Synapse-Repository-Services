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
	 * @param retryDenominator - If a DaemonFailedException is thrown by the worker, then the batch will be divide the batch into sub-batches
	 * using this number as the denominator. An attempt will then be made to retry each sub-batch. If this is set to less than 2,
	 * then no re-try will be attempted.
	 * @param worker - The worker that does the actual work.
	 * @param ids - The original batch of IDs.
	 */
	public static void attemptBatchWithRetry(int retryDenominator, BatchWorker worker, List<Long> ids) throws Exception{
		// if there is a failure attempt to migrate a sub-set of the list
		try{
			worker.attemptBatch(ids);
		}catch(DaemonFailedException e){
			if(retryDenominator < 2){
				// There is nothing else to do if the retry is less then two.
				throw new DaemonFailedException("Daemon failed and the retry-denominator is less than two so no attempt will be made retry with sub-batches", e);
			}
			if(ids.size() < 2){
				// We cannot further divide the batch
				throw e;
			}
			// Break up the batch and attempt smaller chunks
			int subBatchSize = Math.max(1, ids.size()/retryDenominator);
			log.warn("Daemon job failed: "+e.getMessage()+".  Will divide the batch into sub-batches of size: "+subBatchSize+" and re-try", e);
			List<Long> subBatch = new LinkedList<Long>();
			DaemonFailedException lastException = null;
			for(Long id: ids){
				subBatch.add(id);
				if(subBatch.size() >= subBatchSize){
					try{
						worker.attemptBatch(subBatch);
					}catch(DaemonFailedException e2){
						lastException = e2;
					}
					subBatch.clear();
				}
			}
			// If there is any data left then send it.
			if(subBatch.size() > 0){
				try{
					worker.attemptBatch(subBatch);
				}catch(DaemonFailedException e2){
					lastException = e2;
				}
			}
			// Throw the last exception found
			if(lastException != null){
				throw lastException;
			}
		}
	}

}
