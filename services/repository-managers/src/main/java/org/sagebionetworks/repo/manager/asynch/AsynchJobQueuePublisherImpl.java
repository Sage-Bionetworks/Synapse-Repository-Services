package org.sagebionetworks.repo.manager.asynch;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * Basic implementation of AsynchJobQueuePublisher
 * 
 * @author jmhill
 *
 */
public class AsynchJobQueuePublisherImpl implements AsynchJobQueuePublisher {
	
	@Autowired
	AmazonSQS awsSQSClient;
	
	/**
	 * Mapping from a job type to a queue URL
	 */
	private Map<AsynchJobType, String> toTypeToQueueURLMap;


	@Override
	public void publishMessage(AsynchronousJobStatus status) {
		if(status == null) throw new IllegalArgumentException("AsynchronousJobStatus cannot be null");
		if(status.getRequestBody() == null) throw new IllegalArgumentException("AsynchronousJobStatus.jobBody cannot be null");
		// Get the type for the job
		AsynchJobType type = AsynchJobType.findTypeFromRequestClass(status.getRequestBody().getClass());
		// Get the URL for this type's queue
		String url = getQueueURLForType(type);
		/*
		 * Since PLFM-3645, we no longer push the JSON of the request to the SQS.  Instead, we only
		 * publish the jobId and expect the workers to lookup the request from the database.
		 */
		// publish the message
		awsSQSClient.sendMessage(new SendMessageRequest(url, status.getJobId()));
	}
	
	/**
	 * Called when the bean is created.
	 */
	public void initialize(){
		// Map each type to its queue;
		toTypeToQueueURLMap = new HashMap<AsynchJobType, String>(AsynchJobType.values().length);
		for(AsynchJobType type: AsynchJobType.values()){
			String qUrl = this.awsSQSClient.getQueueUrl(type.getQueueName()).getQueueUrl();
			toTypeToQueueURLMap.put(type, qUrl);
		}
	}
	
	/**
	 * Get the queue URL for a type.
	 * 
	 * @param type
	 * @return
	 */
	private String getQueueURLForType(AsynchJobType type){
		String url = toTypeToQueueURLMap.get(type);
		if(url == null){
			throw new IllegalStateException("Cannot find the queue URL for Type: "+type);
		}
		return url;
	}

	@Override
	public Message recieveOneMessage(AsynchJobType type) {
		String url = getQueueURLForType(type);
		ReceiveMessageResult results =awsSQSClient.receiveMessage(new ReceiveMessageRequest(url).withMaxNumberOfMessages(1));
		if(results.getMessages() != null && results.getMessages().size() == 1){
			return results.getMessages().get(0);
		}
		return null;
	}

	@Override
	public void deleteMessage(AsynchJobType type, Message message) {
		String url = getQueueURLForType(type);
		awsSQSClient.deleteMessage(new DeleteMessageRequest(url, message.getReceiptHandle()));
	}

	@Override
	public void emptyAllQueues() {
		for(AsynchJobType type: AsynchJobType.values()){
			for(Message message = recieveOneMessage(type); message != null; message = recieveOneMessage(type)){
				deleteMessage(type, message);
			}
		}
	}
	
}
