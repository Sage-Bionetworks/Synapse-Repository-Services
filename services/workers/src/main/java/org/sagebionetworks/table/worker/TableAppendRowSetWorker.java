package org.sagebionetworks.table.worker;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.AppendableRowSet;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.progress.ThrottlingProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;
/**
 * This worker appends partial row sets to a table.
 * 
 * @author jmhill
 *
 */
public class TableAppendRowSetWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(TableAppendRowSetWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private TableRowManager tableRowManager;
	@Autowired
	private UserManager userManger;


	/**
	 * Process a single message
	 * @param message
	 * @return
	 * @throws Throwable 
	 */
	@Override
	public void run(ProgressCallback<Message> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		// First read the body
		try {
			processStatus(progressCallback, message);
		} catch (Throwable e) {
			log.error("Failed", e);
		}
	}

	/**
	 * @param status
	 * @throws Throwable 
	 */
	public void processStatus(final ProgressCallback<Message> progressCallback, final Message message) throws Throwable {
		AsynchronousJobStatus status = extractStatus(message);
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			AppendableRowSetRequest body = (AppendableRowSetRequest) status.getRequestBody();
			AppendableRowSet appendSet = body.getToAppend();
			if(appendSet == null){
				throw new IllegalArgumentException("ToAppend cannot be null");
			}
			if(appendSet.getTableId() == null){
				throw new IllegalArgumentException("Table ID cannot be null");
			}
			String tableId = appendSet.getTableId();
			long progressCurrent = 0L;
			long progressTotal = 100L;
			// Start the progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Starting...");
			org.sagebionetworks.util.ProgressCallback<Long> rowCallback = new org.sagebionetworks.util.ProgressCallback<Long>() {
				@Override
				public void progressMade(Long progress) {
					progressCallback.progressMade(message);
				}
			};
			// Do the work
			RowReferenceSet results = null;
			if(appendSet instanceof PartialRowSet){
				PartialRowSet partialRowSet = (PartialRowSet) appendSet;
				List<ColumnModel> columnModelsForTable = columnModelManager.getColumnModelsForTable(user, tableId);
				ColumnMapper columnMap = TableModelUtils.createColumnModelColumnMapper(columnModelsForTable, false);
				results =  tableRowManager.appendPartialRows(user, tableId, columnMap, partialRowSet, rowCallback);
			}else if(appendSet instanceof RowSet){
				RowSet rowSet = (RowSet)appendSet;
				ColumnMapper columnMap = columnModelManager.getCurrentColumns(user, tableId, rowSet.getHeaders());
				results = tableRowManager.appendRows(user, tableId, columnMap, rowSet, rowCallback);
			}else{
				throw new IllegalArgumentException("Unknown RowSet type: "+appendSet.getClass().getName());
			}

			RowReferenceSetResults  rrsr = new RowReferenceSetResults();
			rrsr.setRowReferenceSet(results);
			asynchJobStatusManager.setComplete(status.getJobId(), rrsr);
		}catch(Throwable e){
			// Record the error
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw e;
		}
	}
	
	/**
	 * Extract the AsynchUploadRequestBody from the message.
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	AsynchronousJobStatus extractStatus(Message message) throws JSONObjectAdapterException{
		if(message == null){
			throw new IllegalArgumentException("Message cannot be null");
		}
		AsynchronousJobStatus status = MessageUtils.readMessageBody(message, AsynchronousJobStatus.class);
		if(status.getRequestBody() == null){
			throw new IllegalArgumentException("Job body cannot be null");
		}
		if (!(status.getRequestBody() instanceof AppendableRowSetRequest)) {
			throw new IllegalArgumentException("Expected a job body of type: " + AppendableRowSetRequest.class.getName() + " but received: "
					+ status.getRequestBody().getClass().getName());
		}
		return status;
	}
	
}
