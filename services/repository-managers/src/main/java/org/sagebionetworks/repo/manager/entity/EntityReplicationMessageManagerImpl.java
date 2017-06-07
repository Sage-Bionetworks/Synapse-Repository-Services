package org.sagebionetworks.repo.manager.entity;

import java.util.List;

import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.collect.Lists;

public class EntityReplicationMessageManagerImpl implements	EntityReplicationMessageManager {
	
	@Autowired
	AmazonSQSClient sqsClient;
	
	String replicationQueueName;
	String deltaQueueName;
	
	String replicationQueueUrl;
	String deltaQueueUrl;
	
	/**
	 * Publish the given messages to the Entity replication queue.
	 * 
	 * @param toPush
	 * @throws JSONObjectAdapterException
	 */
	public void pushReplicationMessagesToQueue(List<ChangeMessage> toPush)
			throws JSONObjectAdapterException {
		if (toPush.isEmpty()) {
			// nothing to do.
			return;
		}
		List<List<ChangeMessage>> batches = Lists
				.partition(
						toPush,
						ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE);
		for (List<ChangeMessage> batch : batches) {
			ChangeMessages messages = new ChangeMessages();
			messages.setList(batch);
			String messageBody = EntityFactory
					.createJSONStringForEntity(messages);
			sqsClient
					.sendMessage(new SendMessageRequest(replicationQueueUrl, messageBody));
		}
	}
	
	/**
	 * P
	 * @param scopeIds
	 * @throws JSONObjectAdapterException
	 */
	public void pushDeltaMessagesToQueue(List<Long> scopeIds) throws JSONObjectAdapterException{
		if (scopeIds.isEmpty()) {
			// nothing to do.
			return;
		}
		List<List<Long>> batches = Lists
				.partition(
						scopeIds,
						ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE);
		for (List<Long> batch : batches) {
			IdList messages = new IdList();
			messages.setList(batch);
			String messageBody = EntityFactory
					.createJSONStringForEntity(messages);
			sqsClient
					.sendMessage(new SendMessageRequest(deltaQueueUrl, messageBody));
		}
	}
	
	/**
	 * Called when the bean is initialized.
	 */
	public void initialize(){
		if (replicationQueueName == null) {
			throw new IllegalStateException("Replication Queue name cannot be null");
		}
		if(deltaQueueName == null){
			throw new IllegalStateException("Delta Queue name cannot be null");
		}
		if (this.sqsClient == null) {
			throw new IllegalStateException("SQS client cannot be null");
		}
		// replication
		CreateQueueResult result = this.sqsClient.createQueue(replicationQueueName);
		this.replicationQueueUrl = result.getQueueUrl();
		// delta
		result = this.sqsClient.createQueue(deltaQueueName);
		this.deltaQueueUrl = result.getQueueUrl();
	}
	

}
