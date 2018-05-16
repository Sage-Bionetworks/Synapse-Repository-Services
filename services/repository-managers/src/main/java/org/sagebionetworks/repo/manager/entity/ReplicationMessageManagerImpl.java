package org.sagebionetworks.repo.manager.entity;

import java.util.List;

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
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.collect.Lists;

public class ReplicationMessageManagerImpl implements ReplicationMessageManager {

	@Autowired
	AmazonSQS sqsClient;

	String replicationQueueName;
	String replicationQueueUrl;
	
	String reconciliationQueueName;
	String reconciliationQueueUrl;

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.entity.ReplicationMessageManager#pushChangeMessagesToReplicationQueue(java.util.List)
	 */
	@Override
	public void pushChangeMessagesToReplicationQueue(List<ChangeMessage> toPush) {
		ValidateArgument.required(toPush, "toPush");
		if (toPush.isEmpty()) {
			// nothing to do.
			return;
		}
		// Partition into batches that are under the max size.
		List<List<ChangeMessage>> batches = Lists
				.partition(
						toPush,
						ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE);
		for (List<ChangeMessage> batch : batches) {
			ChangeMessages messages = new ChangeMessages();
			messages.setList(batch);
			String messageBody = createMessageBodyJSON(messages);
			sqsClient.sendMessage(new SendMessageRequest(replicationQueueUrl,
					messageBody));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.entity.ReplicationMessageManager#pushContainerIdsToReconciliationQueue(java.util.List)
	 */
	@Override
	public void pushContainerIdsToReconciliationQueue(List<Long> toPush) {
		ValidateArgument.required(toPush, "toPush");
		if (toPush.isEmpty()) {
			// nothing to do.
			return;
		}
		// Partition into batches that are under the max size.
		List<List<Long>> batches = Lists.partition(toPush,
				ChangeMessageUtils.MAX_NUMBER_OF_ID_MESSAGES_PER_SQS_MESSAGE);
		for (List<Long> batch : batches) {
			IdList messages = new IdList();
			messages.setList(batch);
			String messageBody = createMessageBodyJSON(messages);
			sqsClient.sendMessage(new SendMessageRequest(reconciliationQueueUrl,
					messageBody));
		}
	}

	/**
	 * Helper to create a message body from JSONEntity without a checked
	 * exception.
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
			throw new IllegalStateException(
					"Replication Queue name cannot be null");
		}
		if (reconciliationQueueName == null) {
			throw new IllegalStateException("Delta Queue name cannot be null");
		}
		if (this.sqsClient == null) {
			throw new IllegalStateException("SQS client cannot be null");
		}
		// replication
		CreateQueueResult result = this.sqsClient
				.createQueue(replicationQueueName);
		this.replicationQueueUrl = result.getQueueUrl();
		// delta
		result = this.sqsClient.createQueue(reconciliationQueueName);
		this.reconciliationQueueUrl = result.getQueueUrl();
	}

	/**
	 * Injected.
	 * @param replicationQueueName
	 */
	public void setReplicationQueueName(String replicationQueueName) {
		this.replicationQueueName = replicationQueueName;
	}

	/**
	 * Injected.
	 * @param reconciliationQueueName
	 */
	public void setReconciliationQueueName(String reconciliationQueueName) {
		this.reconciliationQueueName = reconciliationQueueName;
	}
	

}
