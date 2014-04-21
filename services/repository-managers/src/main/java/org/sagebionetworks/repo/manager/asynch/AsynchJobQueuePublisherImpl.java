package org.sagebionetworks.repo.manager.asynch;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
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
	AmazonSQSClient awsSQSClient;
	
	private String stackAndStackInstancePrefix;
	
	/**
	 * Mapping from a job type to a queue URL
	 */
	private Map<AsynchJobType, String> toTypeToQueueURLMap;
	
	/**
	 * Injected via spring
	 * 
	 * @param stackAndStackInstancePrefix
	 */
	public void setStackAndStackInstancePrefix(String stackAndStackInstancePrefix) {
		this.stackAndStackInstancePrefix = stackAndStackInstancePrefix;
	}

	@Override
	public void publishMessage(AsynchronousJobStatus status) {
		if(status == null) throw new IllegalArgumentException("AsynchronousJobStatus cannot be null");
		if(status.getJobBody() == null) throw new IllegalArgumentException("AsynchronousJobStatus.jobBody cannot be null");
		// Get the type for the job
		AsynchJobType type = AsynchJobType.findType(status.getJobBody().getClass());
		// Get the URL for this type's queue
		String url = getQueueURLForType(type);
		String bodyJson;
		try {
			bodyJson = EntityFactory.createJSONStringForEntity(status);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		// publish the message
		awsSQSClient.sendMessage(new SendMessageRequest(url, bodyJson));
	}
	
	/**
	 * Called when the bean is created.
	 */
	public void initialize(){
		// Map each type to its queue;
		toTypeToQueueURLMap = new HashMap<AsynchJobType, String>(AsynchJobType.values().length);
		for(AsynchJobType type: AsynchJobType.values()){
			CreateQueueRequest cqRequest = new CreateQueueRequest(stackAndStackInstancePrefix+"-"+type.getQueueNameSuffix());
			CreateQueueResult cqResult = this.awsSQSClient.createQueue(cqRequest);
			String qUrl = cqResult.getQueueUrl();
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
