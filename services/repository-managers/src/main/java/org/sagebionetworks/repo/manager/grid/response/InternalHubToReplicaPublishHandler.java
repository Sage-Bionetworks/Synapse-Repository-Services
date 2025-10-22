package org.sagebionetworks.repo.manager.grid.response;

import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * This handler is used to take events that originate from the "hub" and send
 * them to the internal (server-side) replica queue.
 */
@Service
public class InternalHubToReplicaPublishHandler implements GridEventResponsePublishHandler {

	private final SqsClient sqsClient;
	private final String queueUrl;

	public InternalHubToReplicaPublishHandler(SqsClient sqsClient, StackConfiguration config) {
		super();
		this.sqsClient = sqsClient;
		this.queueUrl = sqsClient
				.getQueueUrl(GetQueueUrlRequest.builder().queueName(config.getQueueName("GRID_INTERNAL_EVENT.fifo")).build())
				.queueUrl();
	}

	@Override
	public EventSource getEventSource() {
		return EventSource.INTERNAL;
	}

	@Override
	public void publishEventResponse(EventContext context, String event) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(event, "event");
		if (EventSource.INTERNAL.equals(context.getEventSource())) {
			sqsClient
					.sendMessage(
							SendMessageRequest.builder().queueUrl(queueUrl).messageBody(event)
									.messageGroupId(context.getConnectionId())
									.messageDeduplicationId(UUID.randomUUID().toString())
									.messageAttributes(Map.of("ConnectionId", MessageAttributeValue.builder()
											.stringValue(context.getConnectionId()).dataType("String").build()))
									.build());
		}
	}

}
