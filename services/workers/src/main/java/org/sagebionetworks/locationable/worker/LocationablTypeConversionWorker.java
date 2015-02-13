package org.sagebionetworks.locationable.worker;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.EntityTypeConverter;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.AsyncLocationableTypeConversionRequest;
import org.sagebionetworks.repo.model.AsyncLocationableTypeConversionResults;
import org.sagebionetworks.repo.model.LocationableTypeConversionResult;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class LocationablTypeConversionWorker implements Worker {

	static private Logger log = LogManager.getLogger(LocationablTypeConversionWorker.class);
	
	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	EntityTypeConverter entityTypeConverter;
	@Autowired
	private UserManager userManger;
	
	WorkerProgress workerProgress;
	List<Message> messages;
	
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
	 * This is where the real work happens
	 * @param message
	 * @return
	 * @throws Throwable 
	 */
	private Message processMessage(Message message) throws Throwable {
		AsynchronousJobStatus status = extractStatus(message);
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			AsyncLocationableTypeConversionRequest body = (AsyncLocationableTypeConversionRequest) status.getRequestBody();
			long progressCurrent = 0L;
			long progressTotal = body.getLocationableIdsToConvert().size();
			// Start the progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Starting...");
			List<LocationableTypeConversionResult> results = new LinkedList<LocationableTypeConversionResult>();
			for(String entityId: body.getLocationableIdsToConvert()){
				// keep this worker alive.
				workerProgress.progressMadeForMessage(message);
				// This is the call that converts a type.
				results.add(entityTypeConverter.convertOldTypeToNew(user, entityId));
				progressCurrent++;
				if(progressCurrent % 10 == 0){
					asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "update: "+entityId);
				}
			}
			AsyncLocationableTypeConversionResults resultBody = new AsyncLocationableTypeConversionResults();
			resultBody.setResults(results);
			// done
			asynchJobStatusManager.setComplete(status.getJobId(), resultBody);
			return message;
		}catch(Throwable e){
			// Record the error
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw e;
		}
	}

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
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
		if (!(status.getRequestBody() instanceof AsyncLocationableTypeConversionRequest)) {
			throw new IllegalArgumentException("Expected a job body of type: " + AsyncLocationableTypeConversionRequest.class.getName() + " but received: "
					+ status.getRequestBody().getClass().getName());
		}
		return status;
	}

}
