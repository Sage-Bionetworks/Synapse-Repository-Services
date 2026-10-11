package org.sagebionetworks.repo.manager.asynch;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.dbo.asynch.FifoQueueParameters;
import org.springframework.beans.factory.annotation.Autowired;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Basic implementation of AsynchJobQueuePublisher
 * 
 * @author jmhill
 *
 */
public class AsynchJobQueuePublisherImpl implements AsynchJobQueuePublisher {
	
	@Autowired
	SqsClient awsSQSClient;
	
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
		SendMessageRequest.Builder request = SendMessageRequest.builder().queueUrl(url).messageBody(status.getJobId());
		if(type.isFifoQueue()) {
			FifoQueueParameters params = type.getFifoParameters(status);
			request.messageDeduplicationId(params.getMessageDeduplicationId());
			request.messageGroupId(params.getMessageGroupId());
		}
		
		/*
		 * Since PLFM-3645, we no longer push the JSON of the request to the SQS.  Instead, we only
		 * publish the jobId and expect the workers to lookup the request from the database.
		 */
		// publish the message
		awsSQSClient.sendMessage(request.build());
	}
	
	/**
	 * Called when the bean is created.
	 */
	public void initialize(){
		// Map each type to its queue;
		toTypeToQueueURLMap = new HashMap<AsynchJobType, String>(AsynchJobType.values().length);
		for(AsynchJobType type: AsynchJobType.values()){
			String qUrl = this.awsSQSClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(type.getQueueName()).build()).queueUrl();
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
		ReceiveMessageResponse results = awsSQSClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(url).maxNumberOfMessages(1).build());
		if(results.hasMessages() && results.messages().size() == 1){
			return results.messages().get(0);
		}
		return null;
	}

	@Override
	public void deleteMessage(AsynchJobType type, Message message) {
		String url = getQueueURLForType(type);
		awsSQSClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(url).receiptHandle(message.receiptHandle()).build());
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
