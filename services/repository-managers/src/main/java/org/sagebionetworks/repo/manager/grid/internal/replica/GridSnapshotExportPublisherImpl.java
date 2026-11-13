package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class GridSnapshotExportPublisherImpl implements GridSnapshotExportPublisher {

	private final SqsClient sqsClient;
	private final String queueUrl;

	public GridSnapshotExportPublisherImpl(SqsClient sqsClient, StackConfiguration config) {
		this.sqsClient = sqsClient;
		this.queueUrl = sqsClient
				.getQueueUrl(GetQueueUrlRequest.builder().queueName(config.getQueueName("GRID_SNAPSHOT_EXPORT.fifo")).build())
				.queueUrl();
	}

	@Override
	public void publishExportRequest(GridConnectionInfo connection) {
		// Grouping by session keeps a session's exports from overlapping.
		sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(connection.getConnectionId())
				.messageGroupId(connection.getSessionId()).messageDeduplicationId(UUID.randomUUID().toString()).build());
	}
}
