package org.sagebionetworks.table.worker;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableUpdateRequestManager;
import org.sagebionetworks.repo.manager.table.TableUpdateRequestManagerProvider;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Umbrella worker for all table update transactions.
 * 
 * @author John
 *
 */
@Service
public class TableUpdateRequestWorker implements AsyncJobRunner<TableUpdateTransactionRequest, TableUpdateTransactionResponse> {

	public static final String WAITING_FOR_TABLE_LOCK = "Waiting for table lock";

	static private Logger log = LogManager.getLogger(TableUpdateRequestWorker.class);
	
	@Autowired
	private TableManagerSupport tableManagerSupport;
	
	@Autowired
	private TableUpdateRequestManagerProvider tableUpdateRequestManagerProvider;
	
	@Autowired
	private TableExceptionTranslator tableExceptionTranslator;

	@Override
	public Class<TableUpdateTransactionRequest> getRequestType() {
		return TableUpdateTransactionRequest.class;
	}
	
	@Override
	public Class<TableUpdateTransactionResponse> getResponseType() {
		return TableUpdateTransactionResponse.class;
	}
	
	@Override
	public TableUpdateTransactionResponse run(String jobId, UserInfo user, TableUpdateTransactionRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws Exception {
		try {
			ValidateArgument.required(request, "TableUpdateTransactionRequest");
			ValidateArgument.required(request.getEntityId(), "TableUpdateTransactionRequest.entityId");
			// Lookup the type of the table
			IdAndVersion idAndVersion = IdAndVersion.parse(request.getEntityId());
			TableType tableType = tableManagerSupport.getTableType(idAndVersion);
			// Lookup the manger for this type
			TableUpdateRequestManager requestManager = tableUpdateRequestManagerProvider.getUpdateRequestManagerForType(tableType);
			// Listen to progress events.
			ProgressListener listener = new ProgressListener(){
				
				AtomicLong counter = new AtomicLong();

				@Override
				public void progressMade() {
					long count = counter.getAndIncrement();
					// update the job status.
					jobProgressCallback.updateProgress("Update: "+count, count, Long.MAX_VALUE);
				}
				
			};
			jobProgressCallback.addProgressListener(listener);
			try{
				// The manager does the rest of the work.
				TableUpdateTransactionResponse response = requestManager.updateTableWithTransaction(jobProgressCallback, user, request);
				log.info("JobId: "+jobId+" complete");
				return response;
			} finally{
				// unconditionally remove the listener.
				jobProgressCallback.removeProgressListener(listener);
			}

		} catch (TableUnavailableException | LockUnavilableException e ){
			log.info(WAITING_FOR_TABLE_LOCK + ":" + e.getMessage());
			// reset the job progress.
			jobProgressCallback.updateProgress(WAITING_FOR_TABLE_LOCK, 0L, 100L);
			throw new RecoverableMessageException(e);
		} catch (RecoverableMessageException e) {
			// reset the job progress.
			jobProgressCallback.updateProgress(e.getMessage(), 0L, 100L);
			throw e;
		} catch (Throwable e){
			log.error("Job failed:", e);
			// Attempt to translate the exception to make it 'user-friendly'
			RuntimeException translated = tableExceptionTranslator.translateException(e);
			// job failed.
			throw translated;
		}
	}
}
