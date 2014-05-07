package org.sagebionetworks.table.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.AsynchDownloadRequestBody;
import org.sagebionetworks.repo.model.table.AsynchDownloadResponseBody;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;

import com.amazonaws.services.sqs.model.Message;

public class TableCSVDownloadWorker implements Callable<List<Message>>{

	static private Logger log = LogManager.getLogger(TableCSVDownloadWorker.class);
	private List<Message> messages;
	private ConnectionFactory tableConnectionFactory;
	private AsynchJobStatusManager asynchJobStatusManager;
	
	@Override
	public List<Message> call() throws Exception {
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

	private Message processMessage(Message message) throws Throwable {
		AsynchronousJobStatus status = extractStatus(message);
		try{
			// First parse the the query
			// The job is complete
			AsynchDownloadResponseBody response = new AsynchDownloadResponseBody();
			asynchJobStatusManager.setComplete(status.getJobId(), response);
			return message;
		}catch(Throwable e){
			// The job failed
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
		if(!(status.getRequestBody() instanceof AsynchDownloadRequestBody)){
			throw new IllegalArgumentException("Expected a job body of type: "+AsynchDownloadRequestBody.class.getName()+" but received: "+status.getRequestBody().getClass().getName());
		}
		return status;
	}

}
