package org.sagebionetworks.table.worker;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableTransactionManager;
import org.sagebionetworks.repo.manager.table.TableTransactionManagerProvider;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Umbrella worker for all table update transactions.
 * 
 * @author John
 *
 */
public class TableTransactionWorker implements MessageDrivenRunner {

	public static final String WAITING_FOR_TABLE_LOCK = "Waiting for table lock";

	static private Logger log = LogManager.getLogger(TableTransactionWorker.class);
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	TableTransactionManagerProvider tableTransactionManagerProvider;
	
	@Autowired
	TableExceptionTranslator tableExceptionTranslator;


	@Override
	public void run(final ProgressCallback progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try{
			TableUpdateTransactionRequest request = AsynchJobUtils.extractRequestBody(status, TableUpdateTransactionRequest.class);
			ValidateArgument.required(request, "TableUpdateTransactionRequest");
			ValidateArgument.required(request.getEntityId(), "TableUpdateTransactionRequest.entityId");
			// Lookup the user that started the job
			UserInfo userInfo = userManager.getUserInfo(status.getStartedByUserId());
			// Lookup the type of the table
			IdAndVersion idAndVersion = IdAndVersion.parse(request.getEntityId());	
			EntityType tableType = tableManagerSupport.getTableEntityType(idAndVersion);
			// Lookup the manger for this type
			TableTransactionManager transactionManager = tableTransactionManagerProvider.getTransactionManagerForType(tableType);
			// Listen to progress events.
			ProgressListener listener = new ProgressListener(){
				
				AtomicLong counter = new AtomicLong();

				@Override
				public void progressMade() {
					long count = counter.getAndIncrement();
					// update the job status.
					asynchJobStatusManager.updateJobProgress(status.getJobId(), count, Long.MAX_VALUE, "Update: "+count);
				}
				
			};
			progressCallback.addProgressListener(listener);
			try{
				// The manager does the rest of the work.
				TableUpdateTransactionResponse responseBody = transactionManager.updateTableWithTransaction(progressCallback, userInfo, request);
				// Set the job complete.
				asynchJobStatusManager.setComplete(status.getJobId(), responseBody);
				log.info("JobId: "+status.getJobId()+" complete");
			}finally{
				// unconditionally remove the listener.
				progressCallback.removeProgressListener(listener);
			}

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
			// Attempt to translate the exception to make it 'user-friendly'
			RuntimeException translated = tableExceptionTranslator.translateException(e);
			// job failed.
			asynchJobStatusManager.setJobFailed(status.getJobId(), translated);
		}
	}

}
