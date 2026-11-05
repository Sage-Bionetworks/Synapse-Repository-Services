package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class GridSnapshotExportPublisherImpl implements GridSnapshotExportPublisher {

	private final SqsClient sqsClient;
	private final String queueUrl;

	public GridSnapshotExportPublisherImpl(SqsClient sqsClient, StackConfiguration config) {
		this.sqsClient = sqsClient;
		this.queueUrl = sqsClient
				.getQueueUrl(GetQueueUrlRequest.builder().queueName(config.getQueueName("GRID_INTERNAL_EVENT.fifo")).build())
				.queueUrl();
	}

	@Override
	public void publishExportRequest(GridConnectionInfo connection) {
		String body = new JsonRxMessage(JsonRxMessageType.Notification).setMethod(GridReplicaManager.EXPORT_SNAPSHOT)
				.toJson();
		/*
		 * The replica's own messages are grouped by connection ID. A separate group per session lets an export run
		 * while the replica keeps applying patches, and keeps a session's exports from overlapping.
		 */
		sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body)
				.messageGroupId("snapshot-export-" + connection.getSessionId())
				.messageDeduplicationId(UUID.randomUUID().toString())
				.messageAttributes(Map.of("ConnectionId", MessageAttributeValue.builder()
						.stringValue(connection.getConnectionId()).dataType("String").build()))
				.build());
	}
}
