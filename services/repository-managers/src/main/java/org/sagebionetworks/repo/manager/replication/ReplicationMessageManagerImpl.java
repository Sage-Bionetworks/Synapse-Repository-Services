package org.sagebionetworks.repo.manager.replication;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class ReplicationMessageManagerImpl implements ReplicationMessageManager {

	static final String REPLICATION_QUEUE_NAME = "TABLE_ENTITY_REPLICATION";
	static final String RECONCILIATION_QUEUE_NAME = "ENTITY_REPLICATION_RECONCILIATION";
	
	private SqsClient sqsClient;
	
	private StackConfiguration config;

	String replicationQueueUrl;
	String reconciliationQueueUrl;

	@Autowired
	public ReplicationMessageManagerImpl(SqsClient sqsClient, StackConfiguration config) {
		this.sqsClient = sqsClient;
		this.config = config;
	}
	
	/**
	 * Called when the bean is initialized.
	 */
	@PostConstruct
	public void initialize() {
		String replicationQueueName = config.getQueueName(REPLICATION_QUEUE_NAME);
		if (replicationQueueName == null) {
			throw new IllegalStateException("Replication Queue name cannot be null");
		}
		String reconciliationQueueName = config.getQueueName(RECONCILIATION_QUEUE_NAME);
		if (reconciliationQueueName == null) {
			throw new IllegalStateException("Delta Queue name cannot be null");
		}
		if (this.sqsClient == null) {
			throw new IllegalStateException("SQS client cannot be null");
		}
		// replication
		this.replicationQueueUrl = this.sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(replicationQueueName).build()).queueUrl();
		// delta
		this.reconciliationQueueUrl = this.sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(reconciliationQueueName).build()).queueUrl();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.entity.ReplicationMessageManager#
	 * pushChangeMessagesToReplicationQueue(java.util.List)
	 */
	@Override
	public void pushChangeMessagesToReplicationQueue(List<ChangeMessage> toPush) {
		pushToQueue(replicationQueueUrl, toPush);
	}
	
	@Override
	public void pushChangeMessagesToReconciliationQueue(List<ChangeMessage> toPush) {
		pushToQueue(reconciliationQueueUrl, toPush);
	}
	
	void pushToQueue(String queueUrl, List<ChangeMessage> toPush) {
		ValidateArgument.required(queueUrl, "queueUrl");
		ValidateArgument.required(toPush, "toPush");
		if (toPush.isEmpty()) {
			// nothing to do.
			return;
		}
		// Partition into batches that are under the max size.
		List<List<ChangeMessage>> batches = Lists.partition(toPush,
				ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE);
		for (List<ChangeMessage> batch : batches) {
			ChangeMessages messages = new ChangeMessages();
			messages.setList(batch);
			String messageBody = createMessageBodyJSON(messages);
			sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build());
		}
	}
	/**
	 * Helper to create a message body from JSONEntity without a checked exception.
	 * 
	 * @param entity
	 * @return
	 */
	String createMessageBodyJSON(JSONEntity entity) {
		try {
			return EntityFactory.createJSONStringForEntity(entity);
		} catch (JSONObjectAdapterException e) {
			// this should not occur.
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getApproximateNumberOfMessageOnReplicationQueue() {
		Map<QueueAttributeName, String> attributes = this.sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
				.queueUrl(replicationQueueUrl).attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES).build()).attributes();
		String stringValue = attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
		if(stringValue == null) {
			throw new IllegalArgumentException("Failed to get queue attribute");
		}
		return Long.parseLong(stringValue);
	}

}
