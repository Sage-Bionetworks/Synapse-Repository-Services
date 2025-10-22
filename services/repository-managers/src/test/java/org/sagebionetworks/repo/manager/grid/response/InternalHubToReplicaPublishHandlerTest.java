package org.sagebionetworks.repo.manager.grid.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ExtendWith(MockitoExtension.class)
public class InternalHubToReplicaPublishHandlerTest {

	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private StackConfiguration mockConfig;
	@Captor
	private ArgumentCaptor<SendMessageRequest> messageCaptor;

	private InternalHubToReplicaPublishHandler handler;

	private String queueUrl;
	private EventContext context;
	private String message;

	@BeforeEach
	public void before() {
		String queueName = "DEV_GRID_INTERNAL_EVENT";
		when(mockConfig.getQueueName("GRID_INTERNAL_EVENT.fifo")).thenReturn(queueName);
		queueUrl = "https://aws.com/sqs/grid_queue";
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		handler = new InternalHubToReplicaPublishHandler(mockSqsClient, mockConfig);
		context = new EventContext(EventType.MESSAGE, EventSource.INTERNAL, "connectionId");
		message = "[8,\"connected\"]";
	}

	@Test
	public void testPublishEventResponse() {

		// call under test
		handler.publishEventResponse(context, message);
		verify(mockSqsClient).sendMessage(messageCaptor.capture());
		SendMessageRequest request = messageCaptor.getValue();
		assertEquals(queueUrl, request.queueUrl());
		assertEquals("connectionId", request.messageGroupId());
		assertNotNull(request.messageDeduplicationId());
		assertEquals(Map.of("ConnectionId",
				MessageAttributeValue.builder().stringValue(context.getConnectionId()).dataType("String").build()),
				request.messageAttributes());
	}

	@Test
	public void testPublishEventResponseWithWebsocket() {
		context = new EventContext(EventType.MESSAGE, EventSource.WEBSOCKET, "connectionId");
		// call under test
		handler.publishEventResponse(context, message);
		verifyNoMoreInteractions(mockSqsClient);
	}

	@Test
	public void testPublishEventResponseWithNullContext() {
		context = null;
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.publishEventResponse(context, message);
		}).getMessage();
		assertEquals("context is required.", errorMessage);
	}

	@Test
	public void testPublishEventResponseWithNullMessage() {
		message = null;
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.publishEventResponse(context, message);
		}).getMessage();
		assertEquals("event is required.", errorMessage);
	}

}
