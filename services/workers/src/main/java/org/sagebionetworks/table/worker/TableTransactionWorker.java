package org.sagebionetworks.table.worker;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ThrottlingProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableTransactionManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;
import com.sun.star.lang.IllegalArgumentException;

/**
 * Umbrella worker for all table update transactions.
 * 
 * @author John
 *
 */
public class TableTransactionWorker implements MessageDrivenRunner {
	
	private static final int THROTTLING_FREQUENCY_MS = 2000;

	public static final String WAITING_FOR_TABLE_LOCK = "Waiting for table lock";

	static private Logger log = LogManager.getLogger(TableTransactionWorker.class);
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	
	@Autowired
	UserManager userManager;
	
	Map<EntityType, TableTransactionManager> managerMap;
	
	/**
	 * Injected.
	 * @param managerMap
	 */
	public void setManagerMap(Map<EntityType, TableTransactionManager> managerMap) {
		this.managerMap = managerMap;
	}


	@Override
	public void run(final ProgressCallback<Void> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try{
			TableUpdateTransactionRequest request = AsynchJobUtils.extractRequestBody(status, TableUpdateTransactionRequest.class);
			ValidateArgument.required(request, "TableUpdateTransactionRequest");
			ValidateArgument.required(request.getEntityId(), "TableUpdateTransactionRequest.entityId");
			// Lookup the user that started the job
			UserInfo userInfo = userManager.getUserInfo(status.getStartedByUserId());
			// Lookup the type of the table
			EntityType tableType = tableManagerSupport.getTableEntityType(request.getEntityId());
			// Lookup the manger for this type
			TableTransactionManager transactionManager = managerMap.get(tableType);
			if(transactionManager == null){
				throw new IllegalArgumentException("Cannot find a transaction manager for type: "+tableType.name());
			}
			// setup a callback to make progress
			ProgressCallback<Void> statusCallback = new ProgressCallback<Void>(){
				
				int count = 1;

				@Override
				public void progressMade(Void param) {
					// forward to the outside
					progressCallback.progressMade(null);
					// update the job status.
					asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Update: "+(count++));
				}
				
			};
			// Use a throttling callback to suppress too many updates.
			ThrottlingProgressCallback<Void> throttlingCallback = new ThrottlingProgressCallback<>(statusCallback, THROTTLING_FREQUENCY_MS);
			// The manager does the rest of the work.
			TableUpdateTransactionResponse responseBody = transactionManager.updateTableWithTransaction(throttlingCallback, userInfo, request);
			// Set the job complete.
			asynchJobStatusManager.setComplete(status.getJobId(), responseBody);
			log.info("JobId: "+status.getJobId()+" complete");
		}catch (TableUnavailableException e){
			log.info(WAITING_FOR_TABLE_LOCK);
			// reset the job progress.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, WAITING_FOR_TABLE_LOCK);
			throw new RecoverableMessageException(e);
		}catch (LockUnavilableException e){
			log.info("LockUnavilableException: "+e.getMessage());
			// reset the job progress.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, WAITING_FOR_TABLE_LOCK);
			throw new RecoverableMessageException(e);
		}catch (RecoverableMessageException e){
			log.info("RecoverableMessageException: "+e.getMessage());
			// reset the job progress.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, e.getMessage());
			throw e;
		}catch (Throwable e){
			log.error("Job failed:", e);
			// job failed.
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
	}

}
