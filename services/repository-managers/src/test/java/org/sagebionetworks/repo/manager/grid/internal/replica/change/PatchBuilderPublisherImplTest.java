package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ExtendWith(MockitoExtension.class)
public class PatchBuilderPublisherImplTest {

	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private StackConfiguration mockConfig;

	private String queueUrl;
	private String queueName;
	private IntendedChangeSet changeSet;

	private PatchBuilderPublisherImpl publisher;

	@BeforeEach
	public void before() {
		queueUrl = "https://some.com/queue";
		queueName = "dev-jmhill-GRID_REPLICA_PATCH_BUILDER.fifo";
		when(mockConfig.getQueueName("GRID_REPLICA_PATCH_BUILDER.fifo")).thenReturn(queueName);
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		publisher = new PatchBuilderPublisherImpl(mockSqsClient, mockConfig);

		changeSet = new IntendedChangeSet().setConnectionId("con123").setReplicaId(33L).setSessionId("session99")
				.setChanges(List.of(new UpdateMetadataChange()
						.setRowMetadataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))))
				.setClockSequenceMaximum(43l);
	}

	@Test
	public void testSendChangesToPatchBuilder() {
		// call under test
		publisher.sendChangesToPatchBuilder(changeSet);
		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		verify(mockSqsClient).sendMessage(captor.capture());
		SendMessageRequest req = captor.getValue();
		assertEquals(queueUrl, req.queueUrl());
		assertEquals("{\"con\":\"con123\",\"ses\":\"session99\",\"rep\":33,\"max\":43,\"set\":[[0,{\"m\":[1,2]}]]}",
				req.messageBody());
		assertEquals("con123", req.messageGroupId());
		String dedupId = req.messageDeduplicationId();
		assertNotNull(dedupId);
		assertDoesNotThrow(() -> UUID.fromString(dedupId));
	}

}
