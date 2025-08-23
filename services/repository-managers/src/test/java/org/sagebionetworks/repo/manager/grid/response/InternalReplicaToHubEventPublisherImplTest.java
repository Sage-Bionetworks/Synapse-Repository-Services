package org.sagebionetworks.repo.manager.grid.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.springframework.context.ApplicationEventPublisher;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ExtendWith(MockitoExtension.class)
public class InternalReplicaToHubEventPublisherImplTest {

	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private StackConfiguration mockConfig;
	@Mock
	private ApplicationEventPublisher mockApplicationHandler;

	private InternalReplicaToHubEventPublisherImpl handler;

	private String queueUrl;
	private EventContext context;
	private String message;

	@BeforeEach
	public void before() {
		String queueName = "DEV_GRID_WEBSOCKET_MESSAGE";
		when(mockConfig.getQueueName("GRID_WEBSOCKET_MESSAGE")).thenReturn(queueName);
		queueUrl = "https://aws.com/sqs/grid_queue";
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		handler = new InternalReplicaToHubEventPublisherImpl(mockSqsClient, mockConfig, mockApplicationHandler);
		context = new EventContext(EventType.MESSAGE, EventSource.INTERNAL, "connectionId");
		message = "[8,\"connected\"]";
	}

	@Test
	public void testSendAfterCommit() {
		// call under test
		handler.sendAfterCommit(new InternalEvent().setBody(message).setContext(context));
		verify(mockSqsClient).sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageAttributes(Map.of(
				"EventType",
				MessageAttributeValue.builder().stringValue(context.getEventType().name()).dataType("String").build(),
				"EventSource",
				MessageAttributeValue.builder().stringValue(context.getEventSource().name()).dataType("String").build(),
				"ConnectionId",
				MessageAttributeValue.builder().stringValue(context.getConnectionId()).dataType("String").build()))
				.messageBody(message).build());
	}

	@Test
	public void testSendAfterCommitWithNullEvent() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.sendAfterCommit(null);
		}).getMessage();
		assertEquals("event is required.", message);
	}

	@Test
	public void testPublishEventAfterCommit() {
		// call under test
		handler.publishEventAfterCommit(context, JsonRxMessageType.Notification, "connect",
				new Connection().setGridSessionId(1L).setReplicaId(2L));
		verify(mockApplicationHandler).publishEvent(new InternalEvent()
				.setBody("[8,\"connect\",{\"gridSessionId\":1,\"replicaId\":2}]").setContext(context));

	}

	@Test
	public void testPublishEventAfterCommitiWithNullContext() {
		context = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.publishEventAfterCommit(context, JsonRxMessageType.Notification, "connect",
					new Connection().setGridSessionId(1L).setReplicaId(2L));
		}).getMessage();
		assertEquals("context is required.", message);
	}

	@Test
	public void testPublishEventAfterCommitiWithNullType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.publishEventAfterCommit(context, null, "connect",
					new Connection().setGridSessionId(1L).setReplicaId(2L));
		}).getMessage();
		assertEquals("type is required.", message);
	}

	@Test
	public void testPublishEventAfterCommitiWithNullMessage() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.publishEventAfterCommit(context, JsonRxMessageType.Notification, null,
					new Connection().setGridSessionId(1L).setReplicaId(2L));
		}).getMessage();
		assertEquals("method is required.", message);
	}

	@Test
	public void testPublishEventAfterCommitiWithNullPayload() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.publishEventAfterCommit(context, JsonRxMessageType.Notification, "connect", null);
		}).getMessage();
		assertEquals("payload is required.", message);
	}

}
