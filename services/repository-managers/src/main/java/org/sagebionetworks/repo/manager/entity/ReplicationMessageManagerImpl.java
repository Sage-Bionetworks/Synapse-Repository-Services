package org.sagebionetworks.repo.manager.entity;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.collect.Lists;

public class ReplicationMessageManagerImpl implements ReplicationMessageManager {

	/**
	 * The maximum number of container ID that will be pushed to a single
	 * reconciliation message. Note: This limit was lowered from 1000 to 10 as part
	 * of the fix for PLFM-5101 and PLFM-5051.
	 */
	static final int MAX_CONTAINERS_IDS_PER_RECONCILIATION_MESSAGE = 10;

	@Autowired
	AmazonSQS sqsClient;

	String replicationQueueName;
	String replicationQueueUrl;

	String reconciliationQueueName;
	String reconciliationQueueUrl;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.entity.ReplicationMessageManager#
	 * pushChangeMessagesToReplicationQueue(java.util.List)
	 */
	@Override
	public void pushChangeMessagesToReplicationQueue(List<ChangeMessage> toPush) {
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
			sqsClient.sendMessage(new SendMessageRequest(replicationQueueUrl, messageBody));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.entity.ReplicationMessageManager#
	 * pushContainerIdsToReconciliationQueue(java.util.List)
	 */
	@Override
	public void pushContainerIdsToReconciliationQueue(List<Long> toPush) {
		ValidateArgument.required(toPush, "toPush");
		if (toPush.isEmpty()) {
			// nothing to do.
			return;
		}
		// Partition into batches that are under the max size.
		List<List<Long>> batches = Lists.partition(toPush, MAX_CONTAINERS_IDS_PER_RECONCILIATION_MESSAGE);
		for (List<Long> batch : batches) {
			IdList messages = new IdList();
			messages.setList(batch);
			String messageBody = createMessageBodyJSON(messages);
			sqsClient.sendMessage(new SendMessageRequest(reconciliationQueueUrl, messageBody));
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

	/**
	 * Called when the bean is initialized.
	 */
	public void initialize() {
		if (replicationQueueName == null) {
			throw new IllegalStateException("Replication Queue name cannot be null");
		}
		if (reconciliationQueueName == null) {
			throw new IllegalStateException("Delta Queue name cannot be null");
		}
		if (this.sqsClient == null) {
			throw new IllegalStateException("SQS client cannot be null");
		}
		// replication
		this.replicationQueueUrl = this.sqsClient.getQueueUrl(replicationQueueName).getQueueUrl();
		// delta
		this.reconciliationQueueUrl = this.sqsClient.getQueueUrl(reconciliationQueueName).getQueueUrl();
	}

	/**
	 * Injected.
	 * 
	 * @param replicationQueueName
	 */
	public void setReplicationQueueName(String replicationQueueName) {
		this.replicationQueueName = replicationQueueName;
	}

	/**
	 * Injected.
	 * 
	 * @param reconciliationQueueName
	 */
	public void setReconciliationQueueName(String reconciliationQueueName) {
		this.reconciliationQueueName = reconciliationQueueName;
	}

	@Override
	public long getApproximateNumberOfMessageOnReplicationQueue() {
		Map<String, String> attributes = this.sqsClient.getQueueAttributes(new GetQueueAttributesRequest()
				.withQueueUrl(replicationQueueUrl).withAttributeNames(QueueAttributeName.ApproximateNumberOfMessages)).getAttributes();
		String stringValue = attributes.get(QueueAttributeName.ApproximateNumberOfMessages.name());
		if(stringValue == null) {
			throw new IllegalArgumentException("Failed to get queue attribute");
		}
		return Long.parseLong(stringValue);
	}

}
