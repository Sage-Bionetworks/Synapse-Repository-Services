package org.sagebionetworks.repo.manager.grid.response;

import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class InternalReplicaToHubEventPublisherImpl implements InternalReplicaToHubEventPublisher {

	private final SqsClient sqsClient;
	private final String queueUrl;
	private final ApplicationEventPublisher applicationEventPublisher;

	public InternalReplicaToHubEventPublisherImpl(SqsClient sqsClient, StackConfiguration config,
			ApplicationEventPublisher applicationEventPublisher) {
		super();
		this.sqsClient = sqsClient;
		this.queueUrl = sqsClient.getQueueUrl(
				GetQueueUrlRequest.builder().queueName(config.getQueueName("GRID_WEBSOCKET_MESSAGE.fifo")).build())
				.queueUrl();
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void publishEventAfterCommit(EventContext context, String messageJson) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(messageJson, "messageJson");
		// the message will not go out until the transaction commits.
		applicationEventPublisher.publishEvent(new InternalEvent().setContext(context).setBody(messageJson));
	}

	@Override
	public void publishEventAfterCommit(EventContext context, JsonRxMessage message) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(message, "message");
		publishEventAfterCommit(context, message.toJson());
	}

	@Override
	public <T extends JSONEntity> void publishEventAfterCommit(EventContext context, JsonRxMessageType type,
			String method, T payload) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(type, "type");
		ValidateArgument.required(method, "method");
		ValidateArgument.required(payload, "payload");
		try {
			publishEventAfterCommit(context, new JsonRxMessage(type).setMethod(method)
					.setBody(EntityFactory.createJSONObjectForEntity(payload)));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Called by the {@link ApplicationEventPublisher} after the source transaction
	 * commits.
	 * 
	 * @param event
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void sendAfterCommit(InternalEvent event) {
		ValidateArgument.required(event, "event");
		sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl)
				.messageDeduplicationId(UUID.randomUUID().toString())
				.messageGroupId(event.getContext().getConnectionId()).messageAttributes(Map.of(
						//
						"EventType",
						MessageAttributeValue.builder().stringValue(event.getContext().getEventType().name())
								.dataType("String").build(),
						//
						"EventSource",
						MessageAttributeValue.builder().stringValue(event.getContext().getEventSource().name())
								.dataType("String").build(),
						//
						"ConnectionId", MessageAttributeValue.builder()
								.stringValue(event.getContext().getConnectionId()).dataType("String").build()))
				.messageBody(event.getBody()).build());
	}

	@Override
	public void publishEvent(EventContext context, JsonRxMessage message) {
		sendAfterCommit(new InternalEvent().setContext(context).setBody(message.toJson()));
	}

}
