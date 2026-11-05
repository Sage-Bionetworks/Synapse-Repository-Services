package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ExtendWith(MockitoExtension.class)
public class GridSnapshotExportPublisherImplTest {

	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private StackConfiguration mockConfig;
	@Captor
	private ArgumentCaptor<SendMessageRequest> messageCaptor;

	private GridSnapshotExportPublisherImpl publisher;
	private String queueUrl;

	@BeforeEach
	public void before() {
		String queueName = "DEV_GRID_INTERNAL_EVENT";
		when(mockConfig.getQueueName("GRID_INTERNAL_EVENT.fifo")).thenReturn(queueName);
		queueUrl = "https://aws.com/sqs/grid_queue";
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		publisher = new GridSnapshotExportPublisherImpl(mockSqsClient, mockConfig);
	}

	@Test
	public void testPublishExportRequestWithTwoConnectionsOfOneSession() {
		GridConnectionInfo first = new GridConnectionInfo().setSessionId("123").setConnectionId("connection-one");
		GridConnectionInfo second = new GridConnectionInfo().setSessionId("123").setConnectionId("connection-two");

		// call under test
		publisher.publishExportRequest(first);
		// call under test
		publisher.publishExportRequest(second);

		verify(mockSqsClient, times(2)).sendMessage(messageCaptor.capture());
		List<SendMessageRequest> requests = messageCaptor.getAllValues();
		SendMessageRequest firstRequest = requests.get(0);
		SendMessageRequest secondRequest = requests.get(1);

		assertEquals(queueUrl, firstRequest.queueUrl());
		assertEquals(new JsonRxMessage(JsonRxMessageType.Notification).setMethod(GridReplicaManager.EXPORT_SNAPSHOT),
				new JsonRxMessage(firstRequest.messageBody()));
		assertEquals(Map.of("ConnectionId",
				MessageAttributeValue.builder().stringValue("connection-one").dataType("String").build()),
				firstRequest.messageAttributes());
		assertEquals(Map.of("ConnectionId",
				MessageAttributeValue.builder().stringValue("connection-two").dataType("String").build()),
				secondRequest.messageAttributes());
		// One group per session, apart from the groups of the replica's own messages
		assertEquals(firstRequest.messageGroupId(), secondRequest.messageGroupId());
		assertNotEquals(first.getConnectionId(), firstRequest.messageGroupId());
		assertNotEquals(firstRequest.messageDeduplicationId(), secondRequest.messageDeduplicationId());
	}

	@Test
	public void testPublishExportRequestWithTwoSessions() {
		// call under test
		publisher.publishExportRequest(new GridConnectionInfo().setSessionId("123").setConnectionId("connection-one"));
		// call under test
		publisher.publishExportRequest(new GridConnectionInfo().setSessionId("456").setConnectionId("connection-two"));

		verify(mockSqsClient, times(2)).sendMessage(messageCaptor.capture());
		List<SendMessageRequest> requests = messageCaptor.getAllValues();
		assertNotEquals(requests.get(0).messageGroupId(), requests.get(1).messageGroupId());
	}
}
