package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class PatchBuilderPublisherImpl implements PatchBuilderPublisher {

	private final SqsClient sqsClient;
	private final String queueUrl;

	public PatchBuilderPublisherImpl(SqsClient sqsClient, StackConfiguration config) {
		super();
		this.sqsClient = sqsClient;
		this.queueUrl = sqsClient.getQueueUrl(
				GetQueueUrlRequest.builder().queueName(config.getQueueName("GRID_REPLICA_PATCH_BUILDER.fifo")).build())
				.queueUrl();
	}

	@Override
	public void sendChangesToPatchBuilder(IntendedChangeSet changeSet) {
		String body = IntendedChangeSerializable.serialize(changeSet).toString();
		/*
		 * Patches for a replica must be created in series to prevent data loss from
		 * conflicting patches. Messages are grouped by 'connectionId' to ensure
		 * the FIFO queue triggers the creation of all patches for the same replica
		 * sequentially.
		 */
		sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageGroupId(changeSet.getConnectionId())
				.messageDeduplicationId(UUID.randomUUID().toString()).messageBody(body).build());
	}

}
