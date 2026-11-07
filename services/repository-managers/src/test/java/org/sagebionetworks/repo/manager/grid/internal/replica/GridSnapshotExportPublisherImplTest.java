package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
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
		String queueName = "DEV_GRID_SNAPSHOT_EXPORT.fifo";
		when(mockConfig.getQueueName("GRID_SNAPSHOT_EXPORT.fifo")).thenReturn(queueName);
		queueUrl = "https://aws.com/sqs/grid_snapshot_export";
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		publisher = new GridSnapshotExportPublisherImpl(mockSqsClient, mockConfig);
	}

	@Test
	public void testPublishExportRequestWithTwoSessions() {
		// call under test
		publisher.publishExportRequest(new GridConnectionInfo().setSessionId("123").setConnectionId("connection-one"));
		// call under test
		publisher.publishExportRequest(new GridConnectionInfo().setSessionId("123").setConnectionId("connection-two"));
		// call under test
		publisher.publishExportRequest(new GridConnectionInfo().setSessionId("456").setConnectionId("connection-three"));

		verify(mockSqsClient, times(3)).sendMessage(messageCaptor.capture());
		List<SendMessageRequest> requests = messageCaptor.getAllValues();
		assertEquals(queueUrl, requests.get(0).queueUrl());
		assertEquals("connection-one", requests.get(0).messageBody());
		assertEquals("connection-two", requests.get(1).messageBody());
		assertEquals("connection-three", requests.get(2).messageBody());
		// One group per session, whichever connection asks
		assertEquals("123", requests.get(0).messageGroupId());
		assertEquals("123", requests.get(1).messageGroupId());
		assertEquals("456", requests.get(2).messageGroupId());
		// Each request is delivered, rather than deduplicated against an earlier one
		assertNotEquals(requests.get(0).messageDeduplicationId(), requests.get(1).messageDeduplicationId());
	}
}
