package org.sagebionetworks.table.worker;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.AppendableRowSet;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;
/**
 * This worker appends partial row sets to a table.
 * 
 * @author jmhill
 *
 */
public class TableAppendRowSetWorker implements Worker {

	static private Logger log = LogManager.getLogger(TableAppendRowSetWorker.class);
	private List<Message> messages;
	private WorkerProgress workerProgress;

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private TableRowManager tableRowManager;
	@Autowired
	private UserManager userManger;

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}

	@Override
	public List<Message> call() throws Exception {
		// We should only get one message
		List<Message> toDelete = new LinkedList<Message>();
		for(Message message: messages){
			try{
				toDelete.add(processMessage(message));
			}catch(Throwable e){
				// Treat unknown errors as unrecoverable and return them
				toDelete.add(message);
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}
	
	/**
	 * Process a single message
	 * @param message
	 * @return
	 * @throws Throwable 
	 */
	public Message processMessage(Message message) throws Throwable{
		// First read the body
		AsynchronousJobStatus status = extractStatus(message);
		processStatus(status);
		return message;
	}

	/**
	 * @param status
	 * @throws Throwable 
	 */
	public void processStatus(AsynchronousJobStatus status) throws Throwable {
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			AppendableRowSetRequest body = (AppendableRowSetRequest) status.getRequestBody();
			AppendableRowSet partialRowSet = body.getToAppend();
			if(partialRowSet == null){
				throw new IllegalArgumentException("ToAppend cannot be null");
			}
			if(partialRowSet.getTableId() == null){
				throw new IllegalArgumentException("Table ID cannot be null");
			}
			String tableId = partialRowSet.getTableId();
			long progressCurrent = 0L;
			long progressTotal = 100L;
			// Start the progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Starting...");
			// Do the work
			List<ColumnModel> columnModelsForTable = columnModelManager.getColumnModelsForTable(user, tableId);
			ColumnMapper columnMap = TableModelUtils.createColumnModelColumnMapper(columnModelsForTable, false);
			RowReferenceSet results = null;
			if(partialRowSet instanceof PartialRowSet){
				results = tableRowManager.appendPartialRows(user, tableId, columnMap, (PartialRowSet)partialRowSet);
			}else if(partialRowSet instanceof RowSet){
				results = tableRowManager.appendRows(user, tableId, columnMap, (RowSet)partialRowSet);
			}else{
				throw new IllegalArgumentException("Unknown RowSet type: "+partialRowSet.getClass().getName());
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
